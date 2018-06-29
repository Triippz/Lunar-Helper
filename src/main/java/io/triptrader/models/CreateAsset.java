/*
 * MIT License
 *
 * Copyright (c) [2018] [Mark Tripoli (Triippz)]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.triptrader.models;

import io.triptrader.exception.SubmitTransactionException;
import io.triptrader.utilities.Connections;
import javafx.scene.control.ComboBox;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;

import java.io.IOException;


public class CreateAsset
{
    private KeyPair srcPair;

    public CreateAsset ( KeyPair srcPair )
    {
        this.srcPair = srcPair;
    }

    public KeyPair getSrcPair() {
        return srcPair;
    }

    public String createNewAsset ( boolean isMainNet, String assetCode, String trustLimit, String sendAmount, String tomlLocation,
                                   boolean authRequiredFlag, boolean authRevocFlag, boolean createIssuingAccount,
                                   String recvAccountId, ComboBox assetType )
            throws SubmitTransactionException
    {
        Server server = Connections.getServer ( isMainNet );

        KeyPair issuingAccount;
        KeyPair distributionAccount;
        SubmitTransactionResponse txResponse;
        StringBuilder newAssetResponses = new StringBuilder();

        distributionAccount = KeyPair.fromSecretSeed ( recvAccountId );

        /** create a new KeyPair **/
        if ( createIssuingAccount )
            issuingAccount = KeyPair.random();
        else
            issuingAccount = KeyPair.fromSecretSeed ( this.srcPair.getSecretSeed() );

        newAssetResponses.append("Issuing account:\n").append( issuingAccount.getAccountId() ).append("\n");
        newAssetResponses.append("Distribution Account:\n").append( distributionAccount.getAccountId() ).append("\n\n");

        /** We need to check and make sure the issuing account has XLM **/
        AccountDetails accountDetails = new AccountDetails ( srcPair );
        String nativeBalance = accountDetails.getNativeBalance ( isMainNet );
        if ( Double.parseDouble(nativeBalance) <= 0 )
            return "Source account must be funded";

        /** create an asset object **/
        Asset newAsset;
        switch ( ( String ) assetType.getValue()  )
        {
            case "ALPHANUM":
                newAsset = createAlphaNum ( assetCode, issuingAccount );
                break;
            case "ALPHANUM4":
                newAsset = createAlphaNum4 ( assetCode, issuingAccount );
                break;
            case "ALPHANUM12":
                newAsset = createAlphaNum12 ( assetCode, issuingAccount );
                break;
            default:
                return "Error creating new asset";
        }

        /** Set the META DATA **/
        AccountResponse sourceAccount = null;
        try {
            sourceAccount = server.accounts().account( issuingAccount );
        } catch (IOException e) {
            e.printStackTrace();
        }
        Transaction setHomeDomain = setMetaData ( sourceAccount, tomlLocation );
        setHomeDomain.sign( issuingAccount );
        try {
            txResponse = server.submitTransaction(setHomeDomain);
            newAssetResponses
                    .append("META DATA\n")
                    .append(txResponse.getEnvelopeXdr() )
                    .append("\n")
                    .append( txResponse.getResultXdr() )
                    .append("\n\n");

        } catch (IOException e) {
            throw new SubmitTransactionException("Error creating meta data for asset");
        }

        /** Requiring or Revoking Authorization **/
        /* set the flags as needed */
        Transaction setAuthorization = setAuthorizationFlags (
                authRequiredFlag,
                authRevocFlag,
                sourceAccount );

        setAuthorization.sign( issuingAccount );
        try {
            txResponse = server.submitTransaction(setAuthorization);
            newAssetResponses
                    .append("AUTH FLAGS:\n")
                    .append(txResponse.getEnvelopeXdr())
                    .append("\n")
                    .append( txResponse.getResultXdr() )
                    .append("\n\n");
        } catch (IOException e) {
            throw new SubmitTransactionException( "Error " );
        }

        /** First, the receiving account must trust the asset **/
        /** We need to trust this account with the same limit **/
        /** of which we want to send to it                    **/
        AccountResponse receiving = null;
        try {
            receiving = server.accounts().account( distributionAccount );
        } catch (IOException e) {
            e.printStackTrace();
        }
        Transaction allowNewAsset = new Transaction.Builder(receiving)
                .addOperation(
                        // The `ChangeTrust` operation creates (or alters) a trustline
                        // The second parameter limits the amount the account can hold
                        new ChangeTrustOperation.Builder( newAsset, trustLimit ).build() )
                .build();
        allowNewAsset.sign( distributionAccount );
        try {
            txResponse = server.submitTransaction( allowNewAsset );
            newAssetResponses
                    .append("Trust Asset:\n")
                    .append(txResponse.getEnvelopeXdr())
                    .append("\n")
                    .append( txResponse.getResultXdr() )
                    .append("\n\n");
        } catch (IOException e) {
            throw new SubmitTransactionException ( "Error creating new asset.\n " + e.getMessage() );
        }

        /** Second, the issuing account actually sends a payment using the asset **/
        AccountResponse issuing = null;
        try {
            issuing = server.accounts().account( issuingAccount );

        } catch (IOException e) {
            e.printStackTrace();
        }
        Transaction sendNewAsset = new Transaction.Builder( issuing )
                .addOperation(
                        new PaymentOperation.Builder( distributionAccount, newAsset, sendAmount ).build() )
                .build();
        sendNewAsset.sign( issuingAccount );
        try {
            txResponse = server.submitTransaction(sendNewAsset);
            newAssetResponses
                    .append("Send Payment:\n")
                    .append( txResponse.getEnvelopeXdr() )
                    .append("\n")
                    .append( txResponse.getResultXdr() )
                    .append("\n\n");
        } catch (IOException e) {
            throw new SubmitTransactionException ( "Error sending new asset to receiving account\n " + e.getMessage() );
        }

        return newAssetResponses.toString();
    }



    private Transaction setMetaData ( AccountResponse sourceAccount, String domain )
    {
        return new Transaction.Builder(sourceAccount)
                .addOperation( new SetOptionsOperation.Builder()
                        .setHomeDomain( domain ).build() )
                .build()  ;
    }


    private Transaction setAuthorizationFlags ( boolean authReq, boolean authRevoc, AccountResponse sourceAccount )
    {

        if ( authReq && authRevoc )
            return new Transaction.Builder( sourceAccount )
                    .addOperation(new SetOptionsOperation.Builder()
                            .setSetFlags(
                                    AccountFlag.AUTH_REQUIRED_FLAG.getValue() | AccountFlag.AUTH_REVOCABLE_FLAG.getValue())
                            .build())
                    .build();
        else if ( authReq && !authRevoc )
            return new Transaction.Builder( sourceAccount )
                    .addOperation(new SetOptionsOperation.Builder()
                            .setSetFlags( AccountFlag.AUTH_REQUIRED_FLAG.getValue() )
                            .build())
                    .build();
        else if ( !authReq && authRevoc )
            return new Transaction.Builder( sourceAccount )
                    .addOperation(new SetOptionsOperation.Builder()
                            .setSetFlags( AccountFlag.AUTH_REVOCABLE_FLAG.getValue())
                            .build())
                    .build();
        else
            return new Transaction.Builder( sourceAccount )
                    .addOperation(new SetOptionsOperation.Builder().build() )
                    .build();
    }

    private Asset createAlphaNum4 ( String assetCode, KeyPair issuer ) {
        return AssetTypeCreditAlphaNum4.createNonNativeAsset ( assetCode, issuer );
    }

    private Asset createAlphaNum ( String assetCode, KeyPair issuer ) {
        return AssetTypeCreditAlphaNum.createNonNativeAsset ( assetCode, issuer );
    }

    private Asset createAlphaNum12 ( String assetCode, KeyPair issuer ) {
        return AssetTypeCreditAlphaNum12.createNonNativeAsset( assetCode, issuer );
    }
}

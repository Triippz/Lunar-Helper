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

import io.triptrader.models.assets.StellarAsset;
import io.triptrader.models.assets.Transactions;
import io.triptrader.utilities.Connections;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.stellar.sdk.*;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.TransactionsRequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.io.IOException;


public class AccountDetails
{
    private KeyPair pair;

    public AccountDetails ( KeyPair pair )
    {
        this.pair = pair;
    }

    @SuppressWarnings("Duplicates")
    public String getNativeBalance ( boolean isMainNet )  {
        Server server = Connections.getServer ( isMainNet );
        String balanceAmount = null;
        String tempBal = null;

        try {
            AccountResponse.Balance balances[] = server.accounts().account(pair).getBalances();
            for (AccountResponse.Balance balance : balances)
            {
                if ( balance.getAssetType().equalsIgnoreCase( "native") )
                {
                    balanceAmount = balance.getBalance();

                }
            }
        } catch  ( IOException | ErrorResponse e) { balanceAmount = "0"; }

        return balanceAmount;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<StellarAsset> getAssetBalances (boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );

        ObservableList<StellarAsset> assetBalances = FXCollections.observableArrayList();
        AccountResponse.Balance balances[] = server.accounts().account( pair ).getBalances();

        for ( AccountResponse.Balance balance : balances )
        {
            if ( !balance.getAssetType().equalsIgnoreCase("native") )
            {
                String assetName;
                if ( balance.getAssetType().equalsIgnoreCase("native") )
                    assetName = "XLM";
                else
                    assetName = balance.getAssetCode();

                assetBalances.add( new StellarAsset (
                        assetName, balance.getBalance() ) );
            }
        }
        return assetBalances;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<StellarAsset> getAllAssetBalances (boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );

        ObservableList<StellarAsset> assetBalances = FXCollections.observableArrayList();
        AccountResponse.Balance balances[] = server.accounts().account( pair ).getBalances();

        for ( AccountResponse.Balance balance : balances )
        {
            String assetName;
            if ( balance.getAssetType().equalsIgnoreCase("native") )
                assetName = "XLM";
            else
                assetName = balance.getAssetCode();

            assetBalances.add( new StellarAsset (
                    assetName, balance.getBalance() ) );
        }
        return assetBalances;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<String> getAvailableAssets (boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );

        ObservableList<String> assets = FXCollections.observableArrayList();
        AccountResponse.Balance balances[] = server.accounts().account( pair ).getBalances();

        for ( AccountResponse.Balance balance : balances )
        {
            String assetName;
            if ( balance.getAssetType().equalsIgnoreCase("native") )
                assetName = "XLM";
            else
                assetName = balance.getAssetCode();
            assets.add( assetName );
        }
        return assets;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getTransactions ( boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );
        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        Page<OperationResponse> page = server.payments().execute();

        for (OperationResponse payment : page.getRecords()) {
            payment.getPagingToken();
            if ( payment instanceof PaymentOperationResponse )
            {
                String amount = ((PaymentOperationResponse) payment).getAmount();
                Asset asset = ((PaymentOperationResponse) payment).getAsset();

                String assetName;
                if (asset.equals(new AssetTypeNative())) {
                    assetName = "XLM";
                } else {
                    StringBuilder assetNameBuilder = new StringBuilder();
                    assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
                    assetNameBuilder.append(":");
                    assetNameBuilder.append(( (AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId());
                    assetName = assetNameBuilder.toString();
                }
                StringBuilder output = new StringBuilder();
                output.append(amount);
                output.append(" ");
                output.append(assetName);
                output.append(" from ");
                output.append(((PaymentOperationResponse) payment).getFrom().getAccountId());
                System.out.println(output.toString());

                //add it to the list
                transactions.add( new Transactions ( assetName, amount, "?", "?") );
            }

        }
        return transactions;
    }

    public KeyPair getPair() {
        return pair;
    }

    public void setPair(KeyPair pair) {
        this.pair = pair;
    }
}

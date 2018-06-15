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
import io.triptrader.utilities.Format;
import io.triptrader.utilities.Resolve;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.*;
import org.stellar.sdk.Asset;
import org.stellar.sdk.requests.ErrorResponse;

import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.TransactionResponse;

import org.stellar.sdk.xdr.*;
import org.stellar.sdk.xdr.Transaction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;


public class AccountDetails
{
    private final static Logger lunHelpLogger = LoggerFactory.getLogger("lh_logger");


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
    public ObservableList<StellarAsset> getAllAssetBalances ( boolean isMainNet ) throws IOException
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
    public StellarAsset[] getAllAssetBalancesArr ( boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );

        AccountResponse.Balance balances[] = server.accounts().account( pair ).getBalances();
        StellarAsset assetBalances[] = new StellarAsset[ balances.length ];

        int i = 0;
        for ( AccountResponse.Balance balance : balances )
        {
            String assetName;
            if ( balance.getAssetType().equalsIgnoreCase("native") )
                assetName = "XLM";
            else
                assetName = balance.getAssetCode();

            assetBalances[i] = new StellarAsset (
                    assetName, balance.getBalance() );
            i++;
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

/*    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getTransactions ( boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );
        server.ledgers().limit(50);

        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        Page<TransactionResponse> page = server.transactions().forAccount( pair ).execute();
        ArrayList<TransactionResponse> transactionResponses = page.getRecords();

        for ( TransactionResponse txResponse : transactionResponses )
        {
            byte[] bytes = Base64.getDecoder().decode( txResponse.getEnvelopeXdr() );
            XdrDataInputStream in = new XdrDataInputStream(new ByteArrayInputStream(bytes));
            Transaction tx = TransactionEnvelope.decode(in).getTx();

            *//* a tx can have multiple operations, so iterate through (potentially) all of them *//*
            for ( int i = 0; i < tx.getOperations().length; i++ )
            {
                if ( tx.getOperations()[i].getBody().getDiscriminant() == OperationType.PAYMENT )
                {
                    String amount = tx.getOperations()[i].getBody().getPaymentOp().getAmount().getInt64().toString();
                    String coin;
                    if ( tx.getOperations()[i].getBody().getPaymentOp().getAsset().getDiscriminant() == AssetType.ASSET_TYPE_NATIVE )
                        coin = "XLM";
                    else
                        coin = String.valueOf(tx.getOperations()[i].getBody().getPaymentOp().getAsset().getAlphaNum12().getAssetCode());
                    String time = "?";
                    String memo = tx.getMemo().getText();

                    transactions.add( new Transactions( coin, Format.parseAmountString( amount ), time, memo ) );
                }
            }
        }
        return transactions;
    }

    @SuppressWarnings("Duplicates")
    public void getPageTransactions ( boolean isMainNet ) throws IOException, URISyntaxException {
        Server server = Connections.getServer ( isMainNet );

        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        Page<TransactionResponse> page = server.transactions().forAccount( pair ).execute();
        boolean hasNext = true;

        while ( hasNext )
        {
            if ( page.getRecords().size() == 0 )
            {
                System.out.println("End of records");
                break;
            } else {
                System.out.println("Records for account "+ page.getRecords().get(0).getSourceAccount().getAccountId() );
                byte[] bytes = Base64.getDecoder().decode( page.getRecords().get(0).getEnvelopeXdr() );
                XdrDataInputStream in = new XdrDataInputStream(new ByteArrayInputStream(bytes));
                Transaction tx = TransactionEnvelope.decode(in).getTx();

                *//* a tx can have multiple operations, so iterate through (potentially) all of them *//*
                for ( int i = 0; i < tx.getOperations().length; i++ )
                {
                    if ( tx.getOperations()[i].getBody().getDiscriminant() == OperationType.PAYMENT )
                    {
                        String amount = tx.getOperations()[i].getBody().getPaymentOp().getAmount().getInt64().toString();
                        String coin;
                        if ( tx.getOperations()[i].getBody().getPaymentOp().getAsset().getDiscriminant() == AssetType.ASSET_TYPE_NATIVE )
                            coin = "XLM";
                        else
                            coin = String.valueOf(tx.getOperations()[i].getBody().getPaymentOp().getAsset().getAlphaNum12().getAssetCode());
                        String time = "?";
                        String memo = tx.getMemo().getText();

                        transactions.add( new Transactions( coin, Format.parseAmountString( amount ), time, memo ) );

                        StringBuilder builder = new StringBuilder()
                                .append("Coin ").append(coin)
                                .append("\nAmount: ").append(amount)
                                .append("\n");
                        System.out.println(builder.toString());
                    }
                }
            }
        }
    }*/

    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getTransactions ( boolean isMainNet ) throws IOException {
        Server server = Connections.getServer ( isMainNet );
        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        ArrayList<TransactionResponse> transactionResponses = server.transactions().forAccount( pair ).limit(100).execute().getRecords();

        lunHelpLogger.debug("{}", transactionResponses.size() );

        for ( TransactionResponse response : transactionResponses )
        {
            byte[] bytes = Base64.getDecoder().decode( response.getEnvelopeXdr() );
            XdrDataInputStream in = new XdrDataInputStream(new ByteArrayInputStream(bytes));
            Transaction tx = TransactionEnvelope.decode(in).getTx();

            /* a tx can have multiple operations, so iterate through (potentially) all of them */
            for ( int i = 0; i < tx.getOperations().length; i++ )
            {
                if ( tx.getOperations()[i].getBody().getDiscriminant() == OperationType.PAYMENT )
                {
                    org.stellar.sdk.xdr.Asset asset = tx.getOperations()[i].getBody().getPaymentOp().getAsset();

                    String amount = tx.getOperations()[i].getBody().getPaymentOp().getAmount().getInt64().toString();
                    String coin = Resolve.assetName ( asset );
                    String memo = tx.getMemo().getText();

                    lunHelpLogger.debug( "{}\t{}", coin,  Format.parseAmountString( amount ));
                    transactions.add( new Transactions(
                              coin
                            , Format.parseAmountString( amount )
                            , Format.time ( response.getCreatedAt() )
                            , memo ) );
                }
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

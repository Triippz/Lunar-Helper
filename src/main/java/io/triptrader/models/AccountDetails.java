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
import org.stellar.sdk.requests.ErrorResponse;

import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.TransactionResponse;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
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
    private static String myToken = null;
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

    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getTransactions ( boolean isMainNet ) throws IOException {
        Server server = Connections.getServer ( isMainNet );
        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount( pair ).limit(100);
        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        Page<OperationResponse> page = paymentsRequest.execute();

        String lastToken = loadLastPagingToken();
        if (lastToken != null) {
            paymentsRequest.cursor(lastToken);
        }

        for ( OperationResponse response : page.getRecords() )
        {
            savePagingToken( response.getPagingToken() );

            // The payments stream includes both sent and received payments. We only
            // want to process received payments here.
            if ( response instanceof PaymentOperationResponse )
            {
                if ( !( ( PaymentOperationResponse ) response ).getFrom().getAccountId().equalsIgnoreCase ( pair.getAccountId() ) )
                {
                    String amount = ( ( PaymentOperationResponse ) response ).getAmount();
                    String asset = Resolve.assetName( ( ( PaymentOperationResponse ) response ).getAsset() );
                    String time = Format.time ( response.getCreatedAt() );

                    transactions.add( new Transactions(
                            asset, amount, time, false) );
                }
                // now get the sent payments
                else {
                    String amount = Format.sentPayment ( ( ( PaymentOperationResponse ) response ).getAmount() );
                    String asset = Resolve.assetName( ( ( PaymentOperationResponse ) response ).getAsset() );
                    String time = Format.time ( response.getCreatedAt() );

                    transactions.add( new Transactions(
                            asset, amount, time, true) );
                }

            }

        }
        return transactions;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getTransactionsFull ( boolean isMainNet ) throws IOException {
        Server server = Connections.getServer(isMainNet);
        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        ArrayList<TransactionResponse> transactionResponses = server.transactions().forAccount(pair).limit(100).execute().getRecords();

        lunHelpLogger.debug("{}", transactionResponses.size());

        for (TransactionResponse response : transactionResponses) {
            byte[] bytes = Base64.getDecoder().decode(response.getEnvelopeXdr());
            XdrDataInputStream in = new XdrDataInputStream(new ByteArrayInputStream(bytes));
            Transaction tx = TransactionEnvelope.decode(in).getTx();

            /* a tx can have multiple operations, so iterate through (potentially) all of them */
            for (int i = 0; i < tx.getOperations().length; i++) {
                if (tx.getOperations()[i].getBody().getDiscriminant() == OperationType.PAYMENT) {
                    org.stellar.sdk.xdr.Asset asset = tx.getOperations()[i].getBody().getPaymentOp().getAsset();

                    String amount = Format.parseAmountString(tx.getOperations()[i].getBody().getPaymentOp().getAmount().getInt64().toString());
                    String coin = Resolve.assetName(asset);
                    String memo = tx.getMemo().getText();
                    KeyPair srcKey = Resolve.getKeyPairFromAccountIdStr(transactionResponses.get(0).getSourceAccount().getAccountId()); // the user's account
                    KeyPair destKey = Resolve.getKeyPairFromAccountId(tx.getOperations()[i].getBody().getPaymentOp().getDestination());
                    String addr;
                    boolean isPayment;

                    //lunHelpLogger.debug( response.getEnvelopeXdr() );

                    /* determine if we sent or recieved this payment */
                    if (!pair.getAccountId().equalsIgnoreCase(srcKey.getAccountId()) && pair.getAccountId().equalsIgnoreCase(destKey.getAccountId())) {
                        if (!srcKey.getAccountId().equalsIgnoreCase(pair.getAccountId())) {
                            //this means the current account is RECEIVING the payment
                            isPayment = false;
                            addr = srcKey.getAccountId();
                            lunHelpLogger.debug("Recieved from\t{}", srcKey.getAccountId());

                        } else {
                            isPayment = true;
                            addr = destKey.getAccountId();
                            amount = Format.sentPayment(amount);
                            lunHelpLogger.debug("Sent to\t{}", destKey.getAccountId());

                        }
                    } else {
                        isPayment = false;
                        addr = "Sent to self";
                        lunHelpLogger.debug("Sent to self: My Account {}\tTo/From {}", pair.getAccountId(), destKey.getAccountId());

                    }

                    lunHelpLogger.debug("{}\t{}", coin, Format.parseAmountString(amount));
                    transactions.add(new Transactions(
                            coin
                            , amount
                            , Format.time(response.getCreatedAt())
                            , memo
                            , addr
                            , isPayment));
                }
            }
        }
        return transactions;
    }

    @SuppressWarnings("Duplicates")
    public ObservableList<Transactions> getAllPaymentsNM ( boolean isMainNet ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );
        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount( pair );
        ObservableList<Transactions> transactions = FXCollections.observableArrayList();
        Page<OperationResponse> page = paymentsRequest.execute();

        String lastToken = loadLastPagingToken();
        if (lastToken != null) {
            paymentsRequest.cursor(lastToken);
        }

        for ( OperationResponse response : page.getRecords() )
        {
            savePagingToken( response.getPagingToken() );

            // The payments stream includes both sent and received payments. We only
            // want to process received payments here.
            if ( response instanceof PaymentOperationResponse )
            {
                if ( !( ( PaymentOperationResponse ) response ).getFrom().getAccountId().equalsIgnoreCase ( pair.getAccountId() ) )
                {
                    String amount = ( ( PaymentOperationResponse ) response ).getAmount();
                    String asset = Resolve.assetName( ( ( PaymentOperationResponse ) response ).getAsset() );
                    String from = ( ( PaymentOperationResponse) response ).getFrom().getAccountId();
                    String time = Format.time ( response.getCreatedAt() );

                    transactions.add( new Transactions(
                            asset, amount, time, from, false ) );
                } else {
                    String amount = Format.sentPayment ( ( ( PaymentOperationResponse ) response ).getAmount() );
                    String asset = Resolve.assetName( ( ( PaymentOperationResponse ) response ).getAsset() );
                    String from = ( ( PaymentOperationResponse) response ).getFrom().getAccountId();
                    String time = Format.time ( response.getCreatedAt() );

                    transactions.add( new Transactions(
                            asset, amount, time, from, true ) );
                }
            }
        }
        return transactions;
    }

    private static void savePagingToken(String pagingToken) {
        // TODO Auto-generated method stub
        myToken = pagingToken;
    }
    private static String loadLastPagingToken() {
        // TODO Auto-generated method stub
        return myToken;
    }

    public KeyPair getPair() {
        return pair;
    }

    public void setPair(KeyPair pair) {
        this.pair = pair;
    }
}

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

import io.triptrader.utilities.Connections;
import io.triptrader.utilities.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.*;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.io.IOException;
import java.util.Objects;

import static io.triptrader.utilities.Props.LUNAR_HELPER_PROPS;
import static io.triptrader.utilities.Props.MAIN_NET;
import static io.triptrader.utilities.Props.TEST_NET;

public class Payment
{
    private final static Logger lunHelpLogger = LoggerFactory.getLogger("lh_logger");

    private KeyPair pair;

    public Payment(KeyPair pair )
    {
        this.pair = pair;
    }

    public SubmitTransactionResponse sendPayment ( boolean isMainNet, KeyPair srcPair, String destination, String ammount, String memo ) throws IOException
    {
        Server server = Connections.getServer ( isMainNet );

        /* we already have the user's pair, but now we need to get the destinations */
        KeyPair destPair = KeyPair.fromAccountId(destination);

        /* now lets make sure the account exists */
        server.accounts().account(destPair);

        /* if there was no error, lets grab the current information on YOUR account */
        AccountResponse sourceAccount = server.accounts().account ( srcPair );

        /* build the tx */
        Transaction transaction = buildNativeTransaction ( sourceAccount, destPair, ammount, memo );
        transaction.sign(srcPair);

        /* send it off to the network */
         return sendTransaction ( transaction, server );

    }

    public void monitorPayments ( boolean isMainNet, KeyPair srcAcct )
    {
        Server server = Connections.getServer ( isMainNet );

        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount( srcAcct);

        // If some payments have already been handled, start the results from the
        // last seen payment. (See below in `handlePayment` where it gets saved.)
        String lastToken = loadLastPagingToken( server, srcAcct );
        if (lastToken != null) {
            paymentsRequest.cursor(lastToken);
        }

        // `stream` will send each recorded payment, one by one, then keep the
        // connection open and continue to send you new payments as they occur.
        paymentsRequest.stream(new EventListener<OperationResponse>() {
            @Override
            public void onEvent(OperationResponse payment) {
                // Record the paging token so we can start from here next time.
                //savePagingToken(payment.getPagingToken());

                // The payments stream includes both sent and received payments. We only
                // want to process received payments here.
                if (payment instanceof PaymentOperationResponse) {
                    if (((PaymentOperationResponse) payment).getTo().equals(srcAcct)) {
                        return;
                    }

                    String amount = ((PaymentOperationResponse) payment).getAmount();

                    Asset asset = ((PaymentOperationResponse) payment).getAsset();
                    String assetName;
                    if (asset.equals(new AssetTypeNative())) {
                        assetName = "lumens";
                    } else {
                        StringBuilder assetNameBuilder = new StringBuilder();
                        assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getCode());
                        assetNameBuilder.append(":");
                        assetNameBuilder.append(((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId());
                        assetName = assetNameBuilder.toString();
                    }

                    StringBuilder output = new StringBuilder();
                    output.append(amount);
                    output.append(" ");
                    output.append(assetName);
                    output.append(" from ");
                    output.append(((PaymentOperationResponse) payment).getFrom().getAccountId());
                    System.out.println(output.toString());
                }

            }
        });

    }

    private String loadLastPagingToken ( Server server, KeyPair account )
    {
        PaymentsRequestBuilder paymentsRequest = server.payments().forAccount(account);
        String lastToken = loadLastPagingToken( server, account );
        if (lastToken != null) {
            paymentsRequest.cursor(lastToken);
        }

        return lastToken;
    }


    private Transaction buildNativeTransaction ( AccountResponse sourceAccount, KeyPair destPair, String ammount, String memo )
    {
        return  new Transaction.Builder ( sourceAccount )
                .addOperation ( new PaymentOperation.Builder(destPair, new AssetTypeNative (), ammount ).build() )
                // A memo allows you to add your own metadata to a transaction. It's
                // optional and does not affect how Stellar treats the transaction.
                .addMemo ( Memo.text ( memo ) )
                .build();
    }

    private SubmitTransactionResponse sendTransaction ( Transaction transaction, Server server ) throws IOException {
        SubmitTransactionResponse response = server.submitTransaction(transaction);

        lunHelpLogger.info("Successfully sent transaction to Stellar network");
        return response;
    }


    public KeyPair getPair() {
        return pair;
    }

    public void setPair(KeyPair pair) {
        this.pair = pair;
    }
}

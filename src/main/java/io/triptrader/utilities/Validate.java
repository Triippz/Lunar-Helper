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

package io.triptrader.utilities;

import io.triptrader.models.AccountDetails;
import io.triptrader.models.assets.StellarAsset;
import io.triptrader.utilities.Connections;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.xdr.MemoType;

import java.io.IOException;

public class Validate
{
    private static final String TOO_LONG = "Memo Text must be under 28 characters in length";
    private static final String MEMO_OK = "MEMO OK";

    public static boolean validatePayment ( boolean isMainNet, KeyPair pair, AccountDetails account,
                                            String destId, String coin, String balance, String memo,
                                            MemoType memotype ) throws Exception
    {
        // validate the account exists
        if ( !accountId ( destId ) ) throw new Exception("Destination Account Does Not Exist!");
        if ( !userBalance( isMainNet, account, pair, coin, balance ) ) throw new Exception("Not enough funds!");

        String memoResponse = memo ( memo, memotype );
        switch ( memoResponse )
        {
            case TOO_LONG:
                throw new Exception(TOO_LONG);
            case MEMO_OK:
                break;
        }
        return true;
    }

    public static boolean accountId ( String accountId )
    {
        try {
            KeyPair pair = KeyPair.fromAccountId ( accountId );
            return true;
        } catch ( Exception e )
        {
            return false;
        }
    }

    public static boolean userBalance ( boolean isMainNet, AccountDetails account, KeyPair pair, String coin, String balance )
            throws IOException
    {
        Server server = Connections.getServer ( isMainNet );
        boolean enoughFunds = false;

        for ( StellarAsset asset : account.getAllAssetBalances ( isMainNet ) )
        {
            if ( coin.equalsIgnoreCase ( asset.getAssetName() ) )
            {
                double dBal = Double.valueOf ( balance );
                double dBalAct = Double.valueOf ( asset.getAssetBalance() );

                if ( dBal >= dBalAct ) {
                    enoughFunds = false;
                }
                if ( dBal <= dBalAct )
                    enoughFunds =  true;
                break;
            }
        }
        return enoughFunds;
    }

    public static String memo (String memo, MemoType memoType )
    {
        switch ( memoType )
        {
            case MEMO_TEXT:
                if ( memo.length() > 28 )
                    return TOO_LONG;
                break;
            case MEMO_ID:
                break;
            case MEMO_HASH:
                break;
            case MEMO_NONE:
                break;
            case MEMO_RETURN:
                break;
        }
        return MEMO_OK;
    }

    public static boolean limitInBounds ( String limit )
    {
        int digits = 0;
        boolean delimHit = false;

        if ( !limit.contains(".") )
            return true;

        for ( int i = 0; i < limit.length(); i++ )
        {
            while ( !delimHit )
            {
                if ( limit.charAt(i) == '.')
                    delimHit = true;
            }
            if ( delimHit )
                digits++;
        }

        return digits <= 7;
    }

}

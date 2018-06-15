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

import io.triptrader.models.assets.StellarAsset;
import org.json.JSONObject;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.xdr.Asset;
import org.stellar.sdk.xdr.Memo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Resolve
{
    private static final String BASE_URL = "https://coincap.io/";

    public static String assetName ( Asset asset )
    {
        switch ( asset.getDiscriminant() )
        {
            case ASSET_TYPE_NATIVE:
                return "XLM";

            case ASSET_TYPE_CREDIT_ALPHANUM4:
                AssetTypeCreditAlphaNum4 parsedAsset4 = (AssetTypeCreditAlphaNum4) org.stellar.sdk.Asset.fromXdr(asset);
                return parsedAsset4.getCode();

            case ASSET_TYPE_CREDIT_ALPHANUM12:
                AssetTypeCreditAlphaNum12 parsedAsset12 = (AssetTypeCreditAlphaNum12) org.stellar.sdk.Asset.fromXdr(asset);
                return parsedAsset12.getCode();

            default:
                return "Unknown Coin";
        }
    }

    public static boolean memoLength ( Memo memo )
    {
        Byte memoByte = Byte.valueOf( memo.getText() );
        return memoByte.intValue() <= 28;
    }


    public static String assetsToDollar ( StellarAsset assets[] ) throws IOException {
        double totalPrice = 0;
        for ( StellarAsset asset : assets ) {
            double temp = checkAssetPrice ( asset.getAssetName() );
            totalPrice += ( temp * Double.parseDouble( asset.getAssetBalance() ) );
        }

        return Format.parseDollarAmount ( String.valueOf ( totalPrice ) );
    }


    private static double checkAssetPrice(String assetCode) throws IOException
    {
        String jsonString = getJSONFromCC ( BASE_URL + "page/" + assetCode );

        JSONObject jsonObject = new JSONObject ( jsonString );
        return jsonObject.getDouble("price_usd" );
    }

    private static String getJSONFromCC(String requestUrl) throws IOException
    {

        String jsonString = "";
        try {
            URL cc = new URL(requestUrl);
            HttpURLConnection ccConnection = (HttpURLConnection) cc.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(ccConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                jsonString += inputLine;
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

}

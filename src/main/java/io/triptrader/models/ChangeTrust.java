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

import io.triptrader.models.assets.Assets;
import io.triptrader.utilities.Connections;
import io.triptrader.utilities.HorizonQuery;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.Asset;
import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class ChangeTrust
{
    private KeyPair pair;
    private final static Logger lunHelpLogger = LoggerFactory.getLogger("lh_logger");

    public ChangeTrust ( KeyPair pair ) { this.pair = pair; }

    public void changeTrust ( boolean isMainNet, Asset asset, String limit ) throws IOException {
        Server server = Connections.getServer ( isMainNet );
        Page<AssetResponse> page = server.assets().execute();


        for ( AssetResponse response : page.getRecords() )
        {
            //response.getAsset().
        }

        ChangeTrustOperation operation = new ChangeTrustOperation.Builder(asset, limit)
                .setSourceAccount( pair )
                .build();
    }

    /**
     * Used to get all assets and their issuer
     * @param isMainNet to determine which horizon server to hit
     * @return Map k=issuer v=assetcode of all assets
     */
    public TreeMap<String,String> getAllAssets (boolean isMainNet )  {
        return HorizonQuery.getAllAssets ( isMainNet );
    }

    /**
     * Returns an object of type Asset given the assetcode
     * @param isMainNet to determine which horizon server to hit
     * @param assetCode short-hand name of the asset
     * @return Asset Object
     * @throws Exception thrown if there is more than one asset with the given asset code
     */
    public Assets getAsset ( boolean isMainNet, String assetCode ) throws Exception {
        return HorizonQuery.getAssetInfo ( isMainNet, assetCode );
    }

    /**
     * Used when there are multiple assets of the same name but with different issuers
     * @param isMainNet to determine which horizon server to hit
     * @param assetCode short-hand name of the asset
     * @return an ArrayList of all applicable assets
     */
    public ArrayList<Assets> getAssets ( boolean isMainNet, String assetCode ) {
        return HorizonQuery.getAssetInfoArr ( isMainNet, assetCode );
    }

    /**
     * Get all assets of the given asset code,
     * @param isMainNet to determine which horizon server to hit
     * @param assetCode short-hand name of the asset
     * @param issuer the issuer's key
     * @return Asset Object
     */
    public Assets getAssetByIssuer ( boolean isMainNet, String assetCode, String issuer ) throws Exception {
        return HorizonQuery.getAssetInfo ( isMainNet, assetCode, issuer );
    }

    /**
     * Used to get all assets which are issued by the given issuer
     * @param isMainNet to determine which horizon server to hit
     * @param issuer the issuer's key
     * @return ArrayList of Asset Objects
     */
    public ArrayList<Assets> getIssuersAssets ( boolean isMainNet, String issuer ) {
        return HorizonQuery.getIssuerAssets ( isMainNet, issuer);
    }

    public String getAssetResponse (boolean isMainNet, TextField asset, TextField issuer )
    {
        StringBuilder response = new StringBuilder();

        if ( issuer.getText().isEmpty() )
        {
            if ( !asset.getText().equalsIgnoreCase("") )
            {
                try {
                    response.append ( getAsset( isMainNet, asset.getText() ) );
                } catch (Exception e) {
                    if ( e.getMessage().equalsIgnoreCase("1") )
                    {
                        ArrayList<Assets> arrayList = getAssets ( isMainNet, asset.getText() );

                        response.append ("MORE THAN ONE ISSUER FOR ASSET FOUND\n\n");
                        arrayList.forEach( ( v ) -> {
                            response.append( v.toString() );
                            response.append( "\n\n" );
                        });
                    } else if ( e.getMessage().contains("not found.") )
                    {
                        response.append("No Asset found");
                    }
                }
            } else {
                response.append("Please enter an Asset");
            }
        } else {
            try {
                response.append ( getAssetByIssuer ( isMainNet, asset.getText(), issuer.getText() ) );
            } catch (Exception e) {
                response.append ( e.getMessage() );
            }
        }
        return response.toString();
    }
}

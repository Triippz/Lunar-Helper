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

package io.triptrader.controllers;

import io.triptrader.exception.SubmitTransactionException;
import io.triptrader.models.*;
import io.triptrader.utilities.Alerts;
import io.triptrader.utilities.ColumnRowFormatter;
import io.triptrader.utilities.Resolve;
import io.triptrader.utilities.Validate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.xdr.MemoType;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;


public class MainMenuController implements Initializable
{
    /*********** GLOBAL VARIABLES *********/
    private final static Logger lunHelpLogger = LoggerFactory.getLogger("lh_logger");

    private KeyPair userKey;
    private Boolean isMainNet;

    public static String lastTxHash;

    /*********** MODEL CLASSES ************/
    private CreateAccount createAccount;
    private Payment payment;
    private AccountDetails accountDetails;
    private ChangeTrust changeTrust;
    private CreateAsset createAsset;

    /******** PAGE INITS ******/
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        getDefaultPane().setVisible(true);
    }

    private void clearAllPanes ( )
    {
        getDefaultPane().setVisible(false);
        getViewTransactionsPane().setVisible(false);
        getCreateAccountPane().setVisible(false);
        getTrustLinesPane().setVisible(false);
        getSendPaymentPane().setVisible(false);
        getCreateAssetPane().setVisible(false);
        getAccountMergePane().setVisible(false);
        getMainAccountPane().setVisible(false);
        getChangeTrustPane().setVisible(false);
    }

    private void initCreateAssetPane ( )
    {
        clearAllPanes();
        getCaResponsePane().setVisible(false);
        getCreateAssetPane().setVisible(true);
        getCaCreationPane().setVisible(true);

        if ( getAssetTypeComboBox().getItems().isEmpty() )
        {
            ObservableList<String> assetTypes = FXCollections.observableArrayList();
            assetTypes.add("ALPHANUM");
            assetTypes.add("ALPHANUM4");
            assetTypes.add("ALPHANUM12");

            getAssetTypeComboBox().setItems ( assetTypes );
        }
    }

    private void initCreateAssetResponsePane ( String response )
    {
        getCaCreationPane().setVisible(false);
        getCaResponsePane().setVisible(true);
        getCaResponsePaneTA().setText( response );

    }

    private void clearChangeTrustPanes ( )
    {
        getCtSelectTrustPane().setVisible(false);
        getCtChangeTrustPane().setVisible(false);
        getCtMainPane().setVisible(false);
    }

    private void initChangeTrust ( )
    {
        clearAllPanes();
        clearChangeTrustPanes();
        getChangeTrustPane().setVisible(true);
        getCtMainPane().setVisible(true);
    }


    private void initCTAssetSearch ( )
    {
        getCtMainPane().setVisible(false);
        getCtSelectTrustPane().setVisible(true);

        if ( getCtComboBox().getItems().isEmpty() )
        {
            ObservableList<String> searchTypes = FXCollections.observableArrayList();
            searchTypes.add("All Assets");
            searchTypes.add("By Asset");
            searchTypes.add("All Issuer's Assets");

            getCtComboBox().setItems ( searchTypes );
        }
    }

    private void initCTKnowAsset ( )
    {
        clearChangeTrustPanes();
        getCtChangeTrustPane().setVisible(true);
    }

    private void initAccountDetails ( )
    {
        clearAllPanes();
        getMainAccountPane().setVisible(true);
        accountDetails = new AccountDetails ( userKey );

        // public key
        getPublicKeyTxtField().setText( userKey.getAccountId() );
        try {
            getNativeBalanceLabel().setText ( Resolve.assetsToDollar( accountDetails.getAllAssetBalancesArr ( isMainNet ) ) );
            initBalancesTable();
            initTransactionsTable();
        } catch ( IOException  e) {
            e.printStackTrace();
        } catch ( ErrorResponse errorResponse )
        {
            lunHelpLogger.info( "New account. Must add funds." );
        }
    }

    private void initCreateAccount ( )
    {
        clearAllPanes();
        getPvtKeyTxtField().clear();
        getPublicKeyTxtFieldCA().clear();
        getXdrTextArea().clear();
        getUseAccountButton().setVisible(false);
        //getCreateAccountButton().setVisible(true);
        getCreateAccountPane().setVisible(true);
    }

    private void initDefaultPane ( )
    {
        clearAllPanes();
        getDefaultPane().setVisible(true);
    }
    private void initSendPayments ( )
    {
        /* we need to have an active account to send a payment */
        if ( userKey == null )
        {
            Alert alert = new Alert(Alert.AlertType.WARNING );
            alert.setHeaderText("No Active Account");
            alert.setContentText("Need an active account to send a payment. Would you like to use one now?");
            Optional<ButtonType> result = alert.showAndWait();
            if ( result.get() == ButtonType.OK)
                initDefaultPane();
        } else {
            clearAllPanes();
            getSendPaymentPane().setVisible(true);

            /* get the coin types */
            try {
                initPaymentsBalanceTable();

                initAssetsBox();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initTransactions( ) throws IOException {
        clearAllPanes();
        getViewTransactionsPane().setVisible(true);
        initFullTransactionsTable();
    }

    /********** Controller Methods ********/
    @SuppressWarnings("unchecked")
    private void initBalancesTable ( ) throws IOException
    {
        getAssetColumn().setCellValueFactory ( new PropertyValueFactory<>("assetName") );
        getBalanceColumn().setCellValueFactory ( new PropertyValueFactory<>("assetBalance") );

        getBalancesTable().setItems ( accountDetails.getAllAssetBalances ( isMainNet ) );
        getBalancesTable().getColumns().clear();
        getBalancesTable().getColumns().setAll( getAssetColumn(), getBalanceColumn() );
    }

    @SuppressWarnings("unchecked")
    private void initPaymentsBalanceTable ( ) throws IOException
    {
        if ( accountDetails == null )
            accountDetails = new AccountDetails ( userKey );

        getPaymentAssetCol().setCellValueFactory( new PropertyValueFactory<>("assetName") );
        getPaymentBalanceCol().setCellValueFactory( new PropertyValueFactory<>("assetBalance") );

        getPaymentTable().setItems( accountDetails.getAllAssetBalances ( isMainNet ) );
        getPaymentTable().getColumns().clear();
        getPaymentTable().getColumns().setAll( getPaymentAssetCol(), getPaymentBalanceCol() );
    }

    @SuppressWarnings("unchecked")
    private void initAssetsBox ( ) throws IOException
    {
        getPaymentChoiceBox().getItems().clear();
        getPaymentChoiceBox().setItems( accountDetails.getAvailableAssets ( isMainNet ) );
    }

    @SuppressWarnings("unchecked")
    private void initTransactionsTable ( ) throws IOException {
        if ( accountDetails == null )
            accountDetails = new AccountDetails ( userKey );

        getTxAssetColumn().setCellValueFactory ( new PropertyValueFactory<>("assetName") );
        getTxAmountColumn().setCellValueFactory ( new PropertyValueFactory<>("amount") );
        getTxTimeColumn().setCellValueFactory ( new PropertyValueFactory<>("date") );

        getTransactionTable().setItems( accountDetails.getTransactions ( isMainNet ) );
        getTransactionTable().getColumns().clear();
        getTransactionTable().getColumns().setAll ( getTxAssetColumn(), getTxAmountColumn(), getTxTimeColumn() );

        ColumnRowFormatter.txRowTextHighlighter ( getTransactionTable(), getTxAmountColumn() );

    }

    @SuppressWarnings({"unchecked", "Duplicates"})
    private void initFullTransactionsTable ( ) throws IOException
    {
        if ( accountDetails == null && userKey == null)
        {
            Alert alert = new Alert(Alert.AlertType.WARNING );
            alert.setHeaderText("No Active Account");
            alert.setContentText("Need an active account to view transactions. Would you like to use one now?");
            Optional<ButtonType> result = alert.showAndWait();
            if ( result.get() == ButtonType.OK)
                initDefaultPane();
        } else if ( accountDetails == null && userKey != null )
            accountDetails = new AccountDetails ( userKey );

        getTxPaneAssetCol().setCellValueFactory ( new PropertyValueFactory<>("assetName") );
        getTxPaneAmountCol().setCellValueFactory ( new PropertyValueFactory<>("amount") );
        getTxPaneDateCol().setCellValueFactory ( new PropertyValueFactory<>("date") );
        getTxPaneToFromCol().setCellValueFactory ( new PropertyValueFactory<>("toFrom") );

        try {
            getTxPaneTable().setItems( accountDetails.getAllPaymentsNM ( isMainNet ) );
            getTxPaneTable().getColumns().clear();
            getTxPaneTable().getColumns().setAll ( getTxAssetColumn(), getTxAmountColumn(), getTxTimeColumn(), getTxPaneToFromCol() );

            // set the colors depending on payment type
            ColumnRowFormatter.txRowTextHighlighter ( getTxPaneTable(), getTxPaneAmountCol() );
        } catch ( NullPointerException e )
        {
            lunHelpLogger.warn("No active account");
        }

    }

    private void showNewAccount ( )
    {
        getCreateAccountButton().setVisible(false);
        getTestAccountCheckBox().setVisible(false);

        getPublicKeyTxtFieldCA().setVisible(true);
        getPvtKeyTxtField().setVisible(true);
        getPvtKeyWarning().setVisible(true);
        getUseAccountButton().setVisible(true);

        createAccount = new CreateAccount();
        KeyPair newAccount = createAccount.createKeyPair();
        if ( getTestAccountCheckBox().isSelected() )
        {
            String response = "";
            try {
                response = createAccount.createTestAccount ( newAccount );
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Account Created!");
                alert.showAndWait();
                getXdrTextArea().setText(response);

            } catch (IOException e) {
                response = e.getMessage();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Error Creating new Account!");
                alert.showAndWait();
                getXdrTextArea().setText(response);
            } finally {
                getXdrTextArea().setVisible(true);
                getXdrTextArea().setText(response);
            }
        }

        getPvtKeyTxtField().setVisible(true);
        getPvtKeyTxtField().setText( new String ( newAccount.getSecretSeed() ) );
        getPvtKeyWarning().setVisible(true);
        getPublicKeyTxtFieldCA().setVisible(true);
        getPublicKeyTxtFieldCA().setText ( newAccount.getAccountId() );
        getUseAccountButton().setVisible(true);

    }

    private void setNetLabel ( )
    {
        getWhichNetLabel().setText( isMainNet.toString().toUpperCase() );
        if ( isMainNet )
            getWhichNetLabel().setTextFill (Color.GREEN);
        else
            getWhichNetLabel().setTextFill (Color.RED);
    }

    private void clearPayments ( )
    {
        getPaymentTable().getColumns().clear();
        getPaymentChoiceBox().getItems().clear();
        getPaymentDestAddrFld().clear();
        getPaymentAmountFld().clear();
        getPaymentMemoFld().clear();

        getViewInExplorerButton().setVisible(false);
        getPaymentSendButton().setVisible(true);
    }

    private void searchForAsset ( )
    {
        /* we need to have an active account to change a trust line */
        if ( userKey == null )
        {
            Alert alert = new Alert(Alert.AlertType.WARNING );
            alert.setHeaderText("No Active Account");
            alert.setContentText("Need an active account to change a trust line. Would you like to use one now?");
            Optional<ButtonType> result = alert.showAndWait();
            if ( result.get() == ButtonType.OK)
                initDefaultPane();
        } else if ( changeTrust == null && userKey != null )
            changeTrust = new ChangeTrust ( userKey );

        /* now lets see what the users checkbox selection was */
        switch ( ( String ) getCtComboBox().getValue() )
        {
            case "All Assets":
                StringBuilder response = new StringBuilder();
                changeTrust.getAllAssets( isMainNet ).forEach( ( k, v ) -> {
                     response
                            .append("Asset: ").append(v)
                            .append("\t\tIssuer: ").append(k).append("\n");

                });
                getCtAssetSearchReponseTA().setText ( response.toString() );
                break;

            case "By Asset":
                getCtAssetSearchReponseTA().setText ( changeTrust.getAssetResponse (
                        isMainNet,
                        getCtAssetSearchAssetCodeTF().getText(),
                        getCtAssetSearchIssuerTF().getText() ) );
                break;

            case "All Issuer's Assets":
                getCtAssetSearchReponseTA().setText ( changeTrust.getIssuerAssetsReponse(
                        isMainNet,
                        getCtAssetSearchIssuerTF() ) );
                break;

            default:
                getCtAssetSearchReponseTA().setText("Please select a search type");
                break;
        }
    }

    private String createAsset ( )
    {
        /* we need to have an active account to create an asset */
        if ( userKey == null )
        {
            Alert alert = new Alert(Alert.AlertType.WARNING );
            alert.setHeaderText("No Active Account");
            alert.setContentText("Need an active funded account to create an asset. Would you like to use one now?");
            Optional<ButtonType> result = alert.showAndWait();
            if ( result.get() == ButtonType.OK)
                initDefaultPane();
        } else if ( createAsset == null && userKey != null )
            createAsset = new CreateAsset ( userKey );

        boolean createNewAccount = false;
        if ( getYesCheckBox().isSelected() )
            createNewAccount = true;
        else if ( getNoCheckBox().isSelected() )
            createNewAccount = false;
        else if ( getYesCheckBox().isSelected() && getNoCheckBox().isSelected() )
            return "Please select either \"Yes\" or \"No\"";

        String response;
        try {
            response = createAsset.createNewAsset( isMainNet,
                    getCaAssetCodeTextField().getText(),
                    getCaTrustLimitTextField().getText(),
                    getCaAmtSendTextBox().getText(),
                    getCaTomlLocationTextField().getText(),
                    getAuthRequiredCheckBox().isSelected(),
                    getAuthRevocableCheckBox().isSelected(),
                    createNewAccount ,
                    getCaRecvLocationTextField().getText(),
                    getAssetTypeComboBox() );
        } catch (SubmitTransactionException e) {
            response = e.getMessage();
            e.printStackTrace();
        }

        return response;
    }

    /********** SCENE ACTIONS ************/
    @FXML
    public void onDefaultEnter ( MouseEvent event )
    {
        // create the keypair
        try {
            userKey = KeyPair.fromSecretSeed ( getPrivateKeyField().getText() );

            // are we using the main or test net?
            isMainNet = !getTestNetCheckBox().isSelected();

            try {
                getAccountBalanceLabel().setText( new AccountDetails( userKey ).getNativeBalance( isMainNet ) );
                getAccountTextField().setText ( userKey.getAccountId() );
                setNetLabel();
            } catch ( Exception e) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Error Loading Account Details!");
                alert.showAndWait();
                lunHelpLogger.error("{}", e.getMessage());
            }

            // disable the default pane and make the account pane visible
            getDefaultPane().setVisible(false);
            initAccountDetails();

        } catch ( Exception e )
        {
            Alerts alerts = new Alerts(Alert.AlertType.ERROR,
                    "Invalid Private Key",
                    ButtonType.OK);
            alerts.showAndWait();

            lunHelpLogger.error( e.getMessage() );
        }
    }

    @FXML
    public void showPrivateKey ( )
    {
        if (  getPrivateKeyTxtField().getText().isEmpty()  )
            getPrivateKeyTxtField().setText ( String.valueOf( userKey.getSecretSeed() ) );
        else
            getPrivateKeyTxtField().setText("");
    }

    @FXML
    public void createAccount ( )
    {
        initCreateAccount();
    }

    @FXML
    public void createNewAccountClick ( )
    {
        showNewAccount ( );
    }

    @FXML
    public void useNewAccountClick ( )
    {

        userKey = KeyPair.fromSecretSeed( getPvtKeyTxtField().getText() );
        isMainNet = !getTestAccountCheckBox().isSelected();



        if ( getAccountDetails() == null )
            accountDetails = new AccountDetails ( userKey );
        else
            getAccountDetails().setPair ( userKey );

        getAccountTextField().setText( userKey.getAccountId() );
        getAccountBalanceLabel().setText(  accountDetails.getNativeBalance ( isMainNet ) );
        setNetLabel();
        getCreateAccountPane().setVisible(false);
        initAccountDetails();
    }

    @FXML
    public void sendPaymentMenuClick ( )
    {
        clearPayments();
        initSendPayments();
    }

    @FXML
    public void sendPaymentClick ( )
    {
        // make sure they have selected an asset
        if ( getPaymentChoiceBox().getValue() == null )
        {
            Alerts alerts = new Alerts(Alert.AlertType.WARNING,
                    "Please select an asset",
                    ButtonType.OK);
            alerts.showAndWait();
        } else {
            if ( payment == null )
                payment = new Payment( userKey );
            if ( accountDetails == null )
                accountDetails = new AccountDetails ( userKey );

            /* validate information is ok */
            try {
                SubmitTransactionResponse transactionResponse;

                if ( Validate.validatePayment(
                        isMainNet, userKey, accountDetails,
                        getPaymentDestAddrFld().getText(),
                        getPaymentChoiceBox().getValue().toString(),
                        getPaymentAmountFld().getText(),
                        getPaymentMemoFld().getText(),
                        MemoType.MEMO_TEXT) )
                {
                    transactionResponse = payment.sendPayment (
                            isMainNet
                            , userKey
                            , getPaymentDestAddrFld().getText()
                            , getPaymentAmountFld().getText()
                            , getPaymentMemoFld().getText() );

                    if ( transactionResponse.isSuccess() )
                    {
                        lunHelpLogger.info("Transaction Sent to: {}\tAmount: {} {}\tMemo: {}"
                                , getPaymentDestAddrFld().getText()
                                , getPaymentAmountFld().getText()
                                , getPaymentChoiceBox().getValue()
                                , getPaymentMemoFld().getText() );

                        lunHelpLogger.info( "TX Hash: {}", transactionResponse.getHash() );
                        lastTxHash = transactionResponse.getHash();

                        getPaymentSendButton().setVisible(false);
                        getViewInExplorerButton().setVisible(true);

                        getAccountBalanceLabel().setText( new AccountDetails( userKey ).getNativeBalance(isMainNet));

                    } else {
                        Alerts alerts = new Alerts( Alert.AlertType.ERROR,
                                transactionResponse.getResultXdr(),
                                ButtonType.OK);
                        alerts.showAndWait();
                    }
                }

            } catch (Exception e) {
                Alerts alert = new Alerts( Alert.AlertType.ERROR,
                        e.getMessage(),
                        ButtonType.OK );
                alert.showAndWait();

                lunHelpLogger.error("{}", e.getMessage() );

            }
        }
    }

    @FXML
    public void viewInExplorerClick ( )
    {
        if ( lastTxHash != null)
        {
            //Parent root;
            try {
                Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("fxml/StellarChain.fxml")));
                Stage stage = new Stage();
                stage.setTitle("Stellar Chain - " + lastTxHash );
                stage.setScene(new Scene(root, 1000, 700));
                stage.show();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void viewTransactionsClick ( )
    {
        try {
            initTransactions();

            if ( payment == null )
                payment = new Payment( userKey );

        } catch (IOException e) {
            lunHelpLogger.error( e.getMessage() );
        }
    }

    @FXML
    public void accountDetailsClick ( )
    {
        initAccountDetails();
    }

    @FXML
    public void changeTrustClick ( )
    {
        if ( changeTrust == null )
            changeTrust = new ChangeTrust( userKey );

        initChangeTrust();
    }

    @FXML
    public void knowTheAssetClick ( ) { initCTKnowAsset(); }

    @FXML
    public void searchForAssetClick ( ) { initCTAssetSearch(); }

    @FXML
    public void ctAssetSearchClick ( ) {
        searchForAsset ( );
    }

    @FXML
    public void ctUseAssetSearchClick ( ) {

    }

    @FXML
    public void ctChangeTrustClick ( ) {
        /* we need to have an active account to create an asset */
        if ( userKey == null )
        {
            Alert alert = new Alert(Alert.AlertType.WARNING );
            alert.setHeaderText("No Active Account");
            alert.setContentText("Need an active funded account to change trust line. Would you like to use one now?");
            Optional<ButtonType> result = alert.showAndWait();
            if ( result.get() == ButtonType.OK)
                initDefaultPane();
        } else if ( changeTrust == null && userKey != null )
            changeTrust = new ChangeTrust ( userKey );

        try {
            String response = changeTrust.changeTrust (
                    isMainNet,
                    getCtChangeTrustAssetTF().getText(),
                    getCtChangeTrustIssuerAccountTF().getText(),
                    getCtChangeTrustLimitTF().getText() );
            System.out.println(response);
        } catch (Exception e) {
            if ( e.getMessage().contains("No Asset found" ) )
            {
                Alert alert = new Alert(Alert.AlertType.WARNING );
                alert.setHeaderText("No Asset");
                alert.setContentText("No asset found\n" + e.getMessage());
            }else {
                getCtChangeTrustErrorLabel().setVisible(true);
                getCtChangeTrustIssuerAccountLabel().setVisible(true);
                getCtChangeTrustIssuerAccountTF().setVisible(true);
            }
        }
    }

    @FXML
    public void createAssetClick ( ) {
        initCreateAssetPane ( );
    }

    @FXML
    public void caCreateAssetClick ( )
    {
       String reponse = createAsset ( );
       initCreateAssetResponsePane ( reponse );
    }


    /*********************************************
     *                  MENU ACTIONS
     *********************************************/
    @FXML
    public void aboutMenuClick ( )
    {

    }

    @FXML
    public void exitMenuClick ( )
    {
        System.exit(0);
    }

    @FXML
    public void changeAccountMenuClick ( )
    {
        getPrivateKeyField().clear();
        initDefaultPane();
    }


    /* SCENE CONTROLS */
    @FXML
    private AnchorPane mainAncPane;
    @FXML
    private SplitPane splitPane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu fileMenu;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private AnchorPane leftAncPane;
    @FXML
    private Button createAccountBut;
    @FXML
    private Button sendPaymentBut;
    @FXML
    private Button changeTrustBut;
    @FXML
    private Button trustLinesButton;
    @FXML
    private Button accountMergeBut;
    @FXML
    private Button createAssetBut;
    @FXML
    private AnchorPane rightAncPane;
    @FXML
    private Pane createAccountPane;
    @FXML
    private Pane sendPaymentPane;
    @FXML
    private Pane changeTrustPane;
    @FXML
    private Pane trustLinesPane;
    @FXML
    private Pane accountMergePane;
    @FXML
    private Pane createAssetPane;
    @FXML
    private Pane defaultPane;
    @FXML
    private Pane mainAccountPane;
    @FXML
    private PasswordField privateKeyField;
    @FXML
    private Button defaultEnterButton;
    @FXML
    private Label publicKeyLabel;
    @FXML
    private Label privateKeyLabel;
    @FXML
    private Label nativeBalanceLabel;
    @FXML
    private CheckBox testNetCheckBox;
    @FXML
    private TextField publicKeyTxtField;
    @FXML
    private TextField privateKeyTxtField;
    @FXML
    private Button showPvtKeyButton;
    @FXML
    private TableView balancesTable;
    @FXML
    private TableColumn assetColumn;
    @FXML
    private TableColumn balanceColumn;
    @FXML
    private TableView transactionTable;
    @FXML
    private TableColumn txAssetColumn;
    @FXML
    private TableColumn txAmountColumn;
    @FXML
    private TableColumn txTimeColumn;
    @FXML
    private Label pvtKeyLabel;
    @FXML
    private TextField pvtKeyTxtField;
    @FXML
    private Label pvtKeyWarning;
    @FXML
    private Label publicKeyLabelCA;
    @FXML
    private TextField publicKeyTxtFieldCA;
    @FXML
    private Button createAccountButton;
    @FXML
    private Button useAccountButton;
    @FXML
    private CheckBox testAccountCheckBox;
    @FXML
    private Label accountBalanceLabel;
    @FXML
    private TextField accountTextField;
    @FXML
    private TableView paymentTable;
    @FXML
    private TableColumn paymentAssetCol;
    @FXML
    private TableColumn paymentBalanceCol;
    @FXML
    private TextField paymentDestAddrFld;
    @FXML
    private TextField paymentAmountFld;
    @FXML
    private TextField paymentMemoFld;
    @FXML
    private ComboBox paymentChoiceBox;
    @FXML
    private Button paymentSendButton;
    @FXML
    private Button viewInExplorerButton;
    @FXML
    private TextArea xdrTextArea;
    @FXML
    private Label whichNetLabel;
    @FXML
    private TableView txPaneTable;
    @FXML
    private TableColumn txPaneAssetCol;
    @FXML
    private TableColumn txPaneAmountCol;
    @FXML
    private TableColumn txPaneDateCol;
    @FXML
    private TableColumn txPaneToFromCol;
    @FXML
    private Pane viewTransactionsPane;
    @FXML
    private Button searchForAssetBut;
    @FXML
    private Button knowTheAssetBut;
    @FXML
    private Pane ctMainPane;
    @FXML
    private Pane ctSelectTrustPane;
    @FXML
    private TextField ctAssetSearchAssetCodeTF;
    @FXML
    private TextField ctAssetSearchIssuerTF;
    @FXML
    private TextArea ctAssetSearchReponseTA;
    @FXML
    private ComboBox ctComboBox;
    @FXML
    private Pane ctChangeTrustPane;
    @FXML
    private TextField ctChangeTrustAssetTF;
    @FXML
    private TextField ctChangeTrustLimitTF;
    @FXML
    private Button ctChangeTrustButton;
    @FXML
    private Pane caCreationPane;
    @FXML
    private Pane caResponsePane;
    @FXML
    private CheckBox authRequiredCheckBox;
    @FXML
    private CheckBox authRevocableCheckBox;
    @FXML
    private ComboBox assetTypeComboBox;
    @FXML
    private CheckBox yesCheckBox;
    @FXML
    private CheckBox noCheckBox;
    @FXML
    private TextField caAssetCodeTextField;
    @FXML
    private TextField caTrustLimitTextField;
    @FXML
    private TextField caTomlLocationTextField;
    @FXML
    private TextField caRecvLocationTextField;
    @FXML
    private TextField caAmtSendTextBox;
    @FXML
    private Button caCreateButton;
    @FXML
    private TextArea caResponsePaneTA;
    @FXML
    private Label ctChangeTrustErrorLabel;
    @FXML
    private Label ctChangeTrustIssuerAccountLabel;
    @FXML
    private TextField ctChangeTrustIssuerAccountTF;

    public AnchorPane getMainAncPane() {
        return mainAncPane;
    }

    public void setMainAncPane(AnchorPane mainAncPane) {
        this.mainAncPane = mainAncPane;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }

    public void setSplitPane(SplitPane splitPane) {
        this.splitPane = splitPane;
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public void setMenuBar(MenuBar menuBar) {
        this.menuBar = menuBar;
    }

    public Menu getFileMenu() {
        return fileMenu;
    }

    public void setFileMenu(Menu fileMenu) {
        this.fileMenu = fileMenu;
    }

    public Menu getHelpMenu() {
        return helpMenu;
    }

    public void setHelpMenu(Menu helpMenu) {
        this.helpMenu = helpMenu;
    }

    public MenuItem getExitMenuItem() {
        return exitMenuItem;
    }

    public void setExitMenuItem(MenuItem exitMenuItem) {
        this.exitMenuItem = exitMenuItem;
    }

    public MenuItem getAboutMenuItem() {
        return aboutMenuItem;
    }

    public void setAboutMenuItem(MenuItem aboutMenuItem) {
        this.aboutMenuItem = aboutMenuItem;
    }

    public AnchorPane getLeftAncPane() {
        return leftAncPane;
    }

    public void setLeftAncPane(AnchorPane leftAncPane) {
        this.leftAncPane = leftAncPane;
    }

    public Button getCreateAccountBut() {
        return createAccountBut;
    }

    public void setCreateAccountBut(Button createAccountBut) {
        this.createAccountBut = createAccountBut;
    }

    public Button getSendPaymentBut() {
        return sendPaymentBut;
    }

    public void setSendPaymentBut(Button sendPaymentBut) {
        this.sendPaymentBut = sendPaymentBut;
    }

    public Button getChangeTrustBut() {
        return changeTrustBut;
    }

    public void setChangeTrustBut(Button changeTrustBut) {
        this.changeTrustBut = changeTrustBut;
    }


    public Button getAccountMergeBut() {
        return accountMergeBut;
    }

    public void setAccountMergeBut(Button accountMergeBut) {
        this.accountMergeBut = accountMergeBut;
    }

    public Button getCreateAssetBut() {
        return createAssetBut;
    }

    public void setCreateAssetBut(Button createAssetBut) {
        this.createAssetBut = createAssetBut;
    }

    public AnchorPane getRightAncPane() {
        return rightAncPane;
    }

    public void setRightAncPane(AnchorPane rightAncPane) {
        this.rightAncPane = rightAncPane;
    }

    private Pane getCreateAccountPane() {
        return createAccountPane;
    }

    public void setCreateAccountPane(Pane createAccountPane) {
        this.createAccountPane = createAccountPane;
    }

    private Pane getSendPaymentPane() {
        return sendPaymentPane;
    }

    public void setSendPaymentPane(Pane sendPaymentPane) {
        this.sendPaymentPane = sendPaymentPane;
    }

    private Pane getChangeTrustPane() {
        return changeTrustPane;
    }

    public void setChangeTrustPane(Pane changeTrustPane) {
        this.changeTrustPane = changeTrustPane;
    }


    private Pane getAccountMergePane() {
        return accountMergePane;
    }

    public void setAccountMergePane(Pane accountMergePane) {
        this.accountMergePane = accountMergePane;
    }

    private Pane getCreateAssetPane() {
        return createAssetPane;
    }

    public void setCreateAssetPane(Pane createAssetPane) {
        this.createAssetPane = createAssetPane;
    }

    private Pane getDefaultPane() {
        return defaultPane;
    }

    public void setDefaultPane(Pane defaultPane) {
        this.defaultPane = defaultPane;
    }

    private Pane getMainAccountPane() {
        return mainAccountPane;
    }

    public void setMainAccountPane(Pane mainAccountPane) {
        this.mainAccountPane = mainAccountPane;
    }

    private PasswordField getPrivateKeyField() {
        return privateKeyField;
    }

    public void setPrivateKeyField(PasswordField privateKeyField) {
        this.privateKeyField = privateKeyField;
    }

    public Button getDefaultEnterButton() {
        return defaultEnterButton;
    }

    public void setDefaultEnterButton(Button defaultEnterButton) {
        this.defaultEnterButton = defaultEnterButton;
    }


    public CreateAccount getCreateAccount() {
        return createAccount;
    }

    public void setCreateAccount(CreateAccount createAccount) {
        this.createAccount = createAccount;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public KeyPair getUserKey() {
        return userKey;
    }

    public void setUserKey(KeyPair userKey) {
        this.userKey = userKey;
    }

    public Label getPublicKeyLabel() {
        return publicKeyLabel;
    }

    public void setPublicKeyLabel(Label publicKeyLabel) {
        this.publicKeyLabel = publicKeyLabel;
    }

    public Label getPrivateKeyLabel() {
        return privateKeyLabel;
    }

    public void setPrivateKeyLabel(Label privateKeyLabel) {
        this.privateKeyLabel = privateKeyLabel;
    }

    private Label getNativeBalanceLabel() {
        return nativeBalanceLabel;
    }

    public void setNativeBalanceLabel(Label nativeBalanceLabel) {
        this.nativeBalanceLabel = nativeBalanceLabel;
    }

    private AccountDetails getAccountDetails() {
        return accountDetails;
    }

    public void setAccountDetails(AccountDetails accountDetails) {
        this.accountDetails = accountDetails;
    }

    public Boolean getMainNet() {
        return isMainNet;
    }

    public void setMainNet(Boolean mainNet) {
        isMainNet = mainNet;
    }

    private CheckBox getTestNetCheckBox() {
        return testNetCheckBox;
    }

    public void setTestNetCheckBox(CheckBox testNetCheckBox) {
        this.testNetCheckBox = testNetCheckBox;
    }

    private TextField getPublicKeyTxtField() {
        return publicKeyTxtField;
    }

    public void setPublicKeyTxtField(TextField publicKeyTxtField) {
        this.publicKeyTxtField = publicKeyTxtField;
    }

    private TextField getPrivateKeyTxtField() {
        return privateKeyTxtField;
    }

    public void setPrivateKeyTxtField(TextField privateKeyTxtField) {
        this.privateKeyTxtField = privateKeyTxtField;
    }

    public Button getShowPvtKeyButton() {
        return showPvtKeyButton;
    }

    public void setShowPvtKeyButton(Button showPvtKeyButton) {
        this.showPvtKeyButton = showPvtKeyButton;
    }

    private TableView getBalancesTable() {
        return balancesTable;
    }

    public void setBalancesTable(TableView balancesTable) {
        this.balancesTable = balancesTable;
    }

    private TableColumn getAssetColumn() {
        return assetColumn;
    }

    public void setAssetColumn(TableColumn assetColumn) {
        this.assetColumn = assetColumn;
    }

    private TableColumn getBalanceColumn() {
        return balanceColumn;
    }

    public void setBalanceColumn(TableColumn balanceColumn) {
        this.balanceColumn = balanceColumn;
    }

    private TableView getTransactionTable() {
        return transactionTable;
    }

    public void setTransactionTable(TableView transactionTable) {
        this.transactionTable = transactionTable;
    }

    private TableColumn getTxAssetColumn() {
        return txAssetColumn;
    }

    public void setTxAssetColumn(TableColumn txAssetColumn) {
        this.txAssetColumn = txAssetColumn;
    }


    private TableColumn getTxAmountColumn() {
        return txAmountColumn;
    }

    public void setTxAmountColumn(TableColumn txAmountColumn) {
        this.txAmountColumn = txAmountColumn;
    }

    private TableColumn getTxTimeColumn() {
        return txTimeColumn;
    }

    public void setTxTimeColumn(TableColumn txTimeColumn) {
        this.txTimeColumn = txTimeColumn;
    }

    public Label getPvtKeyLabel() {
        return pvtKeyLabel;
    }

    public void setPvtKeyLabel(Label pvtKeyLabel) {
        this.pvtKeyLabel = pvtKeyLabel;
    }

    private TextField getPvtKeyTxtField() {
        return pvtKeyTxtField;
    }

    public void setPvtKeyTxtField(TextField pvtKeyTxtField) {
        this.pvtKeyTxtField = pvtKeyTxtField;
    }

    private Label getPvtKeyWarning() {
        return pvtKeyWarning;
    }

    public void setPvtKeyWarning(Label pvtKeyWarning) {
        this.pvtKeyWarning = pvtKeyWarning;
    }


    private Button getCreateAccountButton() {
        return createAccountButton;
    }

    public void setCreateAccountButton(Button createAccountButton) {
        this.createAccountButton = createAccountButton;
    }

    private Button getUseAccountButton() {
        return useAccountButton;
    }

    public void setUseAccountButton(Button useAccountButton) {
        this.useAccountButton = useAccountButton;
    }

    public Label getPublicKeyLabelCA() {
        return publicKeyLabelCA;
    }

    public void setPublicKeyLabelCA(Label publicKeyLabelCA) {
        this.publicKeyLabelCA = publicKeyLabelCA;
    }

    private TextField getPublicKeyTxtFieldCA() {
        return publicKeyTxtFieldCA;
    }

    public void setPublicKeyTxtFieldCA(TextField publicKeyTxtFieldCA) {
        this.publicKeyTxtFieldCA = publicKeyTxtFieldCA;
    }

    private CheckBox getTestAccountCheckBox() {
        return testAccountCheckBox;
    }

    public void setTestAccountCheckBox(CheckBox testAccountCheckBox) {
        this.testAccountCheckBox = testAccountCheckBox;
    }

    private Label getAccountBalanceLabel() {
        return accountBalanceLabel;
    }

    public void setAccountBalanceLabel(Label accountBalanceLabel) {
        this.accountBalanceLabel = accountBalanceLabel;
    }

    private TextField getAccountTextField() {
        return accountTextField;
    }

    public void setAccountTextField(TextField accountTextField) {
        this.accountTextField = accountTextField;
    }

    private TableView getPaymentTable() {
        return paymentTable;
    }

    public void setPaymentTable(TableView paymentTable) {
        this.paymentTable = paymentTable;
    }

    private TableColumn getPaymentAssetCol() {
        return paymentAssetCol;
    }

    public void setPaymentAssetCol(TableColumn paymentAssetCol) {
        this.paymentAssetCol = paymentAssetCol;
    }

    private TableColumn getPaymentBalanceCol() {
        return paymentBalanceCol;
    }

    public void setPaymentBalanceCol(TableColumn paymentBalanceCol) {
        this.paymentBalanceCol = paymentBalanceCol;
    }

    private TextField getPaymentDestAddrFld() {
        return paymentDestAddrFld;
    }

    public void setPaymentDestAddrFld(TextField paymentDestAddrFld) {
        this.paymentDestAddrFld = paymentDestAddrFld;
    }

    private TextField getPaymentAmountFld() {
        return paymentAmountFld;
    }

    public void setPaymentAmountFld(TextField paymentAmountFld) {
        this.paymentAmountFld = paymentAmountFld;
    }

    private TextField getPaymentMemoFld() {
        return paymentMemoFld;
    }

    public void setPaymentMemoFld(TextField paymentMemoFld) {
        this.paymentMemoFld = paymentMemoFld;
    }

    private ComboBox getPaymentChoiceBox() {
        return paymentChoiceBox;
    }

    public void setPaymentChoiceBox(ComboBox paymentChoiceBox) {
        this.paymentChoiceBox = paymentChoiceBox;
    }

    private Button getPaymentSendButton() {
        return paymentSendButton;
    }

    public void setPaymentSendButton(Button paymentSendButton) {
        this.paymentSendButton = paymentSendButton;
    }

    private Button getViewInExplorerButton() {
        return viewInExplorerButton;
    }

    public void setViewInExplorerButton(Button viewInExplorerButton) {
        this.viewInExplorerButton = viewInExplorerButton;
    }

    public String getLastTxHash() {
        return lastTxHash;
    }

    public void setLastTxHash(String lastTxHash) {
        MainMenuController.lastTxHash = lastTxHash;
    }

    public Button getTrustLinesButton() {
        return trustLinesButton;
    }

    public void setTrustLinesButton(Button trustLinesButton) {
        this.trustLinesButton = trustLinesButton;
    }

    private Pane getTrustLinesPane() {
        return trustLinesPane;
    }

    public void setTrustLinesPane(Pane trustLinesPane) {
        this.trustLinesPane = trustLinesPane;
    }

    private TextArea getXdrTextArea() {
        return xdrTextArea;
    }

    public void setXdrTextArea(TextArea xdrTextArea) {
        this.xdrTextArea = xdrTextArea;
    }

    private Label getWhichNetLabel() {
        return whichNetLabel;
    }

    public void setWhichNetLabel(Label whichNetLabel) {
        this.whichNetLabel = whichNetLabel;
    }

    private TableView getTxPaneTable() {
        return txPaneTable;
    }

    public void setTxPaneTable(TableView txPaneTable) {
        this.txPaneTable = txPaneTable;
    }

    private TableColumn getTxPaneAssetCol() {
        return txPaneAssetCol;
    }

    public void setTxPaneAssetCol(TableColumn txPaneAssetCol) {
        this.txPaneAssetCol = txPaneAssetCol;
    }

    private TableColumn getTxPaneAmountCol() {
        return txPaneAmountCol;
    }

    public void setTxPaneAmountCol(TableColumn txPaneAmountCol) {
        this.txPaneAmountCol = txPaneAmountCol;
    }

    private TableColumn getTxPaneDateCol() {
        return txPaneDateCol;
    }

    public void setTxPaneDateCol(TableColumn txPaneDateCol) {
        this.txPaneDateCol = txPaneDateCol;
    }

    private TableColumn getTxPaneToFromCol() {
        return txPaneToFromCol;
    }

    public void setTxPaneToFromCol(TableColumn txPaneToFromCol) {
        this.txPaneToFromCol = txPaneToFromCol;
    }

    private Pane getViewTransactionsPane() {
        return viewTransactionsPane;
    }

    public ChangeTrust getChangeTrust() {
        return changeTrust;
    }

    public Button getSearchForAssetBut() {
        return searchForAssetBut;
    }

    public Button getKnowTheAssetBut() {
        return knowTheAssetBut;
    }

    private Pane getCtMainPane() {
        return ctMainPane;
    }

    private Pane getCtSelectTrustPane() {
        return ctSelectTrustPane;
    }

    private TextField getCtAssetSearchAssetCodeTF() {
        return ctAssetSearchAssetCodeTF;
    }

    private TextField getCtAssetSearchIssuerTF() {
        return ctAssetSearchIssuerTF;
    }

    private TextArea getCtAssetSearchReponseTA() {
        return ctAssetSearchReponseTA;
    }

    private ComboBox getCtComboBox() {
        return ctComboBox;
    }

    private Pane getCtChangeTrustPane() {
        return ctChangeTrustPane;
    }

    private TextField getCtChangeTrustAssetTF() {
        return ctChangeTrustAssetTF;
    }

    private TextField getCtChangeTrustLimitTF() {
        return ctChangeTrustLimitTF;
    }

    private Button getCtChangeTrustButton() {
        return ctChangeTrustButton;
    }

    private Pane getCaCreationPane() {
        return caCreationPane;
    }

    private Pane getCaResponsePane() {
        return caResponsePane;
    }

    private CheckBox getAuthRequiredCheckBox() {
        return authRequiredCheckBox;
    }

    private CheckBox getAuthRevocableCheckBox() {
        return authRevocableCheckBox;
    }

    private ComboBox getAssetTypeComboBox() {
        return assetTypeComboBox;
    }

    private CheckBox getYesCheckBox() {
        return yesCheckBox;
    }

    private CheckBox getNoCheckBox() {
        return noCheckBox;
    }

    private TextField getCaAssetCodeTextField() {
        return caAssetCodeTextField;
    }

    private TextField getCaTrustLimitTextField() {
        return caTrustLimitTextField;
    }

    private TextField getCaTomlLocationTextField() {
        return caTomlLocationTextField;
    }

    private TextField getCaRecvLocationTextField() {
        return caRecvLocationTextField;
    }

    private TextField getCaAmtSendTextBox() {
        return caAmtSendTextBox;
    }

    private Button getCaCreateButton() {
        return caCreateButton;
    }

    private TextArea getCaResponsePaneTA() {
        return caResponsePaneTA;
    }

    private Label getCtChangeTrustErrorLabel() {
        return ctChangeTrustErrorLabel;
    }

    private Label getCtChangeTrustIssuerAccountLabel() {
        return ctChangeTrustIssuerAccountLabel;
    }

    private TextField getCtChangeTrustIssuerAccountTF() {
        return ctChangeTrustIssuerAccountTF;
    }
}

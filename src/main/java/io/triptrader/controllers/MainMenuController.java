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

import io.triptrader.models.AccountDetails;
import io.triptrader.models.CreateAccount;
import io.triptrader.models.Payment;
import io.triptrader.models.Validate;
import io.triptrader.utilities.Alerts;
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
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.xdr.MemoType;

import java.io.IOException;
import java.net.URL;
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

    /******** PAGE INITS ******/
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        getDefaultPane().setVisible(true);
    }

    private void initAccountDetails ( )
    {
        getMainAccountPane().setVisible(true);
        accountDetails = new AccountDetails ( userKey );

        // public key
        getPublicKeyTxtField().setText( userKey.getAccountId() );
        try {
            getNativeBalanceLabel().setText ( accountDetails.getNativeBalance ( isMainNet ) );
            initBalancesTable();
            initTransactionsTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCreateAccount ( )
    {
        getDefaultPane().setVisible(false);
        getMainAccountPane().setVisible(false);
        getAccountMergePane().setVisible(false);
        getAllowTrustPane().setVisible(false);
        getChangeTrustPane().setVisible(false);
        getCreateAssetPane().setVisible(false);
        getSendPaymentPane().setVisible(false);

        getCreateAccountPane().setVisible(true);
    }

    private void initDefaultPane ( )
    {
        getSendPaymentPane().setVisible(false);
        getMainAccountPane().setVisible(false);
        getAccountMergePane().setVisible(false);
        getAllowTrustPane().setVisible(false);
        getChangeTrustPane().setVisible(false);
        getCreateAssetPane().setVisible(false);
        getCreateAccountPane().setVisible(false);

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
            getDefaultPane().setVisible(false);
            getMainAccountPane().setVisible(false);
            getAccountMergePane().setVisible(false);
            getAllowTrustPane().setVisible(false);
            getChangeTrustPane().setVisible(false);
            getCreateAssetPane().setVisible(false);
            getCreateAccountPane().setVisible(false);

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
    /********** Controller Methods ********/
    @SuppressWarnings("unchecked")
    private void initBalancesTable ( ) throws IOException
    {
        getAssetColumn().setCellValueFactory ( new PropertyValueFactory<>("assetName") );
        getBalanceColumn().setCellValueFactory ( new PropertyValueFactory<>("assetBalance") );

        getBalancesTable().setItems ( accountDetails.getAssetBalances ( isMainNet ) );
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
    private void initTransactionsTable ( ) throws IOException
    {
        if ( accountDetails == null )
            accountDetails = new AccountDetails ( userKey );

        getTxAssetColumn().setCellValueFactory ( new PropertyValueFactory<>("assetName") );
        getTxAmountColumn().setCellValueFactory ( new PropertyValueFactory<>("amount") );
        getTxTimeColumn().setCellValueFactory ( new PropertyValueFactory<>("date") );
        getTxMemoColumn().setCellValueFactory ( new PropertyValueFactory<>("memo") );

        getTransactionTable().setItems( accountDetails.getTransactions ( isMainNet ) );
        getTransactionTable().getColumns().clear();
        getTransactionTable().getColumns().setAll ( getTxAssetColumn(), getTxAmountColumn(), getTxTimeColumn(), getTxMemoColumn() );
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
        if ( getTestAccountCheckBox().isSelected() ) {
            try {
                String response = createAccount.createTestAccount ( newAccount );
                Alert alert = new Alert(Alert.AlertType.INFORMATION, response);
                alert.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        getPvtKeyTxtField().setVisible(true);
        getPvtKeyTxtField().setText( new String ( newAccount.getSecretSeed() ) );
        getPvtKeyWarning().setVisible(true);
        getPublicKeyTxtFieldCA().setVisible(true);
        getPublicKeyTxtFieldCA().setText ( newAccount.getAccountId() );
        getUseAccountButton().setVisible(true);

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
                getAccountBalanceLabel().setText( new AccountDetails( userKey ).getNativeBalance(isMainNet));
                getAccountTextField().setText ( userKey.getAccountId() );
            } catch ( Exception e) {
                e.printStackTrace();
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
        if ( getTestAccountCheckBox().isSelected() )
            isMainNet = false;
        else
            isMainNet = true;


        
            if ( getAccountDetails() == null )
                accountDetails = new AccountDetails ( userKey );
            else
                getAccountDetails().setPair ( userKey );

            getAccountTextField().setText( userKey.getAccountId() );
            getAccountBalanceLabel().setText( accountDetails.getNativeBalance ( isMainNet ) );

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

                return;
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
                Parent root = FXMLLoader.load( getClass().getClassLoader().getResource("fxml/StellarChain.fxml") );
                Stage stage = new Stage();
                stage.setTitle("Stellar Chain - " + lastTxHash );
                stage.setScene(new Scene(root, 1000, 700));
                stage.show();
                // Hide this current window (if this is what you want)
                //((Node)(event.getSource())).getScene().getWindow().hide();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    private Button allowTrustBut;
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
    private Pane allowTrustPane;
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
    private TableColumn txMemoColumn;
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

    public Button getAllowTrustBut() {
        return allowTrustBut;
    }

    public void setAllowTrustBut(Button allowTrustBut) {
        this.allowTrustBut = allowTrustBut;
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

    public Pane getCreateAccountPane() {
        return createAccountPane;
    }

    public void setCreateAccountPane(Pane createAccountPane) {
        this.createAccountPane = createAccountPane;
    }

    public Pane getSendPaymentPane() {
        return sendPaymentPane;
    }

    public void setSendPaymentPane(Pane sendPaymentPane) {
        this.sendPaymentPane = sendPaymentPane;
    }

    public Pane getChangeTrustPane() {
        return changeTrustPane;
    }

    public void setChangeTrustPane(Pane changeTrustPane) {
        this.changeTrustPane = changeTrustPane;
    }

    public Pane getAllowTrustPane() {
        return allowTrustPane;
    }

    public void setAllowTrustPane(Pane allowTrustPane) {
        this.allowTrustPane = allowTrustPane;
    }

    public Pane getAccountMergePane() {
        return accountMergePane;
    }

    public void setAccountMergePane(Pane accountMergePane) {
        this.accountMergePane = accountMergePane;
    }

    public Pane getCreateAssetPane() {
        return createAssetPane;
    }

    public void setCreateAssetPane(Pane createAssetPane) {
        this.createAssetPane = createAssetPane;
    }

    public Pane getDefaultPane() {
        return defaultPane;
    }

    public void setDefaultPane(Pane defaultPane) {
        this.defaultPane = defaultPane;
    }

    public Pane getMainAccountPane() {
        return mainAccountPane;
    }

    public void setMainAccountPane(Pane mainAccountPane) {
        this.mainAccountPane = mainAccountPane;
    }

    public PasswordField getPrivateKeyField() {
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

    public Label getNativeBalanceLabel() {
        return nativeBalanceLabel;
    }

    public void setNativeBalanceLabel(Label nativeBalanceLabel) {
        this.nativeBalanceLabel = nativeBalanceLabel;
    }

    public AccountDetails getAccountDetails() {
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

    public CheckBox getTestNetCheckBox() {
        return testNetCheckBox;
    }

    public void setTestNetCheckBox(CheckBox testNetCheckBox) {
        this.testNetCheckBox = testNetCheckBox;
    }

    public TextField getPublicKeyTxtField() {
        return publicKeyTxtField;
    }

    public void setPublicKeyTxtField(TextField publicKeyTxtField) {
        this.publicKeyTxtField = publicKeyTxtField;
    }

    public TextField getPrivateKeyTxtField() {
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

    public TableView getBalancesTable() {
        return balancesTable;
    }

    public void setBalancesTable(TableView balancesTable) {
        this.balancesTable = balancesTable;
    }

    public TableColumn getAssetColumn() {
        return assetColumn;
    }

    public void setAssetColumn(TableColumn assetColumn) {
        this.assetColumn = assetColumn;
    }

    public TableColumn getBalanceColumn() {
        return balanceColumn;
    }

    public void setBalanceColumn(TableColumn balanceColumn) {
        this.balanceColumn = balanceColumn;
    }

    public TableView getTransactionTable() {
        return transactionTable;
    }

    public void setTransactionTable(TableView transactionTable) {
        this.transactionTable = transactionTable;
    }

    public TableColumn getTxAssetColumn() {
        return txAssetColumn;
    }

    public void setTxAssetColumn(TableColumn txAssetColumn) {
        this.txAssetColumn = txAssetColumn;
    }


    public TableColumn getTxMemoColumn() {
        return txMemoColumn;
    }

    public void setTxMemoColumn(TableColumn txMemoColumn) {
        this.txMemoColumn = txMemoColumn;
    }

    public TableColumn getTxAmountColumn() {
        return txAmountColumn;
    }

    public void setTxAmountColumn(TableColumn txAmountColumn) {
        this.txAmountColumn = txAmountColumn;
    }

    public TableColumn getTxTimeColumn() {
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

    public TextField getPvtKeyTxtField() {
        return pvtKeyTxtField;
    }

    public void setPvtKeyTxtField(TextField pvtKeyTxtField) {
        this.pvtKeyTxtField = pvtKeyTxtField;
    }

    public Label getPvtKeyWarning() {
        return pvtKeyWarning;
    }

    public void setPvtKeyWarning(Label pvtKeyWarning) {
        this.pvtKeyWarning = pvtKeyWarning;
    }


    public Button getCreateAccountButton() {
        return createAccountButton;
    }

    public void setCreateAccountButton(Button createAccountButton) {
        this.createAccountButton = createAccountButton;
    }

    public Button getUseAccountButton() {
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

    public TextField getPublicKeyTxtFieldCA() {
        return publicKeyTxtFieldCA;
    }

    public void setPublicKeyTxtFieldCA(TextField publicKeyTxtFieldCA) {
        this.publicKeyTxtFieldCA = publicKeyTxtFieldCA;
    }

    public CheckBox getTestAccountCheckBox() {
        return testAccountCheckBox;
    }

    public void setTestAccountCheckBox(CheckBox testAccountCheckBox) {
        this.testAccountCheckBox = testAccountCheckBox;
    }

    public Label getAccountBalanceLabel() {
        return accountBalanceLabel;
    }

    public void setAccountBalanceLabel(Label accountBalanceLabel) {
        this.accountBalanceLabel = accountBalanceLabel;
    }

    public TextField getAccountTextField() {
        return accountTextField;
    }

    public void setAccountTextField(TextField accountTextField) {
        this.accountTextField = accountTextField;
    }

    public TableView getPaymentTable() {
        return paymentTable;
    }

    public void setPaymentTable(TableView paymentTable) {
        this.paymentTable = paymentTable;
    }

    public TableColumn getPaymentAssetCol() {
        return paymentAssetCol;
    }

    public void setPaymentAssetCol(TableColumn paymentAssetCol) {
        this.paymentAssetCol = paymentAssetCol;
    }

    public TableColumn getPaymentBalanceCol() {
        return paymentBalanceCol;
    }

    public void setPaymentBalanceCol(TableColumn paymentBalanceCol) {
        this.paymentBalanceCol = paymentBalanceCol;
    }

    public TextField getPaymentDestAddrFld() {
        return paymentDestAddrFld;
    }

    public void setPaymentDestAddrFld(TextField paymentDestAddrFld) {
        this.paymentDestAddrFld = paymentDestAddrFld;
    }

    public TextField getPaymentAmountFld() {
        return paymentAmountFld;
    }

    public void setPaymentAmountFld(TextField paymentAmountFld) {
        this.paymentAmountFld = paymentAmountFld;
    }

    public TextField getPaymentMemoFld() {
        return paymentMemoFld;
    }

    public void setPaymentMemoFld(TextField paymentMemoFld) {
        this.paymentMemoFld = paymentMemoFld;
    }

    public ComboBox getPaymentChoiceBox() {
        return paymentChoiceBox;
    }

    public void setPaymentChoiceBox(ComboBox paymentChoiceBox) {
        this.paymentChoiceBox = paymentChoiceBox;
    }

    public Button getPaymentSendButton() {
        return paymentSendButton;
    }

    public void setPaymentSendButton(Button paymentSendButton) {
        this.paymentSendButton = paymentSendButton;
    }

    public Button getViewInExplorerButton() {
        return viewInExplorerButton;
    }

    public void setViewInExplorerButton(Button viewInExplorerButton) {
        this.viewInExplorerButton = viewInExplorerButton;
    }

    public String getLastTxHash() {
        return lastTxHash;
    }

    public void setLastTxHash(String lastTxHash) {
        this.lastTxHash = lastTxHash;
    }
}

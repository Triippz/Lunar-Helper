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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.stellar.sdk.KeyPair;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class MainMenuController implements Initializable
{
    /*********** GLOBAL VARIABLES *********/
    private KeyPair userKey;
    private Boolean isMainNet;

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

    /********** Controller Methods ********/
    @SuppressWarnings("unchecked")
    private void initBalancesTable ( ) throws IOException
    {
        getAssetColumn().setCellValueFactory ( new PropertyValueFactory<>("assetNameColumn") );
        getBalanceColumn().setCellValueFactory ( new PropertyValueFactory<>("assetBalanceColumn") );

        getBalancesTable().setItems ( accountDetails.getAssetBalances ( isMainNet ) );
        getBalancesTable().getColumns().clear();
        getBalancesTable().getColumns().setAll( getAssetColumn(), getBalanceColumn() );
    }

    @SuppressWarnings("unchecked")
    private void initTransactionsTable ( ) throws IOException
    {
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
        getRecoveryPhraseTxtField().setVisible(true);
        getRecoveryPhraseWarning().setVisible(true);
        getShowPhraseButton().setVisible(true);
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
        getRecoveryPhraseTxtField().setText( new String ( newAccount.getSignatureHint().getSignatureHint() ) );
        getRecoveryPhraseLabel().setVisible(true);
        getShowPhraseButton().setVisible(true);
        getUseAccountButton().setVisible(true);

    }

    /********** SCENE ACTIONS ************/
    @FXML
    public void onDefaultEnter ( MouseEvent event )
    {
        // create the keypair
        userKey = KeyPair.fromSecretSeed ( getPrivateKeyField().getText() );

        // are we using the main or test net?
        if ( getTestNetCheckBox().isSelected() )
            isMainNet = false;
        else
            isMainNet = true;

        try {
            getAccountBalanceLabel().setText( new AccountDetails( userKey ).getNativeBalance(isMainNet));
            getAccountTextField().setText ( userKey.getAccountId() );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // disable the default pane and make the account pane visible
        getDefaultPane().setVisible(false);
        initAccountDetails();
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


        try {
            if ( getAccountDetails() == null )
                accountDetails = new AccountDetails ( userKey );
            else
                getAccountDetails().setPair ( userKey );

            getAccountTextField().setText( userKey.getAccountId() );
            getAccountBalanceLabel().setText( accountDetails.getNativeBalance ( isMainNet ) );
        } catch (IOException e) {
            System.out.println(e.getCause());
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
    private Label recoveryPhraseLabel;
    @FXML
    private TextArea recoveryPhraseTxtField;
    @FXML
    private Label recoveryPhraseWarning;
    @FXML
    private Button createAccountButton;
    @FXML
    private Button showPhraseButton;
    @FXML
    private Button useAccountButton;
    @FXML
    private CheckBox testAccountCheckBox;
    @FXML
    private Label accountBalanceLabel;
    @FXML
    private TextField accountTextField;

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


    public Label getRecoveryPhraseLabel() {
        return recoveryPhraseLabel;
    }

    public void setRecoveryPhraseLabel(Label recoveryPhraseLabel) {
        this.recoveryPhraseLabel = recoveryPhraseLabel;
    }

    public TextArea getRecoveryPhraseTxtField() {
        return recoveryPhraseTxtField;
    }

    public void setRecoveryPhraseTxtField(TextArea recoveryPhraseTxtField) {
        this.recoveryPhraseTxtField = recoveryPhraseTxtField;
    }

    public Label getRecoveryPhraseWarning() {
        return recoveryPhraseWarning;
    }

    public void setRecoveryPhraseWarning(Label recoveryPhraseWarning) {
        this.recoveryPhraseWarning = recoveryPhraseWarning;
    }

    public Button getCreateAccountButton() {
        return createAccountButton;
    }

    public void setCreateAccountButton(Button createAccountButton) {
        this.createAccountButton = createAccountButton;
    }

    public Button getShowPhraseButton() {
        return showPhraseButton;
    }

    public void setShowPhraseButton(Button showPhraseButton) {
        this.showPhraseButton = showPhraseButton;
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
}

package cryptox_AndroidTest.Transactions;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import config.RetryAnalyzer;
import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.swipe;
import static pages.generalMethods.*;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;
import org.testng.asserts.SoftAssert;

public class PLTTransactions {

    protected static SoftAssert softAssert = new SoftAssert();
    public String PLT_TOKEN = "PLTToken";
    public String PLT_FOR_ANDROID_TOKEN = "PLTforAndroid";
    public String PLT_IN_DENY_TOKEN = "PLTinDenyList";
    public String PLT_PAUSED_TOKEN = "PLTPaused";
    public String PLT_LEVEL_TOKEN = "PLTLEVEL";
    public String PLT_TRANSFER_VALIDATION = "(//android.view.ViewGroup[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/main_container\"])[1]";
    public String PLT_ACCOUNT_ONE = "4j5R…eNHj";
    public String PLT_ACCOUNT_TWO = "4kRC…YSRt";
    public String PLT_DENY_ACCOUNT = "3s9F…BApW";
    public String PLT_PAUSED_ACCOUNT = "311N…KjPW";
    public static final String TEXT_ALLOW_LIST_NOT_MEMBER = "Account not on the allow list";
    public static final String TEXT_ALLOW_LIST_MEMBER = "Account is on the allow list";
    public static final String TEXT_DENY_LIST_MEMBER = "Account on the deny list";
    public static final String TEXT_TOKEN_PAUSED = "Token paused";
    public static final String TITLE_ALLOW_DENY_LIST = "Allow & Deny Lists";
    public static final String TITLE_PAUSED_TOKENS = "Paused Tokens";
    public static final String DETAILS_ALLOW_DENY_LIST = "Protocol-level tokens can optionally support allow and deny lists to control the ability of Concordium accounts to hold/transfer that PLT. Allow and deny lists are managed by the token’s governance account.";
    public static final String DETAILS_TOKEN_PAUSED = "A paused token refers to a protocol-level token (PLT) whose balance-affecting operations, such as transfers, minting, and burning, are temporarily disabled by the token’s governance account. When a token is paused, any attempt to perform these actions will fail until the token is unpaused.";
    public String MEMO = "//android.widget.TextView[@text='This is Test Memo - Triggered through automated tests']";

      @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_if_user_transfer_PLT_to_specific_account_with_memo() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_ONE, 20));
        Assert.assertTrue(clickOnElement("menuitem_transfer", 20));
        Assert.assertTrue(clickOnElement("token_arrow", 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertTrue(SendTextToField("amount", "0.1", 20));
        Assert.assertTrue(clickOnElement("memo_layout", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(SendTextToField("memo_edittext", "This is Test Memo - Triggered through automated tests", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        Assert.assertTrue(clickOnElement("recipient_layout", 20));
        Assert.assertTrue(clickOnElement("edittext", 20));
        Assert.assertTrue(SendTextToField("edittext", "4kRCVvjUxBGZyAmcHGP2mV9J4QvnXs5GYBaMTtmisg4DznYSRt", 20));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish", 20));
        Assert.assertTrue(clickOnElement("menuitem_activity", 20));
        Assert.assertTrue(clickOnElementByXpath(PLT_TRANSFER_VALIDATION, 20));
        Assert.assertTrue(verifyElementByXpath(MEMO, 20));
    }

      @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_if_user_transfer_PLT_to_specific_account_without_memo() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_ONE, 20));
        Assert.assertTrue(clickOnElement("menuitem_transfer", 20));
        Assert.assertTrue(clickOnElement("token_arrow", 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertTrue(SendTextToField("amount", "0.1", 20));
        Assert.assertTrue(clickOnElement("recipient_layout", 20));
        Assert.assertTrue(clickOnElement("edittext", 20));
        Assert.assertTrue(SendTextToField("edittext", "4kRCVvjUxBGZyAmcHGP2mV9J4QvnXs5GYBaMTtmisg4DznYSRt", 20));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish", 20));
        Assert.assertTrue(clickOnElement("menuitem_activity", 20));
        Assert.assertTrue(clickOnElementByXpath(PLT_TRANSFER_VALIDATION, 20));
        Assert.assertTrue(elementShouldNotAvailable(MEMO, 20));
    }

     @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_screen_with_metadata() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_ONE, 20));
        Assert.assertTrue(clickOnToken(PLT_LEVEL_TOKEN, 20));
        softAssert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElement("raw_metadata_label", 20));
        Assert.assertTrue(verifyElementById("details_text_view", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(verifyElementByXpath("//android.widget.TextView[@text=\"Description\"]", 20));
    }

     @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_screen_without_metadata() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_ONE, 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertTrue(elementShouldNotAvailable("raw_metadata_label", 20));
        Assert.assertTrue(elementShouldNotAvailable("details_text_view", 20));
        Assert.assertTrue(elementShouldNotAvailable("deny_button", 20));
        Assert.assertTrue(elementShouldNotAvailable("//android.widget.TextView[@text=\"Description\"]", 20));
    }

     @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_when_sender_is_not_in_allow_list() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_TWO, 20));
        Assert.assertTrue(clickOnElement("manage_tokens", 20));
        Assert.assertTrue(clickOnElement("toolbar_plus_btn_add_contact_image", 20));
        Assert.assertTrue(SendTextToField("edittext", "PLTForAndroid", 20));
        Assert.assertTrue(clickOnElement("search_icon", 20));
        Assert.assertTrue(clickOnElement("selection", 20));
        Assert.assertTrue(clickOnElement("add_tokens_btn", 20));
        Assert.assertTrue(clickOnElement("toolbar_back_btn", 20));
        Assert.assertTrue(clickOnToken(PLT_FOR_ANDROID_TOKEN, 20));
        Assert.assertTrue(verifyTextById("list_status_title", TEXT_ALLOW_LIST_NOT_MEMBER, 20));
        Assert.assertTrue(clickOnElement("list_status_title", 20));
        Assert.assertTrue(verifyTextById("title_text_view", TITLE_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(verifyTextById("details_text_view", DETAILS_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElement("toolbar_back_btn", 20));
        Assert.assertTrue(clickOnElement("menuitem_transfer", 20));
        Assert.assertTrue(clickOnElement("token_arrow", 20));
        Assert.assertTrue(elementShouldNotAvailable(PLT_FOR_ANDROID_TOKEN, 20));
    }

     @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_when_sender_is_in_allow_list() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_TWO, 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertTrue(verifyTextById("list_status_title", TEXT_ALLOW_LIST_MEMBER, 20));
        Assert.assertTrue(clickOnElement("list_status_title", 20));
        Assert.assertTrue(verifyTextById("title_text_view", TITLE_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(verifyTextById("details_text_view", DETAILS_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElement("toolbar_back_btn", 20));
        Assert.assertTrue(clickOnElement("menuitem_transfer", 20));
        Assert.assertTrue(clickOnElement("token_arrow", 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertTrue(SendTextToField("amount", "0.1", 20));
        Assert.assertTrue(clickOnElement("recipient_layout", 20));
        Assert.assertTrue(clickOnElement("edittext", 20));
        Assert.assertTrue(SendTextToField("edittext", "4j5RYKNs4RQYT16kBxq4MY6djLQbuPbsec6eiR3Y7B4hCneNHj", 20));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(clickOnElement("continue_btn", 40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish", 20));
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_when_sender_is_in_deny_list() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_DENY_ACCOUNT, 20));
        Assert.assertTrue(clickOnToken(PLT_IN_DENY_TOKEN, 20));
        softAssert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(verifyTextById("list_status_title", TEXT_DENY_LIST_MEMBER, 20));
        Assert.assertTrue(clickOnElement("list_status_title", 20));
        Assert.assertTrue(verifyTextById("title_text_view", TITLE_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(verifyTextById("details_text_view", DETAILS_ALLOW_DENY_LIST, 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
    }

      @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_when_sender_is_not_in_deny_list() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_TWO, 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
      //  softAssert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertFalse(verifyTextById("list_status_title", TEXT_DENY_LIST_MEMBER, 20));
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_paused() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_PAUSED_ACCOUNT, 20));
        Assert.assertTrue(clickOnToken(PLT_PAUSED_TOKEN, 20));
        softAssert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(verifyTextById("list_status_title", TEXT_TOKEN_PAUSED, 20));
        Assert.assertTrue(clickOnElement("list_status_title", 20));
        Assert.assertTrue(verifyTextById("title_text_view", TITLE_PAUSED_TOKENS, 20));
        Assert.assertTrue(verifyTextById("details_text_view", DETAILS_TOKEN_PAUSED, 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
    }

     @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Verify_PLT_token_un_paused() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 20));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT_TWO, 20));
        Assert.assertTrue(clickOnToken(PLT_TOKEN, 20));
        Assert.assertFalse(verifyTextById("list_status_title", TEXT_TOKEN_PAUSED, 20));
    }
}
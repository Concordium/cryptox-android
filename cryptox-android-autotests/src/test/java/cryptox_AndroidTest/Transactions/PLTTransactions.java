package cryptox_AndroidTest.Transactions;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.swipe;
import static pages.generalMethods.*;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class PLTTransactions {

    public String PLT_TOKEN = "PLTToken";
    public String PLT_LEVEL_TOKEN = "PLTLEVEL";
    public String PLT_TRANSFER_VALIDATION = "(//android.view.ViewGroup[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/main_container\"])[1]";
    public String PLT_ACCOUNT = "4j5R…eNHj";
    public String MEMO = "//android.widget.TextView[@text='This is Test Memo - Triggered through automated tests']";
    @Test
    public void Verify_if_user_transfer_PLT_to_specific_account_with_memo() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("menuitem_transfer",10));
        Assert.assertTrue(clickOnElement("com.pioneeringtechventures.wallet.stagenet:id/token_arrow",10));
        Assert.assertTrue(clickOnToken(PLT_TOKEN,10));
        Assert.assertTrue(SendTextToField("amount","0.1", 20));
        Assert.assertTrue(clickOnElement("memo_layout", 20));
        Assert.assertTrue(clickOnElement("show_button",10));
        Assert.assertTrue(SendTextToField("memo_edittext","This is Test Memo - Triggered through automated tests", 20));
        Assert.assertTrue(clickOnElement("confirm_button",10));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("edittext",10));
        Assert.assertTrue(SendTextToField("edittext","4kRCVvjUxBGZyAmcHGP2mV9J4QvnXs5GYBaMTtmisg4DznYSRt", 20));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));
        Assert.assertTrue(clickOnElement("menuitem_activity",20));
        Assert.assertTrue(clickOnElementByXpath(PLT_TRANSFER_VALIDATION,10));
        Assert.assertTrue(verifyElementByXpath(MEMO,10));
    }

    @Test
    public void Verify_if_user_transfer_PLT_to_specific_account_without_memo() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 10));
        Assert.assertTrue(clickOnAccount(PLT_ACCOUNT,10));
        Assert.assertTrue(clickOnElement("menuitem_transfer",10));
        Assert.assertTrue(clickOnElement("com.pioneeringtechventures.wallet.stagenet:id/token_arrow",10));
        Assert.assertTrue(clickOnToken(PLT_TOKEN,10));
        Assert.assertTrue(SendTextToField("amount","0.1", 20));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("edittext",10));
        Assert.assertTrue(SendTextToField("edittext","4kRCVvjUxBGZyAmcHGP2mV9J4QvnXs5GYBaMTtmisg4DznYSRt", 20));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));
        Assert.assertTrue(clickOnElement("menuitem_activity",20));
        Assert.assertTrue(clickOnElementByXpath(PLT_TRANSFER_VALIDATION,10));
        Assert.assertTrue(elementShouldNotAvailable(MEMO,10));
    }

    @Test
    public void Verify_PLT_token_screen_with_metadata() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnToken(PLT_LEVEL_TOKEN,10));
        Assert.assertTrue(clickOnElement("keep_button",10));
        Assert.assertTrue(clickOnElement("raw_metadata_label",10));
        Assert.assertTrue(verifyElementById("details_text_view",10));
        Assert.assertTrue(clickOnElement("deny_button",10));
        Assert.assertTrue(verifyElementByXpath("//android.widget.TextView[@text=\"Description\"]",10));
    }

    @Test
    public void Verify_PLT_token_screen_without_metadata() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnToken(PLT_TOKEN,10));
        Assert.assertTrue(clickOnElement("keep_button",10));
        Assert.assertTrue(elementShouldNotAvailable("raw_metadata_label",10));
        Assert.assertTrue(elementShouldNotAvailable("details_text_view",10));
        Assert.assertTrue(elementShouldNotAvailable("deny_button",10));
        Assert.assertTrue(elementShouldNotAvailable("//android.widget.TextView[@text=\"Description\"]",10));
    }
}
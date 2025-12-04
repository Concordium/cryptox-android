package cryptox_AndroidTest.Transactions;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.performScroll;
import static pages.appOperations.commands.swipe;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class PLTTransactions {

    public String PLTToken = "(//android.view.ViewGroup[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/content\"])[2]";
    public String PLT_Token_Validation = "//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/total_textview\" and @text=\"-0.1 PLTToken\"]";
    public String PLT_Account = "//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/account_name\" and @text=\"4j5R…eNHj\"]";
    @Test
    public void Verify_if_user_transfer_PLT_to_Specific_account_with_memo() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("menuitem_transfer",10));
        Assert.assertTrue(clickOnElement("com.pioneeringtechventures.wallet.stagenet:id/token_arrow",10));
        Assert.assertTrue(clickOnElementByXpath(PLTToken,10));
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
        Assert.assertTrue(clickOnElement("menuitem_activity",10));
        Assert.assertTrue(clickOnElementByXpath(PLT_Token_Validation,10));
    }

    @Test
    public void Verify_if_user_transfer_PLT_to_Specific_account_without_memo() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 10));
        Assert.assertTrue(clickOnElementByXpath(PLT_Account,10));
        Assert.assertTrue(clickOnElement("menuitem_transfer",10));
        Assert.assertTrue(clickOnElement("com.pioneeringtechventures.wallet.stagenet:id/token_arrow",10));
        Assert.assertTrue(clickOnElementByXpath(PLTToken,10));
        Assert.assertTrue(SendTextToField("amount","0.1", 20));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("edittext",10));
        Assert.assertTrue(SendTextToField("edittext","4kRCVvjUxBGZyAmcHGP2mV9J4QvnXs5GYBaMTtmisg4DznYSRt", 20));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));
        Assert.assertTrue(clickOnElement("menuitem_activity",10));
        Assert.assertTrue(clickOnElementByXpath(PLT_Token_Validation,10));
    }
}
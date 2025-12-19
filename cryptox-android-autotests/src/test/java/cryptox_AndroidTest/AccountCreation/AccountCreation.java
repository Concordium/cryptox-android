package cryptox_AndroidTest.AccountCreation;

import config.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import static config.appiumconnection.driver;
import static config.appiumconnection.openAppiumSession;
import static cryptox_AndroidTest.baseClass.*;
import static pages.accountManagement.*;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.generalMethods.*;
import static pages.landingScreen.clickConnectButton;
import static pages.landingScreen.clickGetStarted;
import static pages.login.loginCryptoX;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;
import static pages.walletAccountCreationScreen.activateAccount;

public class AccountCreation {
    @Ignore
    public void createConcordionAccount() throws MalformedURLException {
        Assert.assertTrue(AcceptNotificationPopUp());
        Assert.assertTrue(clickConnectButton());
        Assert.assertTrue(clickGetStarted());
        Assert.assertTrue(activateAccount());
        Assert.assertTrue(createPassCodeNow());
        Assert.assertTrue(repeatPassCodeNow());
    }

   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Z_verify_if_a_user_can_create_an_account() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 10));
        Assert.assertTrue(clickOnElement("create_account_button", 10));
        Assert.assertTrue(SendTextToField("account_name_edittext", "AutomationAccount", 10));
        Assert.assertTrue(clickOnElement("next_button", 10));
        Assert.assertTrue(clickOnMostRecentIdentity());
        Assert.assertTrue(clickOnElement("confirm_submit_button", 10));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(verifyTextById("account_name", "AutomationAccount", 20));
    }

   @Test(retryAnalyzer = RetryAnalyzer.class)
    public void A_verify_if_user_can_rename_an_account() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 10));
        Assert.assertTrue(clickOnElementByXpath("(//android.widget.ImageView[@content-desc=\"Account Settings\"])[1]", 10));
        Assert.assertTrue(clickOnElement("change_name", 10));
        Assert.assertTrue(SendTextToField("input_edittext", "AutomationTest", 10));
        Assert.assertTrue(clickOnElement("android:id/button1", 10));
        Assert.assertTrue(clickOnElement("toolbar_back_btn", 10));
        Assert.assertTrue(verifyAccountName("AutomationTest", 20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_account_btn_image", 10));
        Assert.assertTrue(clickOnAccount("AutomationTest",10));
    }
}

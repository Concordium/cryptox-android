package cryptox_AndroidTest.AccountCreation;

import config.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.accountManagement.*;
import static pages.appOperations.commands.performScroll;
import static pages.generalMethods.*;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class IdentityCreation {

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void Z_verify_if_a_user_can_create_new_identity() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 10));
        Assert.assertTrue(clickOnElement("identities_layout",10));
        Assert.assertTrue(clickOnElement("toolbar_plus_btn_add_contact_image",10));
        Assert.assertTrue(clickOnElementByXpath("//*[contains(@text,\"Generated IP 0\")]",30));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(WaitForElement("com.android.chrome:id/compositor_view_holder",20));
        Assert.assertTrue(performScrollDown());
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Submit\"]",10));
        Assert.assertTrue(clickOnElement("got_it_button",10));
        Assert.assertTrue(clickOnElement("confirm_button",10));
        Assert.assertTrue(clickOnElement("android:id/button2",10));
        log.info("Successfully created new identity");
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void A_verify_if_user_can_rename_identity() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 10));
        Assert.assertTrue(clickOnElement("identities_layout",10));
        Assert.assertTrue(clickOnMostRecentIdentity());
        Assert.assertTrue(clickOnElement("name_icon",10));
        Assert.assertTrue(SendTextToField("input_edittext", "AutomationTest", 20));
        Assert.assertTrue(clickOnElement("android:id/button1",20));
        Assert.assertTrue(verifyTextById("name_textview","AutomationTest",10));
        Assert.assertTrue(clickOnElement("toolbar_back_btn",10));
        Assert.assertTrue(verifyMostRecentIdentityName("AutomationTest",20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 10));
        Assert.assertTrue(clickOnElement("identities_layout",10));
        Assert.assertTrue(verifyMostRecentIdentityName("AutomationTest",20));
    }
}


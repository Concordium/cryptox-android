package cryptox_AndroidTest.RecoverAccounts;

import config.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.*;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.createPassCodeScreen.createPassCodeNow;

import static pages.generalMethods.*;
import static pages.landingScreen.clickGetStarted;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;


public class RecoverAccountCases {

    public static String newsTabButton = "menuitem_news";
    public static String navigation_bar_item_large_label_view = "bottom_navigation_view";
    public static String continueButton = "continueButton";


    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void import_wallets_through_Private_key() throws MalformedURLException, InterruptedException {
        log.info("Starting import_wallets_through_Private_key test");

        boolean isSuccess;
        isSuccess = clickGetStarted();
        log.info("clickGetStarted: " + isSuccess);
        Assert.assertTrue(isSuccess);
        Assert.assertTrue(clickOnElement("terms_check_box", 10));

        isSuccess = clickOnImportWalletLink();
        log.info("clickOnImportWalletLink: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = createPassCodeNow();
        log.info("createPassCodeNow: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = repeatPassCodeNow();
        log.info("repeatPassCodeNow: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickOnImportViaPrivateKey();
        log.info("clickOnImportViaPrivateKey: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = AddPrivateKey();
        log.info("AddPrivateKey: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickOnElement(continueButton, 20);
        log.info("clickOnElement (continueButton): " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = verifyPinAndPressOK();
        log.info("verifyPinAndPressOK: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickOnElement(continueButton, 20);
        log.info("clickOnElement (continueButton): " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = verifyPinAndPressOK();
        log.info("verifyPinAndPressOK: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = WaitForElement(continueButton, 150);
        log.info("WaitForElement (continueButton): " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickOnElement(continueButton, 20);
        log.info("clickOnElement (continueButton): " + isSuccess);
        Assert.assertTrue(isSuccess);

        log.info("Successfully recovered Account, Checking if UI is interactive");

        isSuccess = clickOnElement(navigation_bar_item_large_label_view, 10);
        log.info("clickOnElement (navigation_bar_item_large_label_view): " + isSuccess);
        Assert.assertTrue(isSuccess);
    }
}

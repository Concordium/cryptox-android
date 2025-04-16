package cryptox_AndroidTest.RecoverAccounts;

import config.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.*;
import static cryptox_AndroidTest.baseClass.*;
import static cryptox_AndroidTest.createAccount.stageNetPackageName;
import static pages.EnableBiometricPopUp.RejectBiometric;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.landingScreen.clickConnectButton;
import static pages.landingScreen.clickGetStarted;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;


public class RecoverAccountCases {

    public static String newsTabButton = "menuitem_news";
    public static String navigation_bar_item_large_label_view = "menuitem_accounts";
    public static String continueButton = "continueButton";


    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void import_wallets_through_Private_key() throws MalformedURLException, InterruptedException {
        // Added logging for debugging purposes
        log.info("Starting import_wallets_through_Private_key test");

        // Simulate failure for retry logic (optional for testing)
        boolean isSuccess;

        isSuccess = AcceptNotificationPopUp();
        log.info("AcceptNotificationPopUp: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickConnectButton();
        log.info("clickConnectButton: " + isSuccess);
        Assert.assertTrue(isSuccess);

        isSuccess = clickGetStarted();
        log.info("clickGetStarted: " + isSuccess);
        Assert.assertTrue(isSuccess);

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

        isSuccess = clickOnElement(newsTabButton, 10);
        log.info("clickOnElement (newsTabButton): " + isSuccess);
        Assert.assertTrue(isSuccess);

        log.info("Successfully recovered Account, Checking if UI is interactive");

        isSuccess = clickOnElement(navigation_bar_item_large_label_view, 10);
        log.info("clickOnElement (navigation_bar_item_large_label_view): " + isSuccess);
        Assert.assertTrue(isSuccess);
    }
}

package cryptox_AndroidTest.RecoverAccounts;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static config.appiumconnection.log;
import static config.systemInfo.getSeedPhrase;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.*;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.generalMethods.*;
import static pages.landingScreen.clickGetStarted;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;

public class recoverAccountThroughSeedPhrase {
    @Test
    public void import_wallets_through_Seed_Phrase() throws MalformedURLException, InterruptedException {
        Assert.assertTrue(clickGetStarted());
        Assert.assertTrue(clickOnElement("terms_check_box",10));
        Assert.assertTrue(clickOnElement("import_wallet_button",10));
        Assert.assertTrue(createPassCodeNow());
        Assert.assertTrue(repeatPassCodeNow());
        Assert.assertTrue(clickOnImportViaSeedPhrase());
        Assert.assertTrue(SendTextToField("etTitle",getSeedPhrase(), 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement(continueButton,20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(WaitForElement(continueButton,100));
        Assert.assertTrue(clickOnElement(continueButton,20));
        log.info("successfully recovered Account");
    }
}

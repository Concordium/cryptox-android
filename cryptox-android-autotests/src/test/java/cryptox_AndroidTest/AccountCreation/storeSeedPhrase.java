package cryptox_AndroidTest.AccountCreation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.*;
import static pages.accountSetup.accountScreen.onboardingStatusTitleVerification;
import static pages.accountSetup.seedPhraseScreen.*;
import static pages.login.loginCryptoX;
import static pages.verifyIdentity.*;
import static pages.verifyPIN.verifyPinAndContinue;
import static pages.verifyPIN.verifyPinAndPressOK;


public class storeSeedPhrase {


 @Test
    public void verify_if_user_is_able_to_save_the_seed_phrase() throws MalformedURLException {
     driver.terminateApp(PackageName);
     driver.activateApp(PackageName);
     Assert.assertTrue(loginCryptoX());
     Assert.assertTrue(verifyProgressBarUpdate("33.0"));
     Assert.assertTrue(saveSeedPhrase());
     Assert.assertTrue(seedPhraseBackup());
     Assert.assertTrue(pressContinueButton());
     Assert.assertTrue(VerifyPINAndPressOKButton());
     Assert.assertTrue(verifyProgressBarUpdate("66.0"));
    }

    @Test
    public void Z_verify_if_user_is_able_to_verifyIdentity() throws MalformedURLException {
        Assert.assertTrue(verifyID());
        Assert.assertTrue(verifyPinAndContinue());
        Assert.assertTrue(onboardingStatusTitleVerification("Verification completed"));
        Assert.assertTrue(verifyIdentityCreateAccountButton());
        Assert.assertTrue(verifyPinAndPressOK());


    }
}





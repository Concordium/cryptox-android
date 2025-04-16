package cryptox_AndroidTest.AccountCreation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import static config.appiumconnection.driver;
import static config.appiumconnection.openAppiumSession;
import static cryptox_AndroidTest.baseClass.*;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.landingScreen.clickConnectButton;
import static pages.landingScreen.clickGetStarted;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.walletAccountCreationScreen.activateAccount;

public class AccountCreation {
@Test
    public void createConcordionAccount() throws MalformedURLException {
         Assert.assertTrue(AcceptNotificationPopUp());
         Assert.assertTrue(clickConnectButton());
         Assert.assertTrue(clickGetStarted());
         Assert.assertTrue(activateAccount());
         Assert.assertTrue(createPassCodeNow());
         Assert.assertTrue(repeatPassCodeNow());


    }
}

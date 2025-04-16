package cryptox_AndroidTest;


import org.testng.Assert;
import org.testng.annotations.*;

import java.net.MalformedURLException;

import static config.StartAppiumServer.*;
import static config.appiumconnection.driver;
import static config.appiumconnection.openAppiumSession;
import static pages.EnableBiometricPopUp.RejectBiometric;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.landingScreen.clickConnectButton;
import static pages.landingScreen.clickGetStarted;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.walletAccountCreationScreen.activateAccount;

public class createAccount {

public static String deviceOne = "18131FDF6000EZ";
public static String deviceTwo = "ZT322PQS55";

public static String testnetPackageName = "com.pioneeringtechventures.wallet.testnet";
public static String stageNetPackageName = "com.pioneeringtechventures.wallet.stagenet";



public static String activityName = "com.concordium.wallet.ui.MainActivity";
public static String loginActivity = "com.concordium.wallet.ui.auth.login.AuthLoginActivity";


@BeforeTest
    public void setup() throws MalformedURLException {
    openAppiumSession(deviceOne, stageNetPackageName, activityName);
//    openAppiumSession(deviceTwo,packageName,activityName,"4723");
      createConcordionAccount();


    }

    @Ignore
    public void B_notification() throws MalformedURLException {
        Assert.assertTrue(AcceptNotificationPopUp());

    }

    public void createConcordionAccount() throws MalformedURLException {
    Assert.assertTrue(clickConnectButton());
    Assert.assertTrue(clickGetStarted());
    Assert.assertTrue(activateAccount());
    Assert.assertTrue(createPassCodeNow());
    Assert.assertTrue(repeatPassCodeNow());
    Assert.assertTrue(RejectBiometric());
}

@AfterTest
    public void tearDown(){
    driver.quit();
}
}


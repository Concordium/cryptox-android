package cryptox_AndroidTest;


import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.createAccount.*;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.requestCCDs.clickOnRequestCCDs;
import static pages.popUps.AcceptNotificationPopUp;

public class RecoverAccountCases {

    @Ignore
    public void RequestCCDSforTesting() throws MalformedURLException {

//        driver.terminateApp(packageName);
//        driver.activateApp(packageName);
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(clickOnRequestCCDs());

    }
}

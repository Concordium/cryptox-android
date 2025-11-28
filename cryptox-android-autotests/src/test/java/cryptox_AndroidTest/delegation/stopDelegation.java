package cryptox_AndroidTest.delegation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import static pages.appOperations.commands.swipe;
import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.validation.createValidation.clickOnEarnRewards;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnElement;
import static pages.login.loginCryptoX;
import static pages.popUps.clickOnAndroidDefaultPopUP;
import static pages.verifyPIN.verifyPinAndPressOK;

public class stopDelegation {

    @Test
    public void Verify_if_user_can_stop_a_passive_delegator() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("stop_button",20));
        Assert.assertTrue(clickOnElement("continue_button",20));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("submit_delegation_finish",20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(elementShouldNotAvailable("accounts_overview_total_details_delegating",5));
        Assert.assertTrue(elementShouldNotAvailable("delegating_label",5));
    }
}

package cryptox_AndroidTest.delegation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.validation.createValidation.clickOnEarnRewards;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnElement;
import static pages.login.loginCryptoX;
import static pages.operations.verifyText;
import static pages.popUps.clickOnAndroidDefaultPopUP;
import static pages.verifyPIN.verifyPinAndPressOK;
import static pages.appOperations.commands.swipe;

public class createDelegation {


    @Test
    public void  Verify_if_user_can_create_a_passive_delegator() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("btn_start_earning",20));
        Assert.assertTrue(clickOnElement("delegation_type_layout",20));
        Assert.assertTrue(clickOnElement("pool_registration_continue",20));
        Assert.assertTrue(SendTextToField("amount","100", 20));
        Assert.assertTrue(clickOnElement("pool_registration_continue",20));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("submit_delegation_finish",20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("content",20));
        Thread.sleep(10000);
        Assert.assertTrue(verifyText("accounts_overview_total_details_delegating",20, "100 CCD"));
        Assert.assertTrue(verifyText("delegating_label",20, "Earning:"));

    }





}

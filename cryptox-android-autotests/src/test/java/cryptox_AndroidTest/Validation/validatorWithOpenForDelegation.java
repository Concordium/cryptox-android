package cryptox_AndroidTest.Validation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.validation.createValidation.*;
import static pages.Transactions.validation.stopValidation.clickOnStopButton;
import static pages.Transactions.validation.stopValidation.clickOnValidationStatusButton;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.performScroll;
import static pages.appOperations.commands.swipe;
import static pages.generalMethods.*;
import static pages.login.loginCryptoX;
import static pages.popUps.clickOnAndroidDefaultPopUP;
import static pages.verifyPIN.verifyPinAndPressOK;

public class validatorWithOpenForDelegation {

    public static String baker_id = "accounts_overview_total_details_staked";


    @Test
    public void Z_verify_if_user_is_able_to_create_validator_with_deligation() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("btnBaker",20));
        Assert.assertTrue(performScroll());
        Thread.sleep(20000);
        Assert.assertTrue(clickOnValidationSetupButton());
        Assert.assertTrue(enterAmountForPool());
        Assert.assertTrue(clickOnContinueButtonOnRegisterValidatorScreen());
        Assert.assertTrue(clickOnContinueButtonOnDelegationSScreen());
        Assert.assertTrue(clickOnElement("baker_registration_continue",20));
        Assert.assertTrue(clickOnElement("baker_registration_open_continue",20));
        Assert.assertTrue(clickOnElement("ok_button",20));
        Assert.assertTrue(clickOnContinueButtonOnCValidatorCommissionScreen());
        Assert.assertTrue(saveValidatorKeys());
        Assert.assertTrue(clickOnAndroidDefaultPopUP());
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(ClickOnValidatorFinishButton());
        Assert.assertTrue(clickOnElement("ok_button",20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("token_details_button",20));
        Assert.assertTrue(verifyElementById("accounts_overview_total_details_staked",20));
    }

    @Test
    public void A_verify_if_user_is_able_to_stop_the_validation_with_delegetion() throws MalformedURLException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("stop_button",20));
        Assert.assertTrue(clickOnElement("ok_button",20));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("token_details_button",20));
        Assert.assertTrue(elementShouldNotAvailable(baker_id,2));
    }
}
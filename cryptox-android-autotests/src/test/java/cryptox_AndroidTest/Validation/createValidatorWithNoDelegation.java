package cryptox_AndroidTest.Validation;

import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.offset.PointOption;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;
import static cryptox_AndroidTest.Validation.validatorWithOpenForDelegation.baker_id;
import static cryptox_AndroidTest.baseClass.*;
import static org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.setup;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.validation.createValidation.*;
import static pages.Transactions.validation.stopValidation.clickOnStopButton;
import static pages.Transactions.validation.stopValidation.clickOnValidationStatusButton;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.performScroll;
import static pages.appOperations.commands.swipe;
import static pages.login.loginCryptoX;
import static pages.popUps.clickOnAndroidDefaultPopUP;
import static pages.verifyPIN.verifyPinAndPressOK;

public class createValidatorWithNoDelegation {

    @Test
    public void Z_verify_if_user_is_able_to_create_validator_with_no_deligation() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("allow_button",5));
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("btnBaker",20));
        Assert.assertTrue(performScroll());
        Assert.assertTrue(clickOnElement("continue_button",10));
        Assert.assertTrue(enterAmountForPool());
        Assert.assertTrue(clickOnContinueButtonOnRegisterValidatorScreen());
        String environment = "stagenet";
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet." + environment + ":id/button\" and @text=\"Closed for new\"]",20));
        Assert.assertTrue(clickOnContinueButtonOnDelegationSScreen());
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
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(verifyValidatorID());
    }

    @Test
    public void A_verify_if_user_is_able_to_stop_the_no_delegetion_validation() throws MalformedURLException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnElement("stop_button",20));
        Assert.assertTrue(clickOnElement("continue_button",20));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(elementShouldNotAvailable(baker_id,2));
    }
}
package cryptox_AndroidTest.Validation;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.createAccount.*;
import static org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.setup;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.requestCCDs.clickOnRequestCCDs;
import static pages.Transactions.validation.createValidation.*;
import static pages.login.loginCryptoX;

public class createValidator {

    @Ignore
    public void create_validator_with_no_deligation() throws MalformedURLException {
        driver.terminateApp(stageNetPackageName);
        driver.activateApp(stageNetPackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(clickOnEarnRewards());
        Assert.assertTrue(clickOnValidationSetupButton());
        Assert.assertTrue(VerifyValidatorPage());
        Assert.assertTrue(clickOnNextButton());
        Assert.assertTrue(clickOnContinueButton());
        Assert.assertTrue(enterAmountForPool());
        Assert.assertTrue(clickOnContinueButtonOnRegisterValidatorScreen());
        Assert.assertTrue(clickOnContinueButtonOnDelegationSScreen());
        Assert.assertTrue(clickOnContinueButtonOnCValidatorCommissionScreen());








    }


}

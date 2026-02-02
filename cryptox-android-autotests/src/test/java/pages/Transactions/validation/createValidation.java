package pages.Transactions.validation;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.testng.Assert;

import static config.appiumconnection.*;
import static config.systemInfo.getCurrentActivity;

public class createValidation {

    static By acceptPrivacyPolicy = By.id("consent_check_box");
    static By earnRewards = By.id("menuitem_earn");
    public static By validatorSetup = By.id("continue_button");
    static By becomeAValidatorLabel = By.id(("toolbar_title"));
    static By clickOnValidatorNextButton = By.id(("create_ident_intro_next"));
    static By createIdentIntroNext = By.id("create_ident_intro_next");
    static By clickOnContinueButtonID = By.id("create_ident_intro_continue");
    static By enterAmountId = By.id("amount");
    static By clickOnContinueButtonOnRegisterValidatorScreenID = By.id("baker_registration_continue");
    static By clickOnContinueButtonOnDelegationScreenID = By.id("baker_registration_continue");
    static By clickOnContinueButtonOnValidatorCommissionScreenID = By.id("baker_registration_export");
    static By baker_registration_open_continueID = By.id("baker_registration_open_continue");
    static By exportKeyConsent = By.id("android:id/button1");
    static By validator_registration_export = By.id("baker_registration_export");
    static By submit_baker_transactionID = By.id("submit_baker_transaction");
    static By submit_baker_finish = By.id("submit_delegation_finish");
    static By validatorID = By.id("accounts_overview_total_details_delegating");


    public static boolean login() {
        try {
            String ActivityName = driver.currentActivity();
            log.info("This is the current Activity {}", ActivityName);
            MobileElement accept_privacyPolicy = waitForElement(acceptPrivacyPolicy, 30);
            assert accept_privacyPolicy != null;
            if (accept_privacyPolicy.isDisplayed()) {
                accept_privacyPolicy.click();
                Thread.sleep(5000);
                return true;

            } else {

                System.out.println("Unable to find element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }

    public static boolean clickOnEarnRewards() {
        try {
            String ActivityName = driver.currentActivity();
            log.info("This is the current Activity {}", ActivityName);
            MobileElement earn_rewards = waitForElement(earnRewards, 30);
            assert earn_rewards != null;
            if (earn_rewards.isDisplayed()) {
                earn_rewards.click();
                Thread.sleep(5000);
                return true;

            } else {

                System.out.println("Unable to find element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }

    public static boolean clickOnValidationSetupButton() {
        try {

            MobileElement validator_Setup = waitForElement(validatorSetup, 30);
            assert validator_Setup != null;
            if (validator_Setup.isDisplayed()) {
                validator_Setup.click();
                Thread.sleep(5000);
                log.info("successfully clicked on " + validator_Setup);
                return true;

            } else {

                System.out.println("Unable to find element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }

    public static boolean VerifyValidatorPage() {
        try {

            MobileElement become_AValidatorLabel = waitForElement(becomeAValidatorLabel, 30);
            System.out.println(become_AValidatorLabel + "Label Found");
            assert become_AValidatorLabel != null;
            String validatorHeadingText = become_AValidatorLabel.getText();
            System.out.println(validatorHeadingText + " is different than Learn about validation");

            if (validatorHeadingText.equals("Learn about validation")) {

                return true;
            } else {

                System.out.println("heading of validator page is not correct");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }

    public static boolean clickOnNextButton() {
        try {

            MobileElement click_OnValidatorNextButton = waitForElement(clickOnValidatorNextButton, 30);

            assert click_OnValidatorNextButton != null;
            if (click_OnValidatorNextButton.isDisplayed()) {
                click_OnValidatorNextButton.click();
                Thread.sleep(2000);
                MobileElement create_ident_intro_next = waitForElement(createIdentIntroNext, 30);
                assert create_ident_intro_next != null;
                create_ident_intro_next.click();
//                Thread.sleep(2000);
//                MobileElement create_Ident_Intro_Continue = waitForElement(createIdentIntroContinue, 30);
//                assert create_Ident_Intro_Continue != null;
//                create_Ident_Intro_Continue.click();


                return true;

            } else {

                System.out.println("Unable to find click_OnValidatorNextButton element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }

    public static boolean clickOnContinueButton() {

        {
            try {

                MobileElement click_OnContinueButton = waitForElement(clickOnContinueButtonID, 30);

                assert click_OnContinueButton != null;
                if (click_OnContinueButton.isDisplayed()) {
                    click_OnContinueButton.click();
                    Thread.sleep(2000);
                    return true;

                } else {

                    System.out.println("Unable to find click_OnValidatorNextButton element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }


    public static boolean enterAmountForPool() {

        {
            try {
                getCurrentActivity();
                MobileElement enter_Amount = waitForElement(enterAmountId, 30);

                assert enter_Amount != null;
                if (enter_Amount.isDisplayed()) {
                    enter_Amount.clear();
                    enter_Amount.sendKeys("12000");

                    return true;

                } else {

                    System.out.println("Unable to find enter_Amount element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean clickOnContinueButtonOnRegisterValidatorScreen() {

        {
            try {

                MobileElement click_OnContinueButtonOnRegisterValidatorScreenID = waitForElement(clickOnContinueButtonOnRegisterValidatorScreenID, 30);

                assert click_OnContinueButtonOnRegisterValidatorScreenID != null;
                if (click_OnContinueButtonOnRegisterValidatorScreenID.isDisplayed()) {
                    click_OnContinueButtonOnRegisterValidatorScreenID.click();


                    return true;

                } else {

                    System.out.println("Unable to find click_OnContinueButtonOnRegisterValidatorScreenID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean clickOnContinueButtonOnDelegationSScreen() {

        {
            try {

                MobileElement click_OnContinueButtonOnDelegationScreenID = waitForElement(clickOnContinueButtonOnDelegationScreenID, 30);

                assert click_OnContinueButtonOnDelegationScreenID != null;
                if (click_OnContinueButtonOnDelegationScreenID.isDisplayed()) {
                    click_OnContinueButtonOnDelegationScreenID.click();


                    return true;

                } else {

                    System.out.println("Unable to find click_OnContinueButtonOnRegisterValidatorScreenID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean clickOnContinueButtonOnCValidatorCommissionScreen() {

        {
            try {

                MobileElement click_OnContinueButtonOnValidatorCommissionScreenID = waitForElement(clickOnContinueButtonOnValidatorCommissionScreenID, 30);

                assert click_OnContinueButtonOnValidatorCommissionScreenID != null;
                if (click_OnContinueButtonOnValidatorCommissionScreenID.isDisplayed()) {
                    click_OnContinueButtonOnValidatorCommissionScreenID.click();


                    return true;

                } else {

                    System.out.println("Unable to find click_OnContinueButtonOnRegisterValidatorScreenID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean clickOnValidatorRegistrationOpenContinue() {

        {
            try {

                MobileElement click_OnContinueButtonOnValidatorCommissionScreenID = waitForElement(baker_registration_open_continueID, 30);

                assert click_OnContinueButtonOnValidatorCommissionScreenID != null;
                if (click_OnContinueButtonOnValidatorCommissionScreenID.isDisplayed()) {
                    click_OnContinueButtonOnValidatorCommissionScreenID.click();


                    return true;

                } else {

                    System.out.println("Unable to find baker_registration_open_continueID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean saveValidatorKeys() {

        {
            try {

                MobileElement exportKey_Consent = waitForElement(exportKeyConsent, 30);

                assert exportKey_Consent != null;
                if (exportKey_Consent.isDisplayed()) {
                    exportKey_Consent.click();


                    return true;

                } else {

                    System.out.println("Unable to find baker_registration_open_continueID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean submitBakerTransaction() {

        {
            try {

                MobileElement exportKey_Consent = waitForElement(submit_baker_transactionID, 30);

                assert exportKey_Consent != null;
                if (exportKey_Consent.isDisplayed()) {
                    exportKey_Consent.click();

                    return true;

                } else {

                    System.out.println("Unable to find baker_registration_open_continueID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean ClickOnValidatorFinishButton() {

        {
            try {


                MobileElement submitBakerFinish = waitForElement(submit_baker_finish, 30);

                assert submitBakerFinish != null;
                if (submitBakerFinish.isDisplayed()) {
                    submitBakerFinish.click();

                    return true;

                } else {

                    System.out.println("Unable to find baker_registration_open_continueID element");
                    return false;
                }

            } catch (Exception exp) {
                log.error(String.valueOf(exp.getCause()));
                System.out.println(exp.getMessage());
                log.error(String.valueOf(exp.fillInStackTrace()));
            }


            return false;
        }


    }

    public static boolean verifyValidatorID() {
        try {
            MobileElement validator_ID = waitForElement(validatorID, 30);

            if (validator_ID != null && validator_ID.isDisplayed()) {
                String validatorText = validator_ID.getText();

                String numericText = validatorText.replaceAll("[^\\d]", "");

                int validatorInt = Integer.parseInt(numericText);

                if (validatorInt > 0) {
                    return true;
                } else {
                    log.warn("Validator ID is not greater than 0: " + validatorInt);
                }
            } else {
                log.warn("Validator element not found or not displayed: " + validatorID);
            }
        } catch (NumberFormatException nfe) {
            log.error("Validator ID is not a valid number: " + nfe.getMessage(), nfe);
        } catch (Exception e) {
            log.error("Exception occurred while verifying Validator ID: " + e.getMessage(), e);
        }

        return false;
    }


}

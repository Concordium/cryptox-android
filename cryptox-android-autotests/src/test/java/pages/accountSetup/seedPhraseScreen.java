package pages.accountSetup;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class seedPhraseScreen {
    static By copySeedPhraseAndSave = By.id("onboarding_action_button");
    static By pressContineButton = By.id("continue_button");
    public static By confirmPassCode = By.id("password_edittext");
    public static By pressOkButton = By.id("second_dialog_button");
    static By seedPhraseCopyCheckBox = By.id("consent_check_box");
    static By progressBar = By.id("onboarding_status_progress_bar");

    public static boolean saveSeedPhrase()

    {
        try
        {


            MobileElement copy_SeedPhraseAndSave = waitForElement(copySeedPhraseAndSave,30);

            assert copy_SeedPhraseAndSave != null;

            if(copy_SeedPhraseAndSave.isDisplayed())
            {
                copy_SeedPhraseAndSave.click();
                Thread.sleep(2000);
                return true;
            }
            else {

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


    public static boolean pressContinueButton()

    {
        try
        {
            MobileElement press_continueButton = waitForElement(pressContineButton,30);



            assert press_continueButton != null;


            if(press_continueButton.isDisplayed())
            {
                press_continueButton.click();

                return true;
            }
            else {

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

    public static boolean VerifyPINAndPressOKButton()

    {
        try
        {
            MobileElement confirm_PassCode = waitForElement(confirmPassCode,30);
            assert confirm_PassCode != null;
            if(confirm_PassCode.isDisplayed())
            {
                confirm_PassCode.sendKeys("111111");
                Thread.sleep(10000);
                System.out.println("unable to find confirm_PassCode");
                MobileElement press_OkButton = waitForElement(pressOkButton,30);
                assert press_OkButton != null;
                press_OkButton.click();
                return true;
            }
            else {

                System.out.println("unable to find confirm_PassCode or press_OkButton");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }
        return false;
    }


    public static boolean seedPhraseBackup()

    {
        try
        {


            MobileElement seed_PhraseCopyCheckBox = waitForElement(seedPhraseCopyCheckBox,30);

            assert seed_PhraseCopyCheckBox != null;

            if(seed_PhraseCopyCheckBox.isDisplayed())
            {
                seed_PhraseCopyCheckBox.click();
                Thread.sleep(2000);
                return true;
            }
            else {

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

    public static boolean verifyProgressBarUpdate(String percentage)

    {
        try
        {
            MobileElement progress_Bar_status = waitForElement(progressBar,30);



            assert progress_Bar_status != null;
            String accountCreationStatus = progress_Bar_status.getText();
            if(accountCreationStatus.equals(percentage))
            {
                System.out.println(accountCreationStatus);
                return true;
            }
            else {

                System.out.println("Unable to find element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }
        return false;
    }}
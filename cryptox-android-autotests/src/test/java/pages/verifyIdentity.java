package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;

public class verifyIdentity {
    static By concordiumTestNetID = By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/header_textview\" and @text=\"Concordium testnet IP\"]");
    static By createAccountVerifyScreen = By.id("onboarding_inner_action_button");
    static By onboardinButton = By.id("onboarding_action_button");


    public static boolean verifyID()

    {
        try
        {Thread.sleep(2000);

            MobileElement onboarding_Button = waitForElement(onboardinButton,30);
            assert onboarding_Button != null;
            if(onboarding_Button.isDisplayed())
            {
                onboarding_Button.click();
                MobileElement accept_activityTracking = waitForElement(concordiumTestNetID,30);
                accept_activityTracking.click();
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

    public static boolean verifyIdentityCreateAccountButton()

    {
        try
        {Thread.sleep(2000);

            MobileElement create_AccountVerifyScreen = waitForElement(createAccountVerifyScreen,30);
            assert create_AccountVerifyScreen != null;
            if(create_AccountVerifyScreen.isDisplayed())
            {
                create_AccountVerifyScreen.click();
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
}

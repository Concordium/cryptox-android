package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class landingScreen {
    public static By acceptPrivacyPolicy = By.id("terms_check_box");
    static By getStartedButton = By.id("get_started_button");

    public static boolean clickConnectButton()

    {
       try
       {

           MobileElement accept_privacyPolicy = waitForElement(acceptPrivacyPolicy,30);
           assert accept_privacyPolicy != null;
           if(accept_privacyPolicy.isDisplayed())
           {
               accept_privacyPolicy.click();
               Thread.sleep(5000);
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


    public static boolean clickGetStarted()

    {
        try
        {
            MobileElement click_getStartedButton = driver.findElement(getStartedButton);
            if(click_getStartedButton.isDisplayed())
            {
                click_getStartedButton.click();
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

package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;

public class EnableBiometricPopUp {
    static By declineBiometricAccess = By.id("deny_button");

    public static boolean RejectBiometric()

    {
        try
        {
            MobileElement decline_BiometricAccess = waitForElement(declineBiometricAccess,30);
            assert decline_BiometricAccess != null;

            if(decline_BiometricAccess.isDisplayed())
            {
                decline_BiometricAccess.click();
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

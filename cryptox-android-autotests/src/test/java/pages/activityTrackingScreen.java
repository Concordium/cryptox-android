package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class activityTrackingScreen {
    static By acceptActivityTracking = By.id("allow_button");

    public static boolean AcceptActivityTracking()

    {
        try
        {Thread.sleep(2000);

            MobileElement accept_activityTracking = waitForElement(acceptActivityTracking,30);
            assert accept_activityTracking != null;
            if(accept_activityTracking.isDisplayed())
            {
                accept_activityTracking.click();
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

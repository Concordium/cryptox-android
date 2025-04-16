package pages.Transactions.validation;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class stopValidation {
    static By statusButtonBottom = By.id("status_button_bottom");
    static By stop_button = By.id("stop_button");


    public static boolean clickOnValidationStatusButton() {
        try {

            MobileElement status_ButtonBottom = waitForElement(statusButtonBottom, 30);
            assert status_ButtonBottom != null;
            if (status_ButtonBottom.isDisplayed()) {
                status_ButtonBottom.click();
                Thread.sleep(5000);
                log.info("Successfully clicked on status Button");
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
    public static boolean clickOnStopButton() {
        try {
            String ActivityName = driver.currentActivity();
            log.info("This is the current Activity {}", ActivityName);

            MobileElement stopButton = waitForElement(stop_button, 30);
            log.info("This is the current Activity {}", ActivityName);
            assert stopButton != null;
            if (stopButton.isDisplayed()) {
                stopButton.click();
                Thread.sleep(5000);
                return true;

            } else {

                log.info("Unable to find element {}", stop_button);
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

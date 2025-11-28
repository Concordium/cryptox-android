package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class landingScreen {
    static By continueButton = By.id("continue_button");

    public static boolean clickConnectButton() {
        try {
            MobileElement continueB = waitForElement(continueButton, 10);
            if (continueB.isDisplayed()) {
                continueB.click();
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

    public static boolean clickGetStarted() {
        try {
            MobileElement continueB = waitForElement(continueButton, 10);
            if (continueB.isDisplayed()) {
                continueB.click();
                continueB.click();
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
}

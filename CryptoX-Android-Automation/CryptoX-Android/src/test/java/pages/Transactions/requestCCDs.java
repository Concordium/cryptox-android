package pages.Transactions;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;
import static config.screenshotRecord.takeScreenShot;

public class requestCCDs {


    static By accountWidget = By.id("content");
    static By requestCCD = By.id("gtu_drop_button");


    public static boolean clickOnAccountWidget() {
        try {
            Thread.sleep(5000);
            MobileElement account_widget = waitForElement(accountWidget, 30);
            assert account_widget != null;
            if (account_widget.isDisplayed()) {
                account_widget.click();
                Thread.sleep(2000);
                return true;
            } else {

                System.out.println("Unable to find element");
                takeScreenShot("requestCCD.png");
            }

        } catch (Exception exp) {
            takeScreenShot("requestCCD.png");
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }


    public static boolean clickOnRequestCCDs() {
        try {

            MobileElement request_CCD = waitForElement(requestCCD, 30);
            assert request_CCD != null;
            if (request_CCD.isDisplayed()) {
                request_CCD.click();
                Thread.sleep(2000);
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
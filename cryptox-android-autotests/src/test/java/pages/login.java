package pages;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.popUps.*;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.testng.Assert;


public class login {

    static By pinOne = By.id("digit1_edittext");


    public static boolean loginCryptoX() {
        try {
            MobileElement pin_Box1 = waitForElement(pinOne, 30);


            assert pin_Box1 != null;
            if (pin_Box1.isDisplayed()) {
                pin_Box1.click();

                // Simulate pressing each digit of the PIN
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"
                driver.pressKey(new KeyEvent(AndroidKey.DIGIT_1)); // Press "1"

                // Optionally, you can simulate pressing Enter or a confirmation key
                // driver.pressKey(new KeyEvent(AndroidKey.ENTER)); // Uncomment if needed

                // Optional: Add a delay if necessary
                Thread.sleep(5000);
//                MobileElement notification_popupWindow = waitForElement(notificationPopUp, 30);
//
//                if (notification_popupWindow != null) {
//                    notification_popupWindow.click();                                }
                log.info("Logged in Successfully");

                return true;
            } else {
                log.error("Unable to find login screen or button are not clickable.");
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

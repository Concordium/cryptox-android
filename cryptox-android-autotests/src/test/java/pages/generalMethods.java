package pages;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static config.appiumconnection.*;

public class generalMethods {

    public static boolean clickOnElement(String elementID, Integer timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {

                elementToLookFor.click();
                log.info("Successfully clicked on Element {}", elementID);
                return true;
            } else {

                log.error("unable to find Element{}", elementID);

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean elementShouldNotAvailable(String elementID, Integer Timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, Timeout);

            assert elementToLookFor == null;
            return true;

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;

    }

    public static boolean verifyElementById(String elementID, Integer timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {

                elementToLookFor.click();
                return true;
            } else {

                System.out.println("unable to verify Element" + elementID);

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean verifyElementByXpath(String elementID, Integer timeout) {
        try {
            By elementIDs = By.xpath(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {

                elementToLookFor.click();
                return true;
            } else {

                System.out.println("unable to verify Element" + elementID);

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean clickOnAccount(String expectedAccount, int timeout) {

        int maxScrolls = 5;

        try {
            log.info("Trying to click account: {}", expectedAccount);

            WebDriverWait wait = new WebDriverWait(driver, timeout);

            for (int i = 0; i < maxScrolls; i++) {
                log.info("Fetching account list (Scroll Down attempt: {})", i + 1);

                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.id("com.pioneeringtechventures.wallet.stagenet:id/account_name")
                ));

                List<MobileElement> accounts = driver.findElements(
                        By.id("com.pioneeringtechventures.wallet.stagenet:id/account_name")
                );

                log.info("Number of accounts found on screen: {}", accounts.size());

                for (MobileElement account : accounts) {
                    String actualName = account.getText();
                    log.info("Found account name: {}", actualName);

                    if (actualName.equalsIgnoreCase(expectedAccount)) {
                        account.click();
                        log.info("Successfully clicked account: {}", actualName);
                        return true;
                    }
                }

                log.warn("Account '{}' not found in current view. Scrolling down...", expectedAccount);

                if (!performScrollDown()) {
                    log.warn("Scroll down not possible further. Stopping downward search.");
                    break;
                }
            }

            // If not found, try scrolling up
            for (int i = 0; i < maxScrolls; i++) {
                log.info("Fetching account list (Scroll Up attempt: {})", i + 1);

                List<MobileElement> accounts = driver.findElements(
                        By.id("com.pioneeringtechventures.wallet.stagenet:id/account_name")
                );

                for (MobileElement account : accounts) {
                    String actualName = account.getText();
                    log.info("Found account name: {}", actualName);

                    if (actualName.equalsIgnoreCase(expectedAccount)) {
                        account.click();
                        log.info("Successfully clicked account: {}", actualName);
                        return true;
                    }
                }

                log.warn("Account '{}' not found in current view. Scrolling up...", expectedAccount);

                if (!performScrollUp()) {
                    log.warn("Scroll up not possible further. Stopping upward search.");
                    break;
                }
            }

            log.error("Account '{}' not found after scrolling down and up {} times", expectedAccount, maxScrolls);
            return false;

        } catch (TimeoutException te) {
            log.error("Timed out waiting for account list to load", te);
            return false;

        } catch (Exception e) {
            log.error("Exception while clicking account '{}'", expectedAccount, e);
            return false;
        }
    }

    public static boolean performScrollUp() {
        try {
            Dimension size = driver.manage().window().getSize();

            int centerX = size.width / 2;
            int startY  = (int) (size.height * 0.42);
            int endY    = (int) (size.height * 0.58); // slight upward scroll

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);

            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), centerX, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(350),
                    PointerInput.Origin.viewport(), centerX, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            driver.perform(Collections.singletonList(swipe));

            log.info("Performed slight scroll up successfully (Appium 3).");
            return true;

        } catch (Exception e) {
            log.error("Failed to perform slight scroll up (Appium 3)", e);
            return false;
        }
    }



    public static boolean performScrollDown() {
        try {
            driver.findElement(MobileBy.AndroidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            log.info("Performed scroll down successfully.");
            return true;
        } catch (Exception e) {
            log.error("Failed to perform scroll down: " + e.getMessage());
            return false;
        }
    }

    public static boolean clickOnToken(String tokenName, int timeoutInSeconds) {
        String xpath = String.format(
                "//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/title' and @text='%s']",
                tokenName
        );
        return clickOnElementByXpath(xpath, timeoutInSeconds);
    }

    public static boolean clickOnElementByXpath(String elementID, Integer timeout) {
        try {
            By elementIDs = By.xpath(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {

                elementToLookFor.click();
                return true;
            } else {

                System.out.println("unable to find Element" + elementID);

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean SendTextToField(String elementID, String Text, Integer timeout)

    {
        try
        {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
                return true;
            }


            else {

                System.out.println("unable to find Element" + elementID );

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean SendTextToFieldByClassName(String elementID, String Text, Integer timeout)

    {
        try
        {
            By elementIDs = By.className(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
                return true;
            }


            else {

                System.out.println("unable to find Element" + elementID );

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean WaitForElement(String elementID, Integer Timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, Timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {


                return true;
            } else {

                log.error("unable to find Element{}", elementID , "While waiting for element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean verifyTextById(String id, String expectedText, int timeoutInSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);

            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(id)));

            List<MobileElement> elements = driver.findElements(By.id(id));

            for (MobileElement el : elements) {
                String actualText = el.getText().trim();
                if (actualText.equals(expectedText)) {
                    log.info("Matched text: " + actualText);
                    return true;
                }
            }

            log.error("Expected text not found: " + expectedText);
            return false;

        } catch (Exception e) {
            log.error("Exception in verifyTextById: " + e.getMessage());
            return false;
        }
    }
}
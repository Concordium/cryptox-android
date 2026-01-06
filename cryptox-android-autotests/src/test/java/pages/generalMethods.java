package pages;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static config.appiumconnection.*;
import static pages.accountManagement.performScrollDown;

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

        int maxScrolls = 6;
        Set<String> visitedAccounts = new HashSet<>();

        try {
            log.info("Trying to click account: {}", expectedAccount);

            WebDriverWait wait = new WebDriverWait(driver, timeout);

            for (int scrollCount = 1; scrollCount <= maxScrolls; scrollCount++) {

                log.info("Fetching visible accounts. Scroll attempt: {}", scrollCount);

                List<WebElement> accounts = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("com.pioneeringtechventures.wallet.stagenet:id/account_name")));

                log.info("Accounts visible on screen: {}", accounts.size());

                for (WebElement account : accounts) {

                    String actualName;
                    try {
                        actualName = account.getText();
                    } catch (StaleElementReferenceException e) {
                        log.warn("Stale element detected. Skipping.");
                        continue;
                    }

                    if (visitedAccounts.contains(actualName)) {
                        continue;
                    }

                    visitedAccounts.add(actualName);
                    log.info("Found account name: {}", actualName);

                    if (actualName.equalsIgnoreCase(expectedAccount)) {

                        wait.until(ExpectedConditions.elementToBeClickable(account)).click();
                        log.info("Successfully clicked account: {}", actualName);
                        return true;
                    }
                }

                log.warn("Account '{}' not found. Scrolling down...", expectedAccount);

                boolean scrolled = performScrollDown();
                Thread.sleep(800);

                if (!scrolled) {
                    log.warn("Reached end of list. No more scrolling possible.");
                    break;
                }
            }

            log.error("Account '{}' not found after {} scrolls", expectedAccount, maxScrolls);
            return false;

        } catch (TimeoutException e) {
            log.error("Timed out waiting for account list", e);
            return false;

        } catch (Exception e) {
            log.error("Unexpected exception while clicking account '{}'", expectedAccount, e);
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

    public static boolean SendTextToField(String elementID, String Text, Integer timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
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

    public static boolean SendTextToFieldByClassName(String elementID, String Text, Integer timeout) {
        try {
            By elementIDs = By.className(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
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

    public static boolean WaitForElement(String elementID, Integer Timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, Timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {


                return true;
            } else {

                log.error("unable to find Element{}", elementID, "While waiting for element");
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
package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    public static boolean clickOnAccount(String accountName, int timeout) {
        String xpath = String.format(
                "//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/account_name' and @text='%s']",
                accountName
        );
        return clickOnElementByXpath(xpath, timeout);
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
package pages;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;
import static pages.generalMethods.performScrollDown;

public class accountManagement {

    public static boolean verifyAccountName(String accountName, int timeout) {
        String xpath = String.format(
                "//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/account_name' and @text='%s']",
                accountName
        );

        try {
            WebDriverWait wait = new WebDriverWait(driver, timeout);

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));

            log.info("Account name '{}' verified successfully.", accountName);
            return true;

        } catch (Exception e) {
            log.error("Failed to verify account name '{}'.", accountName);
            return false;
        }
    }

    public static boolean clickOnMostRecentIdentity() {
        try {
            for (int i = 0; i < 5; i++) {
                performScrollDown();
            }

            By identityLocator = By.xpath(
                    "//android.widget.LinearLayout[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/identity_view']"
            );

            List<MobileElement> identities = driver.findElements(identityLocator);

            if (identities.isEmpty()) {
                log.error("No identities found in the Identity list.");
                return false;
            }

            MobileElement lastIdentity = identities.get(identities.size() - 1);

            lastIdentity.click();
            log.info("Clicked on the most recent identity successfully.");
            return true;

        } catch (Exception e) {
            log.error("Failed to click on the most recent identity.", e);
            return false;
        }
    }


    public static boolean verifyMostRecentIdentityName(String expectedName, int timeoutInSeconds) {
        try {
            for (int i = 0; i < 5; i++) {
                performScrollDown();
            }

            By identityLocator = By.xpath(
                    "//android.widget.LinearLayout[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/identity_view']"
            );

            List<MobileElement> identities = driver.findElements(identityLocator);

            if (identities.isEmpty()) {
                log.error("No identities found on screen.");
                return false;
            }

            MobileElement lastIdentity = identities.get(identities.size() - 1);

            MobileElement nameTextView = lastIdentity.findElement(
                    By.id("com.pioneeringtechventures.wallet.stagenet:id/name_textview")
            );

            String actualName = nameTextView.getText();

            if (expectedName.equals(actualName)) {
                log.info("Last identity name verification PASSED: " + actualName);
                return true;
            } else {
                log.error("Last identity name verification FAILED. Actual: " + actualName +
                        ", Expected: " + expectedName);
                return false;
            }

        } catch (Exception e) {
            log.error("Error verifying the last identity name.", e);
            return false;
        }
    }

}
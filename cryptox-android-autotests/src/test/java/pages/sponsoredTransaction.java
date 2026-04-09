package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static config.appiumconnection.driver;

public class sponsoredTransaction {

    private static final Logger log = LoggerFactory.getLogger(sponsoredTransaction.class);
    private static final int TIMEOUT = 20;


    public static boolean clickElement(By locator, int timeoutInSec) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeoutInSec);
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            element.click();
            return true;
        } catch (Exception e) {
            log.error("Click failed for " + locator + " : " + e.getMessage());
            return false;
        }
    }

    public static void switchToWebView() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            Set<String> contexts = driver.getContextHandles();
            for (String context : contexts) {
                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    log.info("Switched to WebView: " + context);
                    return;
                }
            }
            Thread.sleep(3000);
        }
    }

    public static void switchToNative() {
        driver.context("NATIVE_APP");
    }

    public static boolean verifyTextOnApp(By locator, String expectedText, int timeout) {
        try {
            switchToNative();
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            String actualText = element.getText().trim();
            log.info("Expected Text: " + expectedText + " | Actual Text: " + actualText);
            return actualText.equals(expectedText);
        } catch (Exception e) {
            log.error("Verification failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean verifyAccountsOnDashboard(String senderFromApp, String recipientFromApp) {
        try {
            String events = driver.findElement(By.xpath("//span[contains(text(),'from')]")).getText();
            log.info("Web Event Text: " + events);

            Pattern pattern = Pattern.compile("from account\\s+(\\S+)\\s+to account\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(events.replace("\n", " "));

            if (matcher.find()) {
                String senderFragment = matcher.group(1);
                String recipientFragment = matcher.group(2);
                boolean senderMatches = senderFromApp.startsWith(senderFragment);
                boolean recipientMatches = recipientFromApp.startsWith(recipientFragment);

                if (senderMatches && recipientMatches) {
                    log.info("Accounts match correctly!");
                    return true;
                } else {
                    log.warn("Accounts do NOT match!");
                    return false;
                }
            } else {
                log.warn("Could not extract accounts from web text!");
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying accounts: " + e.getMessage());
            return false;
        }
    }
}
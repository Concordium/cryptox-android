package config;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static config.appiumconnection.*;
import static cryptox_AndroidTest.baseClass.PackageName;
import static org.openqa.selenium.By.id;
import static pages.generalMethods.*;

public class systemInfo {

    public static LocalDate getCurrentDate() {
        // Get the current date using LocalDate
        LocalDate currentDate = LocalDate.now();

        // Print the current date
        System.out.println("Current Date: " + currentDate);

        return currentDate;
    }

    public static boolean getDeviceDimensions(){

        Dimension screenSize = driver.manage().window().getSize();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();

        System.out.println("Screen Width: " + width);
        System.out.println("Screen Height: " + height);
        return true;
    }


    public static String getSeedPhrase(){

        List<String> seedPhrase = Arrays.asList(
//                "gravity dirt slow explain goat wage sample stand spirit chimney athlete van dinosaur rely fragile wagon miracle fun firm ensure find stereo try delay",
//                "luxury crouch desk purity medal replace kitten trial work tuition disease poverty icon banana banner tonight salad muscle special eye bleak involve unknown knife",
                "word thumb room input arm enjoy audit dumb echo deputy cherry violin elder promote vivid around below believe awesome word bubble inhale discover benefit");
        // Create a Random object
        Random random = new Random();

        // Randomly select an index
        int randomIndex = random.nextInt(seedPhrase.size());

        // Fetch the private key at the selected index
        return seedPhrase.get(randomIndex);
    }

    public static String getCurrentActivity(){

        String currentActivity = driver.currentActivity();
        log.info("Hello Hello Hello, This is current Activity{}", currentActivity);
        return currentActivity;
    }
    public static void getCustomNetwork() throws InterruptedException {
        driver.activateApp(PackageName);
        if (isElementPresent("toolbar_networks_btn", 20)) {
            clickOnElement("toolbar_networks_btn", 20);
            turnOnToggle("dev_mode_switch", 20);
            clickOnElement("add_button", 20);
            SendTextToFieldByXpath(
                    "(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[1]",
                    "Stagenet", 20);
            SendTextToFieldByXpath(
                    "(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[2]",
                    "https://wallet-proxy.stagenet.concordium.com/", 20);
            SendTextToFieldByXpath(
                    "(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[4]",
                    "https://stagenet.ccdscan.io/", 20);

            System.out.println("Successfully entered custom network details.");
            try {
                if (driver.isKeyboardShown()) {
                    System.out.println("Keyboard is visible, dismissing via AndroidKey.BACK.");
                    ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
                    System.out.println("Keyboard dismissed successfully.");
                } else {
                    System.out.println("Keyboard was not visible, skipping dismiss.");
                }
            } catch (Exception e) {
                System.out.println("Could not dismiss keyboard: " + e.getMessage());
            }
            if (isElementPresent("cancel_button", 20)) {
                clickOnElement("cancel_button", 20);
                System.out.println("Successfully click on keep editing button");
            }
            else {
                System.out.println("unable to click on keep editing button");
            }
            WebDriverWait wait = new WebDriverWait(driver, 20);
            MobileElement saveBtn = (MobileElement) wait.until(
                    ExpectedConditions.elementToBeClickable(id("save_button")));

            int maxAttempts = 10;
            int attempt = 0;
            while (!driver.findElements(id("save_button")).isEmpty() && attempt < maxAttempts) {
                saveBtn.click();
                saveBtn.click();
                System.out.println("Clicked save button, attempt " + (attempt + 1) + ", waiting for it to disappear...");
                Thread.sleep(1000);
                attempt++;
            }

            Assert.assertTrue(driver.findElements(id("save_button")).isEmpty(), "Save button did not disappear after " + maxAttempts + " attempts");
            System.out.println("Save button is not visible");
            boolean isSwitched = false;
            for (int i = 0; i < 3; i++) {
                if (verifyElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/name_text_view\" and @text=\"Stagenet\"]", 5)) {
                    isSwitched = true;
                    System.out.println("Successfully switched to the custom network setup.");
                    break;
                } else {
                    System.out.println("Retry " + (i + 1) + ": Not switched yet...");
                    Thread.sleep(2000); // important wait before retry
                }
            }
            Assert.assertTrue(isSwitched, "Failed to switch to Stagenet after retries");
        }
    }

    public static boolean turnOnToggle(String elementId, int timeoutInSeconds) {
        try {
            MobileElement toggle = (MobileElement) waitForElement(id(elementId), timeoutInSeconds);
            if (toggle.getAttribute("checked").equals("false")) {
                toggle.click();
            }
            log.info("Successfully Toggled");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to perform Toggled");
            return false;
        }
    }

    public static boolean isElementPresent(String locatorKey, int timeout) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(id(locatorKey)));
            log.info("Network button {} is visible. Proceeding with the flow of setting a customized network.", locatorKey);
            return true;
        } catch (Exception e) {
            log.error("Network button {} is not visible. Proceeding with the normal flow without setting a customized network.", locatorKey);
            return false;
        }
    }

}

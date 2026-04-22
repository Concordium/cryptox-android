package cryptox_AndroidTest;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.time.Duration;

import static config.appiumconnection.*;
import static config.systemInfo.getDeviceDimensions;
import static pages.generalMethods.*;

import config.configLoader;

public class baseClass {
    config.configLoader configLoader = new configLoader();
    public boolean localExecution = Boolean.parseBoolean(configLoader.getProperty("isLocalExecution"));
    public static String PhysicalDevice = "ZT322PQS55";
    public static String EmulatorLocal = "emulator-5554"; // Change to your device ID
    public static String EmulatorCloud = "emulator-5554"; // Change to your device ID
    public static String appURL;
    public static String Device;
    public static String testnetPackageName = "com.pioneeringtechventures.wallet.testnet";
    public static String packageName = "com.pioneeringtechventures.wallet.testnet";
    public static String activityName = "com.concordium.wallet.ui.MainActivity";
    public static String PackageName = packageName;
    boolean isSwitched = false;


    @BeforeTest
    public void setup() throws MalformedURLException, InterruptedException {

        log.info("This i value From File" + localExecution);
        if (localExecution) {

            appURL = "C:\\Automation\\CryptoX-Android-Automation\\CryptoX-Android/stagenet.apk";
            Device = EmulatorLocal;

        } else if (!localExecution) {
            appURL = "/home/runner/work/CryptoX-Android-Automation/CryptoX-Android-Automation/CryptoX-Android/app.apk";
            Device = EmulatorCloud;
        }

        openAppiumSession(Device, PackageName, activityName);
        getDeviceDimensions();
        getCustomNetwork();
    }

    @AfterTest
    public void tearDown() {
        driver.removeApp(PackageName); // This will remove the app from the device
        if (!localExecution) {
            driver.removeApp(PackageName); // This will remove the app from the device
        } else {
            driver.quit(); // This will close the appium driver.
        }
    }

    public void getCustomNetwork() throws InterruptedException {
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
                    driver.hideKeyboard();
                    System.out.println("Keyboard was visible, hidden successfully.");
                } else {
                    System.out.println("Keyboard was not visible, skipping hide.");
                }
            } catch (Exception e) {
                System.out.println("Could not check/hide keyboard: " + e.getMessage());
            }

            WebDriverWait wait = new WebDriverWait(driver, 20);
            MobileElement saveBtn = (MobileElement) wait.until(
                    ExpectedConditions.elementToBeClickable(MobileBy.id("save_button"))
            );
            saveBtn.click();
            saveBtn.click();
            System.out.println("Successfully clicked on save network button.");
            wait.until(ExpectedConditions.invisibilityOfElementLocated(MobileBy.id("save_button")));
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
            Assert.assertTrue(isSwitched, "Failed to switch to Stagenet after retries");        }
    }

    public boolean turnOnToggle(String elementId, int timeoutInSeconds) {
        try {
            MobileElement toggle = (MobileElement) waitForElement(By.id(elementId), timeoutInSeconds);
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

    public boolean isElementPresent(String locatorKey, int timeout) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(locatorKey)));
            log.info("Network button {} is visible. Proceeding with the flow of setting a customized network.", locatorKey);
            return true;
        } catch (Exception e) {
            log.error("Network button {} is not visible. Proceeding with the normal flow without setting a customized network.", locatorKey);
            return false;
        }
    }

}


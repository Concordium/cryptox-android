package cryptox_AndroidTest;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import java.net.MalformedURLException;
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



    @BeforeTest
    public void setup() throws MalformedURLException, InterruptedException {
        log.info("isLocalExecution: " + localExecution);

        if (localExecution) {
            appURL = getEnvOrDefault("APP_URL", "C:\\Automation\\CryptoX-Android-Automation\\CryptoX-Android\\stagenet.apk");
            Device = getEnvOrDefault("DEVICE_UDID", EmulatorLocal);
        } else {
            appURL = getEnvOrDefault("APP_URL", "/home/runner/work/CryptoX-Android-Automation/CryptoX-Android-Automation/CryptoX-Android/app.apk");
            Device = getEnvOrDefault("DEVICE_UDID", EmulatorCloud);
        }

        boolean connected = openAppiumSession(Device, PackageName, activityName);
        if (!connected || driver == null) {
            throw new RuntimeException("Appium driver failed to initialize. Check Appium server is running on port 4723.");
        }
        getDeviceDimensions();
        getCustomNetwork();
    }

    // ✅ Helper — reads from env or returns default
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    @AfterTest
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    public void getCustomNetwork() throws InterruptedException {
        driver.activateApp(PackageName);
        if (isElementPresent("toolbar_networks_btn", 5)) {
            clickOnElement("toolbar_networks_btn", 20);
            turnOnToggle("dev_mode_switch", 20);
            clickOnElement("add_button", 20);
            SendTextToFieldByXpath("(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[1]", "Stagenet", 20);
            SendTextToFieldByXpath("(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[2]", "https://wallet-proxy.stagenet.concordium.com/", 20);
            SendTextToFieldByXpath("(//android.widget.EditText[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/edittext\"])[4]", "https://stagenet.ccdscan.io/", 20);
            WebDriverWait wait = new WebDriverWait(driver, 20);
            MobileElement saveBtn = (MobileElement) wait.until(ExpectedConditions.elementToBeClickable(By.id("save_button")));
            saveBtn.click();
            saveBtn.click();
            Thread.sleep(3000);
            Assert.assertTrue(verifyElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/name_text_view\" and @text=\"Stagenet\"]", 20));
        } else {
            System.out.println("toolbar_networks_btn not visible — skipping custom network setup.");
        }
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


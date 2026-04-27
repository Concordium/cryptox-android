package config;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import org.openqa.selenium.Capabilities;


import static config.StartAppiumServer.startAppium;
import static cryptox_AndroidTest.baseClass.Device;
import static cryptox_AndroidTest.baseClass.appURL;

public class appiumconnection {
    public static AndroidDriver<MobileElement> driver;
    public static final Logger log = LoggerFactory.getLogger(appiumconnection.class);


    public static boolean openAppiumSession(String deviceName, String packageName, String activityName) throws MalformedURLException {
        try {
            DesiredCapabilities cap = getDesiredCapabilities(deviceName, packageName, activityName);
            URL url = new URL("http://127.0.0.1:4723/");
            System.out.println("This is the APPURL " + appURL);
            System.out.println("This is the device " + Device);
            driver = new AndroidDriver<>(url, cap);
            System.out.println("Application Started");
            return true;
        } catch (Exception exp) {
            log.error("Error creating connection to appium session: {}", exp.getMessage());
            log.error("Stack Trace: ", exp);
        }
        return false;
    }

    private static DesiredCapabilities getDesiredCapabilities(String deviceName, String packageName, String activityName) {
        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setCapability("deviceName", "emulator-5554");
        cap.setCapability("udid", deviceName);
        cap.setCapability("platformName", "Android");
        cap.setCapability("platformVersion", "10");
        cap.setCapability("appPackage", packageName);
        cap.setCapability("noReset", true);
        cap.setCapability("appActivity", activityName);
        cap.setCapability("automationName", "UiAutomator2");
        cap.setCapability("autoGrantPermissions", true);
        cap.setCapability("autoAcceptAlerts", true);
        cap.setCapability("autoDismissAlerts", true);
        cap.setCapability("fullReset", false);
        grantNotificationPermission("emulator-5554", packageName);
        cap.setCapability("unicodeKeyboard", true);
        return cap;
    }

    public static void  grantNotificationPermission(String emulatorId, String appPackage) {
        try {
            String command = "adb -s " + emulatorId + " shell pm grant " + appPackage + " android.permission.POST_NOTIFICATIONS";
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            System.out.println("Notification permission granted via ADB.");
        } catch (Exception e) {
            System.out.println("Failed to grant notification permission: " + e.getMessage());
        }
    }


    // Method to wait for an element to be visible
    public static MobileElement waitForElement(By locator, int timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        try {
            return (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            log.error("Element not found: {}. Exception: {}", locator, e.getMessage());
            return null;
        }
    }
}

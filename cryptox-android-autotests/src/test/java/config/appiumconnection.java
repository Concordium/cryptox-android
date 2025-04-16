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


    public static boolean openAppiumBSSession(String deviceName, String packageName, String activityName, String portNumber) throws MalformedURLException {
        try {
            startAppium(portNumber);
            MutableCapabilities cap = new MutableCapabilities();
            URL url = new URL("https://hub-cloud.browserstack.com/wd/hub");
            driver = new AndroidDriver<>(url, cap);
            log.info("Application Started");
            return true;
        } catch (Exception exp) {
            log.error("Error starting Appium session: {}", exp.getMessage());
            log.error("Stack Trace: ", exp);
        }
        return false;
    }

    public static boolean openAppiumSession(String deviceName, String packageName, String activityName) throws MalformedURLException {
        try {
            DesiredCapabilities cap = getDesiredCapabilities(deviceName, packageName, activityName);
            URL url = new URL("http://127.0.0.1:4723/");
            System.out.println("This is the URL " + url);
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
        cap.setCapability("appPackage", packageName);
        cap.setCapability("appActivity", activityName);
        cap.setCapability("automationName", "UiAutomator2");
        return cap;
    }


    public static boolean openDeviceFarmAppiumSession() throws MalformedURLException {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("platformName", "Android"); // Platform (can also be iOS)
            capabilities.setCapability("deviceName", "DeviceName"); // You can specify a device or use a pool
            capabilities.setCapability("app", "arn:aws:devicefarm:us-west-2:192549843005:upload:000d9278-b81b-4319-93c8-87a7bba8f00b/bad4d852-0300-4716-be4d-63ad5c330ea6"); // ARN of your uploaded app
            capabilities.setCapability("automationName", "Appium");

            // AWS Device Farm endpoint URL (ensure to use the correct region)
            String deviceFarmAppiumUrl = "https://devicefarm.us-west-2.amazonaws.com/wd/hub"; // Update region if necessary

            // Create the remote driver connection to AWS Device Farm's Appium server
            driver = new AndroidDriver(new URL(deviceFarmAppiumUrl), capabilities);
            return true;
        } catch (Exception exp) {
            log.error("Error starting Appium session: {}", exp.getMessage());
            log.error("Stack Trace: ", exp);
        }
        return false;
    }


    public static boolean openLocalAppiumSession(String deviceName, String packageName, String activityName, String portNumber) throws MalformedURLException {
        try {
            startAppium(portNumber);
            DesiredCapabilities cap = new DesiredCapabilities();
            cap.setCapability("deviceName", "Pixel6");
            cap.setCapability("udid",deviceName );
            cap.setCapability("platformName", "Android");
            cap.setCapability("platformVersion", "14");
            cap.setCapability("appPackage", packageName);
            cap.setCapability("appActivity", activityName);

//

            cap.setCapability("automationName", "UiAutomator2");
            URL url = new URL("http://127.0.0.1:" + portNumber);
            driver = new AndroidDriver<>(url, cap);
            System.out.println("Application Started");
            return true;
        } catch (Exception exp) {
            log.error("Error starting Appium session: {}", exp.getMessage());
            log.error("Stack Trace: ", exp);
        }
        return false;
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

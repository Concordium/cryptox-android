package cryptox_AndroidTest;

import config.appiumconnection;
import org.openqa.selenium.Dimension;
import org.testng.annotations.*;
import java.net.MalformedURLException;
import static config.appiumconnection.*;
import static config.systemInfo.getDeviceDimensions;
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
public static String stagePackageName = "com.pioneeringtechventures.wallet.stagenet";
public static String activityName = "com.concordium.wallet.ui.MainActivity";
public static String PackageName = stagePackageName;
public static String slackUrl = "https://hooks.slack.com/services/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";



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
}

@AfterTest
    public void tearDown(){
////    driver.removeApp(PackageName); // This will remove the app from the device
//}
    if(!localExecution){
        driver.removeApp(PackageName); // This will remove the app from the device
    }
    else {
        driver.quit(); // This will close the appium driver.
    }
}
}


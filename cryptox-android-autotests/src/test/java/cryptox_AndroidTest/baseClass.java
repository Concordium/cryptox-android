package cryptox_AndroidTest;
import static config.systemInfo.*;
import org.testng.annotations.*;
import java.net.MalformedURLException;
import static config.appiumconnection.*;
import static config.systemInfo.getDeviceDimensions;


public class baseClass {
    public static String appURL;
    public static String Device = "emulator-5554";;
    public static String PackageName = "com.pioneeringtechventures.wallet.testnet";
    public static String activityName = "com.concordium.wallet.ui.MainActivity";

    @BeforeTest
    public void setup() throws MalformedURLException, InterruptedException {


        openAppiumSession(Device, PackageName, activityName);
        getDeviceDimensions();
        getCustomNetwork();
    }

    @AfterTest
    public void tearDown() {
        driver.removeApp(PackageName); // This will remove the app from the device
           }


}


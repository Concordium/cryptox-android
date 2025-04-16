package pages.appOperations;


import static config.appiumconnection.*;
import static cryptox_AndroidTest.baseClass.PackageName;


public class activateApp {
    public static boolean ActivateCryptoXApp() {
        try {
            driver.activateApp(PackageName);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }
}

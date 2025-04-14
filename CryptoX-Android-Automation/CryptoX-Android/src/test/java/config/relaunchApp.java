package config;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;

public class relaunchApp {


    public static By reLaunchCryptoXAppViaIcon = By.xpath("//android.widget.TextView[@content-desc=\"CryptoX Testnet Wallet\"]");

    public static boolean reLaunchCryptoX()

    {
        try
        {

            MobileElement re_LaunchCryptoXAppViaIcon = waitForElement(reLaunchCryptoXAppViaIcon,30);
            assert re_LaunchCryptoXAppViaIcon != null;
            if(re_LaunchCryptoXAppViaIcon.isDisplayed())
            {
                re_LaunchCryptoXAppViaIcon.click();
                Thread.sleep(2000);
                return true;
            }
            else {

                System.out.println("Unable to find element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }




        return false;
    }
}

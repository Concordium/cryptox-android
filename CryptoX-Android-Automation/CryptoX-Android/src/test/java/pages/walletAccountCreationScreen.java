package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class walletAccountCreationScreen {
    static By activateAccountScreen = By.id("create_wallet_button");

    public static boolean activateAccount()

    {
        try
        {
            MobileElement activate_AccountScreen = waitForElement(activateAccountScreen,30);
            assert activate_AccountScreen != null;

            if(activate_AccountScreen.isDisplayed())
            {
                activate_AccountScreen.click();
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

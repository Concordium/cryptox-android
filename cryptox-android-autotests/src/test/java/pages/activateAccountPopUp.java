package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class activateAccountPopUp {
    static By activateAccountPopupButton = By.id("create_wallet_button");

    public static boolean clickOnActivateAccount()

    {
        try
        {

            MobileElement activate_AccountPopUpButton = waitForElement(activateAccountPopupButton,30);
            assert activate_AccountPopUpButton != null;
            if(activate_AccountPopUpButton.isDisplayed())
            {
                activate_AccountPopUpButton.click();
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

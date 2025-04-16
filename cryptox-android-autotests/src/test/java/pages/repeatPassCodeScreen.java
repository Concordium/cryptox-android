package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;
import static pages.createPassCodeScreen.*;

public class repeatPassCodeScreen {

    static By repeatPasscodeScreen = By.id("details_text_view");

    public static boolean repeatPassCodeNow()

    {
        try
        {
            MobileElement create_PassCode = waitForElement(createPassCode,10);
            MobileElement create_PassCode2 = waitForElement(createPassCode2,3);
            MobileElement create_PassCode3 = waitForElement(createPassCode3,3);
            MobileElement create_PassCode4 = waitForElement(createPassCode4,3);
            MobileElement create_PassCode5 = waitForElement(createPassCode5,3);
            MobileElement create_PassCode6 = waitForElement(createPassCode6,3);

            assert create_PassCode != null;

            if(create_PassCode.isDisplayed())
            {
                create_PassCode.click();
                assert create_PassCode2 != null;
                create_PassCode2.click();
                assert create_PassCode3 != null;
                create_PassCode3.click();
                assert create_PassCode4 != null;
                create_PassCode4.click();
                assert create_PassCode5 != null;
                create_PassCode5.click();
                assert create_PassCode6 != null;
                create_PassCode6.click();

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

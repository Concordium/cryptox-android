package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.*;

public class createPassCodeScreen {
    static By createPassCode = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By createPassCode2 = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By createPassCode3 = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By createPassCode4 = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By createPassCode5 = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By createPassCode6 = By.xpath("//android.widget.Button[@text=\"1\"]");
    static By repeatPasscodeScreen = By.id("details_text_view");

    public static boolean createPassCodeNow()

    {
        try
        { Thread.sleep(2000);
            MobileElement create_PassCode = waitForElement(createPassCode,10);
            MobileElement create_PassCode2 = waitForElement(createPassCode2,3);
            MobileElement create_PassCode3 = waitForElement(createPassCode3,3);
            MobileElement create_PassCode4 = waitForElement(createPassCode4,3);
            MobileElement create_PassCode5 = waitForElement(createPassCode5,3);
            MobileElement create_PassCode6 = waitForElement(createPassCode6,3);
            assert create_PassCode != null;
            assert create_PassCode2 != null;
            assert create_PassCode3 != null;
            assert create_PassCode4 != null;
            assert create_PassCode5 != null;
            assert create_PassCode6 != null;

            if(create_PassCode.isDisplayed())
            {
                create_PassCode.click();
                create_PassCode2.click();
                create_PassCode3.click();
                create_PassCode4.click();
                create_PassCode5.click();
                create_PassCode6.click();
                Thread.sleep(3000);
                MobileElement repeat_PassCodeScreen = driver.findElement(repeatPasscodeScreen);
                repeat_PassCodeScreen.isDisplayed();
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

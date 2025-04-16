package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import static config.appiumconnection.*;
import static pages.accountSetup.seedPhraseScreen.confirmPassCode;
import static pages.accountSetup.seedPhraseScreen.pressOkButton;

public class verifyPIN {


    static By submitButton = By.xpath("//android.widget.Button[@text=\"Submit\"]");
    static By gotItButton = By.id("got_it_button");
    static By change_orientation = By.id("com.android.chrome:id/menu_button");
    static By changeBrowserView = By.xpath("//android.widget.TextView[@content-desc=\"Turn on Request desktop site\"]");


    public static boolean verifyPinAndContinue() {
        try {

            MobileElement confirm_PassCode = waitForElement(confirmPassCode, 30);
            MobileElement press_OkButton = waitForElement(pressOkButton, 30);
            assert confirm_PassCode != null;
            assert press_OkButton != null;
            if (confirm_PassCode.isDisplayed()) {
                Thread.sleep(2000);
                confirm_PassCode.sendKeys("111111");
                Thread.sleep(3000);
                press_OkButton.click();
                Thread.sleep(7000);

                MobileElement press_SubmitButton = waitForElement(submitButton, 30);
//                MobileElement changeOrientation = waitForElement(change_orientation, 30);
//                MobileElement change_BrowserView = waitForElement(changeBrowserView, 30);

                if (press_SubmitButton == null){
//                    changeOrientation.click();
//                    Thread.sleep(2000);
//                    change_BrowserView.click();
//                    Thread.sleep(2000);
                    press_SubmitButton.click();

                }
                press_SubmitButton.click();
//                Thread.sleep(3000);
//                MobileElement got_ItButton = waitForElement(gotItButton, 30);
//                assert got_ItButton != null;
//                got_ItButton.click();
                return true;
            } else {

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


    public static boolean verifyPinAndPressOK() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            MobileElement confirm_PassCode = waitForElement(confirmPassCode, 30);
            MobileElement press_OkButton = waitForElement(pressOkButton, 30);
            assert confirm_PassCode != null;
            assert press_OkButton != null;
            if (confirm_PassCode.isDisplayed()) {
                Thread.sleep(2000);
                confirm_PassCode.sendKeys("111111");
                Thread.sleep(3000);
                press_OkButton.click();
                Thread.sleep(7000);

                return true;
            } else {

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
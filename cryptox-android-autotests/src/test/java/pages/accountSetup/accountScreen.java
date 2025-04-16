package pages.accountSetup;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;

public class accountScreen {

    static By onboarding_status_title = By.id("onboarding_status_title");
    static By progressBar = By.id("onboarding_status_progress_bar");

    public static boolean onboardingStatusTitleVerification(String title)

    {
        try
        {


            MobileElement onboardingStatusTitle = waitForElement(onboarding_status_title,30);

            assert onboardingStatusTitle != null;
            String onBoardingStatusTitle = onboardingStatusTitle.getText();

            if(onBoardingStatusTitle.equals(title))
            {

                return true;
            }
            else {

                System.out.println("Title Didn't match Expecting " + title+" getting "+ onBoardingStatusTitle);

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

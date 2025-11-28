package pages.appOperations;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

import static config.appiumconnection.*;
import static config.systemInfo.getCurrentActivity;
import static pages.Transactions.validation.createValidation.validatorSetup;
import static pages.accountSetup.seedPhraseScreen.confirmPassCode;

public class commands {

    private static final int SWIPE_DURATION = 2000;
    private static final int WAIT_TIMEOUT = 30;

    public static boolean swipe() {
        try {
            getCurrentActivity();
            MobileElement sliderThumb = findSliderThumb();
            if (sliderThumb == null) {
                log.error("Slider thumb not found.");
                return false;
            }

            int startX = sliderThumb.getLocation().getX();
            int startY = sliderThumb.getLocation().getY();
            int endX = startX + SWIPE_DURATION;

            // Keep trying until confirmPassCodeElement is found or timeout occurs
            MobileElement confirmPassCodeElement = null;
            int attempts = 0;
            int MAX_ATTEMPTS = 10;
            while (attempts < MAX_ATTEMPTS) {
                // Perform the swipe action
                performSwipe(sliderThumb, startX, startY, endX);
                Thread.sleep(5000);

                // Wait for the confirm passcode screen
                confirmPassCodeElement = waitForElement(confirmPassCode, WAIT_TIMEOUT);
                getCurrentActivity();

                if (confirmPassCodeElement != null) {
                    log.info("Swipe successful: Transaction Confirmation confirmed.");
                    return true;
                } else {
                    log.info("Swipe failed, retrying...");
                    attempts++;
                }
            }

            log.error("Max swipe attempts reached, confirmation not found.");
            return false;

        } catch (Exception e) {
            log.error("Error during swipe operation", e);
            throw new RuntimeException("Error during swipe operation", e);
        }
    }


    private static MobileElement findSliderThumb() {
        By sliderThumbLocator = By.id("slider_thumb");
        return waitForElement(sliderThumbLocator, WAIT_TIMEOUT);
    }

    private static void performSwipe(MobileElement elementToSwipe, int startX, int startY, int endX) {
        Actions action = new Actions(driver);
        action.moveToElement(elementToSwipe)      // Move to the initial element
                .clickAndHold()                    // Click and hold the slider thumb
                .moveByOffset(endX - startX, 0)    // Swipe to the right by the calculated distance
                .release()                          // Release the thumb at the new position
                .perform();
    }

    public static boolean performScroll() {
        try {
            driver.findElement(
                    MobileBy.AndroidUIAutomator(
                            "new UiScrollable(new UiSelector().scrollable(true))" +
                                    ".setAsVerticalList()" +
                                    ".scrollToEnd(20)"
                    )
            );
            return true;
        } catch (Exception e) {
            System.out.println("Could not scroll to the end: " + e.getMessage());
            return false;
        }
    }
}

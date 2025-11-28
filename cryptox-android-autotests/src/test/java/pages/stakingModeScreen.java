package pages;

import com.google.common.collect.ImmutableMap;
import config.appiumconnection;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.accountRecovery.recoveryThroughPrivateKey;

public class stakingModeScreen {

    private static final Logger log = LoggerFactory.getLogger(stakingModeScreen.class);

    // --- Force tap using Appium gestures ---
    private static void forceTap(MobileElement element) {
        int x = element.getCenter().getX();
        int y = element.getCenter().getY();

        ((JavascriptExecutor) appiumconnection.driver).executeScript(
                "mobile: tapGesture",
                ImmutableMap.of("x", x, "y", y)
        );

        log.info("Force tapped checkbox at coordinates ({}, {})", x, y);
    }

    // --- Main method for clicking checkbox ---
    public static boolean clickCheckbox(String elementId, int timeoutInSeconds) {
        try {
            // Try normal click first
            recoveryThroughPrivateKey.clickOnElement(elementId, timeoutInSeconds);

            MobileElement checkbox =
                    appiumconnection.driver.findElement(By.id(elementId));

            String beforeState = checkbox.getAttribute("checked");
            log.info("Checkbox state BEFORE click: {}", beforeState);

            // If not checked -> force tap
            if (!"true".equals(beforeState)) {
                log.info("Checkbox not toggled. Applying force tap...");
                forceTap(checkbox);
                Thread.sleep(500);
            }

            // Read updated state
            String afterState = checkbox.getAttribute("checked");
            log.info("Checkbox state AFTER tap: {}", afterState);

            return "true".equals(afterState);

        } catch (Exception e) {
            log.error("Failed to click checkbox: {}", e.getMessage());
            return false;
        }
    }
}

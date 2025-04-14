package config;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.io.FileHandler;

import java.io.File;
import java.io.IOException;

import static config.appiumconnection.*;

public class screenshotRecord {


    public static boolean takeScreenShot(String pathName) {
        // Capture screenshot
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            // Define the destination file
            File destination = new File(pathName);
            FileHandler.copy(screenshot, destination);
            System.out.println("Screenshot saved as" + pathName + ".png");
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
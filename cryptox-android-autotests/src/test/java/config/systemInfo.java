package config;

import org.openqa.selenium.Dimension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;

public class systemInfo {

    public static LocalDate getCurrentDate() {
        // Get the current date using LocalDate
        LocalDate currentDate = LocalDate.now();

        // Print the current date
        System.out.println("Current Date: " + currentDate);

        return currentDate;
    }

    public static boolean getDeviceDimensions(){

        Dimension screenSize = driver.manage().window().getSize();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();

        System.out.println("Screen Width: " + width);
        System.out.println("Screen Height: " + height);
        return true;
    }


    public static String getSeedPhrase(){

        List<String> seedPhrase = Arrays.asList(
                "gravity dirt slow explain goat wage sample stand spirit chimney athlete van dinosaur rely fragile wagon miracle fun firm ensure find stereo try delay",
                "luxury crouch desk purity medal replace kitten trial work tuition disease poverty icon banana banner tonight salad muscle special eye bleak involve unknown knife",
                "word thumb room input arm enjoy audit dumb echo deputy cherry violin elder promote vivid around below believe awesome word bubble inhale discover benefit");
        // Create a Random object
        Random random = new Random();

        // Randomly select an index
        int randomIndex = random.nextInt(seedPhrase.size());

        // Fetch the private key at the selected index
        return seedPhrase.get(randomIndex);
    }

    public static String getCurrentActivity(){

        String currentActivity = driver.currentActivity();
        log.info("Hello Hello Hello, This is current Activity{}", currentActivity);
        return currentActivity;
    }
}

package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static config.appiumconnection.*;

public class popUps {

    public static By notificationAndroidSettings = By.xpath("//android.view.ViewGroup");
    static By allowNotification = By.id("allow_button");
    static By permission_allow_button = By.id("com.android.permissioncontroller:id/permission_allow_button");
    static By androidButtonClick = By.id("android:id/button1");
    public static By acceptPrivacyPolicy = By.id("terms_check_box");


    public static boolean AcceptNotificationPopUp() {
        try {
//            MobileElement notification_AndroidSettings = waitForElement(notificationAndroidSettings, 30);
            MobileElement allow_Notification = waitForElement(allowNotification, 30);

            if(allow_Notification != null){
                allow_Notification.click();
//                MobileElement permission_allow_Settings = waitForElement(permission_allow_button, 30);
//                assert permission_allow_Settings != null;
//                permission_allow_Settings.click();
//                log.info("successfully clicked {}", permission_allow_Settings);
                return true;
            }
            else {
                return false;
            }


        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }


        return false;
    }


    public static boolean notificationTraySlideDown() {
        try {
            MobileElement notification_popupWindow = waitForElement(notificationAndroidSettings, 30);
            MobileElement allow_Notification = waitForElement(allowNotification, 30);


            assert notification_popupWindow != null;


            if (notification_popupWindow.isDisplayed()) {
                assert allow_Notification != null;
                allow_Notification.click();
                MobileElement allow_NotificationSettings = waitForElement(notificationAndroidSettings, 30);
                assert allow_NotificationSettings != null;
                allow_NotificationSettings.click();

//                assert false;

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

    public static boolean clickOnAndroidDefaultPopUP() {
        try {
            MobileElement android_ButtonClick = waitForElement(androidButtonClick, 30);


            assert android_ButtonClick != null;


            if (android_ButtonClick.isDisplayed()) {
                android_ButtonClick.click();
                Thread.sleep(5000);

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




    public static boolean searchNotificationContent() {
        try {
            // Execute an ADB command to get the notification content
            String command = "adb shell dumpsys notification --noredact";
            Process process = Runtime.getRuntime().exec(command);
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean found = false;

            // Read through the notification content
            while ((line = reader.readLine()) != null) {
                if (line.contains("You Received")) {
                    System.out.println("Found notification with 'You Received': " + line);

                }
            }

            if (!found) {
                System.out.println("'You Received' not found in the current notifications.");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }




}




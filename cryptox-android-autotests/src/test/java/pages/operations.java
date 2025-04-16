package pages;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import java.util.Objects;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;

public class operations {

    public static boolean verifyText(String elementID, Integer waiTime, String compareTo) {

        {
            try {


                MobileElement element_id = waitForElement(By.id(elementID), waiTime);

                assert element_id != null;
                if (element_id.isDisplayed()) {
                    String validateValidatorID = element_id.getText();
                    System.out.println("veriable text was " + validateValidatorID + " but want to compare to " + compareTo);
                    assert Objects.equals(validateValidatorID, compareTo);
                    return true;

                } else {

                    System.out.println("Unable to find" + elementID);
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
}

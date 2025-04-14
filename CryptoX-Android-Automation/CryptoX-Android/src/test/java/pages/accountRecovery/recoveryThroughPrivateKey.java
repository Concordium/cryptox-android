package pages.accountRecovery;

import config.configLoader;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;
import static config.screenshotRecord.takeScreenShot;


public class recoveryThroughPrivateKey {
    static By importWalletButton = By.id("import_wallet_button");
    static By import_privateKey_button = By.id("import_seed_button");
    static By walletPrivateKeyTextBox = By.id("etTitle");
    public static By importSeedPhraseButton = By.id("import_seed_phrase_button");





    public static boolean clickOnImportWalletLink()

    {

        try
        {
            MobileElement import_WalletButton = waitForElement(importWalletButton,30);
            assert import_WalletButton != null;
            if (import_WalletButton.isDisplayed()){
                import_WalletButton.click();
                return true;
            }


            else {

                System.out.println("unable to find import_WalletButton Element");

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }
    public static boolean clickOnImportViaPrivateKey()

    {
        try
        {


            MobileElement importPrivateKeyButton = waitForElement(import_privateKey_button,30);

            assert importPrivateKeyButton != null;
            if (importPrivateKeyButton.isDisplayed()){

                importPrivateKeyButton.click();
                return true;
            }


            else {

                System.out.println("unable to find import_WalletButton Element");

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean clickOnImportViaSeedPhrase()

    {
        try
        {


            MobileElement importSeedPhrase = waitForElement(importSeedPhraseButton,30);

            assert importSeedPhrase != null;
            if (importSeedPhrase.isDisplayed()){

                importSeedPhrase.click();
                return true;
            }


            else {

                System.out.println("unable to find import_WalletButton Element");

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean AddPrivateKey()

    {
        try
        {
            List<String> privateKeys = Arrays.asList(
                    "ad2ee464ea5e968dd0b07dfe7d960e2ae0ce981bfbf293f5d03013e1a58826cf824ed1f7a34ef95bdf2b306416a6d1d211ccaa6bf9e1d824c3bc6dfb9c467f30"
            );
            // Create a Random object
            Random random = new Random();

            // Randomly select an index
            int randomIndex = random.nextInt(privateKeys.size());

            // Fetch the private key at the selected index
            String accountPrivateKey = privateKeys.get(randomIndex);

            MobileElement wallet_PrivateKeyTextBox = waitForElement(walletPrivateKeyTextBox,30);

            assert wallet_PrivateKeyTextBox != null;
            if (wallet_PrivateKeyTextBox.isDisplayed()){

                wallet_PrivateKeyTextBox.clear();
                wallet_PrivateKeyTextBox.sendKeys(accountPrivateKey);
            return true;
            }

            else {

                System.out.println("unable to find import_WalletButton Element");

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean clickOnElement(String elementID, Integer timeout)

    {
        try
        {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){

                elementToLookFor.click();
                log.info("clicked on Element successfully{}", elementID);
                return true;
            }


            else {

                log.error("unable to find Element{}", elementID);
                takeScreenShot("elementID.png");

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }



    public static boolean WaitForElement(String elementID, Integer Timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, Timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()) {


                return true;
            } else {

                log.error("unable to find Element{}", elementID , "While waiting for element");
                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;


    }

    public static boolean elementShouldNotAvailable(String elementID, Integer Timeout) {
        try {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs, Timeout);

            assert elementToLookFor == null;
            return true;

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;


    }

    public static boolean clickOnElementByXpath(String elementID, Integer timeout)

    {
        try
        {
            By elementIDs = By.xpath(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){

                elementToLookFor.click();
                return true;
            }


            else {

                System.out.println("unable to find Element" + elementID );

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean SendTextToField(String elementID, String Text, Integer timeout)

    {
        try
        {
            By elementIDs = By.id(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
                return true;
            }


            else {

                System.out.println("unable to find Element" + elementID );

                return false;
            }

        } catch (Exception exp) {
            log.error(String.valueOf(exp.getCause()));
            System.out.println(exp.getMessage());
            log.error(String.valueOf(exp.fillInStackTrace()));
        }

        return false;
    }

    public static boolean SendTextToFieldByClassName(String elementID, String Text, Integer timeout)

    {
        try
        {
            By elementIDs = By.className(elementID);

            MobileElement elementToLookFor = waitForElement(elementIDs,timeout);

            assert elementToLookFor != null;
            if (elementToLookFor.isDisplayed()){
                elementToLookFor.clear();
                elementToLookFor.click();
                elementToLookFor.sendKeys(Text);
                return true;
            }


            else {

                System.out.println("unable to find Element" + elementID );

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


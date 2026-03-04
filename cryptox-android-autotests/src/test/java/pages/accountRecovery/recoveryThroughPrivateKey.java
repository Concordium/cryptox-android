package pages.accountRecovery;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static config.appiumconnection.log;
import static config.appiumconnection.waitForElement;


public class recoveryThroughPrivateKey {
    static By importWalletButton = By.id("import_wallet_button");
    static By import_privateKey_button = By.id("import_seed_button");
    static By walletPrivateKeyTextBox = By.id("etTitle");
    public static By importSeedPhraseButton = By.id("import_seed_phrase_button");
    public static By import_file_wallet = By.id("import_backup_file_button");


    public static boolean clickOnImportWalletLink() {
        try {
            MobileElement import_WalletButton = waitForElement(importWalletButton, 30);
            assert import_WalletButton != null;
            if (import_WalletButton.isDisplayed()) {
                import_WalletButton.click();
                return true;
            } else {

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

    public static boolean clickOnImportViaPrivateKey() {
        try {
            MobileElement importPrivateKeyButton = waitForElement(import_privateKey_button, 30);

            assert importPrivateKeyButton != null;
            if (importPrivateKeyButton.isDisplayed()) {

                importPrivateKeyButton.click();
                return true;
            } else {

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

    public static boolean clickOnImportViaSeedPhrase() {
        try {
            MobileElement importSeedPhrase = waitForElement(importSeedPhraseButton, 30);

            assert importSeedPhrase != null;
            if (importSeedPhrase.isDisplayed()) {

                importSeedPhrase.click();
                return true;
            } else {

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

    public static boolean AddPrivateKey() {
        try {
            List<String> privateKeys = Arrays.asList(
                    "ad2ee464ea5e968dd0b07dfe7d960e2ae0ce981bfbf293f5d03013e1a58826cf824ed1f7a34ef95bdf2b306416a6d1d211ccaa6bf9e1d824c3bc6dfb9c467f30"
            );
            // Create a Random object
            Random random = new Random();

            // Randomly select an index
            int randomIndex = random.nextInt(privateKeys.size());

            // Fetch the private key at the selected index
            String accountPrivateKey = privateKeys.get(randomIndex);

            MobileElement wallet_PrivateKeyTextBox = waitForElement(walletPrivateKeyTextBox, 30);

            assert wallet_PrivateKeyTextBox != null;
            if (wallet_PrivateKeyTextBox.isDisplayed()) {

                wallet_PrivateKeyTextBox.clear();
                wallet_PrivateKeyTextBox.sendKeys(accountPrivateKey);
                return true;
            } else {

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

    public static boolean clickOnImportViaBackupFile() {
        try {
            MobileElement fileButton = waitForElement(import_file_wallet, 10);
            if (fileButton.isDisplayed()) {
                fileButton.click();
                return true;
            } else {
                System.out.println("Unable to find element");
                return false;
            }
        } catch (Exception exp) {
            log.error(String.valueOf(exp.fillInStackTrace()));
        }
        return false;
    }
}



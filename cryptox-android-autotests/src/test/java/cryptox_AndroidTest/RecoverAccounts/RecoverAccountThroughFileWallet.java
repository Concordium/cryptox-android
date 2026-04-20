package cryptox_AndroidTest.RecoverAccounts;

import cryptox_AndroidTest.baseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;
import static config.systemInfo.getSeedPhrase;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.continueButton;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnImportViaBackupFile;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnImportViaSeedPhrase;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.generalMethods.*;
import static pages.landingScreen.clickGetStarted;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;

public class RecoverAccountThroughFileWallet extends baseClass {

    public static final String WALLET_RESOURCE_PATH = "wallet/exp_file_wallet.concordiumwallet";
    public static final String WALLET_FILE_NAME = "//android.widget.TextView[@resource-id=\"android:id/title\" and @text=\"exp_file_wallet.concordiumwallet\"]";


    public void pushWalletFile() {
        try {
            File walletFile = new File(
                    getClass()
                            .getClassLoader()
                            .getResource(WALLET_RESOURCE_PATH)
                            .toURI()
            );
            if (!walletFile.exists()) {
                throw new RuntimeException("Wallet file not found in resources!");
            }
            byte[] fileContent = Files.readAllBytes(walletFile.toPath());
            String base64Data = Base64.getEncoder().encodeToString(fileContent);

            driver.pushFile("/sdcard/Download/exp_file_wallet.concordiumwallet", base64Data.getBytes());
            log.info("Wallet file pushed successfully!");

            String udid = driver.getCapabilities().getCapability("udid").toString();
            Process process = Runtime.getRuntime().exec(new String[]{
                    "adb", "-s", udid, "shell",
                    "am", "broadcast", "-a",
                    "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    "-d", "file:///sdcard/Download/exp_file_wallet.concordiumwallet"
            });
            process.waitFor();
            log.info("Media store refreshed!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to push wallet file", e);
        }
    }
    private void navigateToDownloadsInFilePicker() {
        try {
            Thread.sleep(2000);
            clickOnElementByXpath("//android.widget.ImageButton[@content-desc=\"Show roots\"]", 10);
            Thread.sleep(1000);
            clickOnElementByXpath("//android.widget.TextView[@text=\"Downloads\"]", 10);
            Thread.sleep(1000);
            log.info("Navigated to Downloads in file picker");
        } catch (Exception e) {
            log.info("Could not navigate to Downloads - file picker may already be there");
        }
    }

    @Test
    public void import_wallet_through_File() throws IOException, InterruptedException {
        getCustomNetwork();
        Thread.sleep(10000);
        pushWalletFile();
        Assert.assertTrue(clickGetStarted());
        Assert.assertTrue(clickOnElement("terms_check_box", 20));
        Assert.assertTrue(clickOnElement("import_wallet_button", 20));
        Assert.assertTrue(createPassCodeNow());
        Assert.assertTrue(repeatPassCodeNow());
        Assert.assertTrue(clickOnImportViaBackupFile());
        Assert.assertTrue(clickOnElement("ok_button", 20));
        navigateToDownloadsInFilePicker();
        Assert.assertTrue(clickOnElementByXpath(WALLET_FILE_NAME, 20));
        Assert.assertTrue(SendTextToField("password_edittext", "000000", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(waitForLoaderToDisappear("message_text_view", 200));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        log.info("successfully recovered Account");
    }

}

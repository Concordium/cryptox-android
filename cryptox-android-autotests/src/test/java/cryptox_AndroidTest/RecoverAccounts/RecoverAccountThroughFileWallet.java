package cryptox_AndroidTest.RecoverAccounts;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

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

public class RecoverAccountThroughFileWallet {

    private static final String WALLET_RESOURCE_PATH = "wallet/exp_file_wallet.concordiumwallet";
    private static final String WALLET_FILE_NAME = "//*[@resource-id='android:id/title' and contains(@text,'exp_file_wallet')]";


    private void pushWalletFile() {

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

            driver.pushFile(
                    "/sdcard/Documents/exp_file_wallet.concordiumwallet",
                    walletFile
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to push wallet file", e);
        }
    }

    @Test
    public void import_wallet_through_File() throws IOException, InterruptedException {
        pushWalletFile();
        Assert.assertTrue(clickGetStarted());
        Assert.assertTrue(clickOnElement("terms_check_box", 20));
        Assert.assertTrue(clickOnElement("import_wallet_button", 20));
        Assert.assertTrue(createPassCodeNow());
        Assert.assertTrue(repeatPassCodeNow());
        Assert.assertTrue(clickOnImportViaBackupFile());
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElementByXpath(WALLET_FILE_NAME, 20));
        Assert.assertTrue(SendTextToField("password_edittext", "000000", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(waitForLoaderToDisappear("message_text_view", 200));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        log.info("successfully recovered Account");
    }

}

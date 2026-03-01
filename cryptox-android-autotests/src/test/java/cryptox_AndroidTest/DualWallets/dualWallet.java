package cryptox_AndroidTest.DualWallets;

import com.beust.ah.A;
import config.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static config.appiumconnection.log;
import static config.systemInfo.getSeedPhrase;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.continueButton;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountThroughFileWallet.WALLET_FILE_NAME;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountThroughFileWallet.WALLET_RESOURCE_PATH;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnImportViaSeedPhrase;
import static pages.createPassCodeScreen.createPassCodeNow;
import static pages.generalMethods.*;
import static pages.generalMethods.clickOnElement;
import static pages.landingScreen.clickGetStarted;
import static pages.login.loginCryptoX;
import static pages.repeatPassCodeScreen.repeatPassCodeNow;
import static pages.verifyPIN.verifyPinAndPressOK;

public class dualWallet {

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
            driver.pushFile(
                    "/sdcard/Documents/exp_file_wallet.concordiumwallet",
                    walletFile
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to push wallet file", e);
        }
    }

    @Test
    public void z_verify_a_user_can_managed_file_base_wallet_and_seed_phrase_wallet() throws MalformedURLException, InterruptedException {
        pushWalletFile();
        Assert.assertTrue(clickGetStarted());
        Assert.assertTrue(clickOnElement("terms_check_box",10));
        Assert.assertTrue(clickOnElement("import_wallet_button",10));
        Assert.assertTrue(createPassCodeNow());
        Assert.assertTrue(repeatPassCodeNow());
        Assert.assertTrue(clickOnImportViaSeedPhrase());
        Assert.assertTrue(SendTextToField("etTitle",getSeedPhrase(), 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement(continueButton,20));
        Assert.assertTrue(verifyPinAndPressOK());
        Thread.sleep(5);
        Assert.assertTrue(WaitForElement(continueButton,100));
        Assert.assertTrue(clickOnElement(continueButton,20));
        log.info("successfully recovered Account");
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElement("icon_image_view", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElementByXpath(WALLET_FILE_NAME, 20));
        Assert.assertTrue(SendTextToField("password_edittext", "000000", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(waitForLoaderToDisappear("message_text_view", 200));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        log.info("successfully recovered Account");
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void verify_a_user_can_create_new_identity() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 10));
        Assert.assertTrue(clickOnElement("identities_layout",10));
        Assert.assertTrue(clickOnElement("toolbar_plus_btn_add_contact_image",10));
        Assert.assertTrue(clickOnElementByXpath("//*[contains(@text,\"Generated IP 0\")]",30));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(WaitForElement("com.android.chrome:id/compositor_view_holder",20));
        Assert.assertTrue(performScrollDown());
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Submit\"]",10));
        Assert.assertTrue(clickOnElement("got_it_button",10));
        Assert.assertTrue(clickOnElement("confirm_button",10));
        Assert.assertTrue(clickOnElement("android:id/button2",10));
        log.info("Successfully created new identity");
    }

    @Test
    public void verify_a_user_can_switch_between_file_based_and_seed__phrase_wallets__seamlessly() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(verifyElementById("wallet_switch_view", 20));
        Assert.assertTrue(verifyElementByXpath("//android.widget.TextView[@text=\"We recommend that you migrate to a seed phrase wallet in order to make use of the full range of Concordium Wallet features.\"]", 20));
        Assert.assertTrue(clickOnElement("wallet_switch_view", 20));
        Assert.assertTrue(verifyElementById("include_seed_phrase_backup_banner", 20));
    }

    @Test
    public void verify_a_user_selected_file_based_wallet_should_remain_loaded_after_app_relaunch() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(verifyElementById("wallet_switch_view", 20));
        Assert.assertTrue(clickOnElement("wallet_switch_view", 20));
        Assert.assertFalse(verifyElementById("toolbar_account_label", 20));
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(verifyElementByXpath("//android.widget.TextView[@text=\"We recommend that you migrate to a seed phrase wallet in order to make use of the full range of Concordium Wallet features.\"]", 20));
    }

    @Test
    public void verify_as_a_user_adding_a_file_based_account_with_incorrect_password_should_fail_gracefully() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElement("icon_image_view", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertTrue(clickOnElementByXpath(WALLET_FILE_NAME, 20));
        Assert.assertTrue(SendTextToField("password_edittext", "111111", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(verifyTextById("message_text_view", "Import failed Invalid password or import file corrupted", 20));
        Assert.assertTrue(clickOnElement("confirm_button", 20));
    }

    @Test
    public void verify_as_a_user_can_reemove_file_based_wallet() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/name_text_view\" and @text=\"File wallet\"]", 20));
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElement("remove_button_text_view", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertFalse(verifyElementById("wallet_switch_view", 20));

    }

    @Test
    public void verify_as_a_see_Add_a_file_wallet_option_when_only_one_wallet_is_added() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(verifyElementById("icon_image_view", 20));
    }

    @Test
    public void verify_as_a_user_can_reemove_seed_phrase_wallet() {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/name_text_view\" and @text=\"Seed phrase wallet\"]", 20));
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn", 20));
        Assert.assertTrue(clickOnElement("wallets_layout", 20));
        Assert.assertTrue(clickOnElement("remove_button_text_view", 20));
        Assert.assertTrue(clickOnElement("ok_button", 20));
        Assert.assertFalse(verifyElementById("wallet_switch_view", 20));
    }



    }

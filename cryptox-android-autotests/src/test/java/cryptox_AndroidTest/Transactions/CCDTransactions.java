package cryptox_AndroidTest.Transactions;

import config.configLoader;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import static config.appiumconnection.driver;
import static config.appiumconnection.openAppiumSession;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.navigation_bar_item_large_label_view;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.newsTabButton;
import static cryptox_AndroidTest.baseClass.*;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.performScroll;
import static pages.appOperations.commands.swipe;
import static pages.generalMethods.*;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class CCDTransactions {
//    config.configLoader configLoader = new configLoader("src/main/resources/setupData.properties");
//    String RecoveryAccount = configLoader.getProperty("RecoveryAccountAddress");
//    public String RecoveryAccount = "4Ek1LdFtdbkSwowStM73fMp6ZZd1RLTvQpRr1ZLJMVgPjUx4JT";



    @Test
    public void Verify_if_user_transfer_CCDS_to_Specific_account() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("menuitem_transfer",20));
        Assert.assertTrue(SendTextToField("amount","1", 20));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("edittext",10));
        Assert.assertTrue(SendTextToField("edittext","4DuEPm6nY3kfAjACe3g5Ed71bkCQNh5AGBSqJQGkRR4vdxKTQN", 20));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));
    }

    @Test
    public void Verify_if_user_transfer_CCDS_to_Specific_account_WithMemo() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("menuitem_transfer",10));
        Assert.assertTrue(SendTextToField("amount","1", 20));
        Assert.assertTrue(performScroll());
        Assert.assertTrue(clickOnElement("memo_layout", 20));
        Assert.assertTrue(clickOnElement("show_button",10));
        Assert.assertTrue(SendTextToField("memo_edittext","This is Test Memo - Triggered through automated tests", 20));
        Assert.assertTrue(clickOnElement("confirm_button",10));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("edittext",10));
        Assert.assertTrue(SendTextToField("edittext","4DuEPm6nY3kfAjACe3g5Ed71bkCQNh5AGBSqJQGkRR4vdxKTQN", 20));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(clickOnElement("continue_btn",40));
        Assert.assertTrue(swipe());
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));
    }

    @Ignore
    public void Verify_if_user_can_add_Payee() throws MalformedURLException, InterruptedException {
        Assert.assertTrue(clickOnElement("menuitem_transfer",20));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
        Assert.assertTrue(clickOnElement("toolbar_plus_btn_add_contact_image",20));
        Assert.assertTrue(SendTextToField("recipient_name_edittext","Test", 20));
        Assert.assertTrue(SendTextToField("recipient_address_edittext","4Ek1LdFtdbkSwowStM73fMp6ZZd1RLTvQpRr1ZLJMVgPjUx4JT", 20));
        Assert.assertTrue(clickOnElement("save_button",40));
        Assert.assertTrue(elementShouldNotAvailable("snackbar_text",10));
    }
}
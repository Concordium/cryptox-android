package cryptox_AndroidTest.Transactions;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.navigation_bar_item_large_label_view;
import static cryptox_AndroidTest.RecoverAccounts.RecoverAccountCases.newsTabButton;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.requestCCDs.clickOnRequestCCDs;
import static pages.accountRecovery.recoveryThroughPrivateKey.SendTextToField;
import static pages.accountRecovery.recoveryThroughPrivateKey.clickOnElement;
import static pages.appOperations.commands.swipe;
import static pages.login.loginCryptoX;
import static pages.popUps.AcceptNotificationPopUp;
import static pages.verifyPIN.verifyPinAndPressOK;

public class RequestForCCDs {


    @Ignore
    public void Verify_if_user_can_request_for_CCDs() throws MalformedURLException, InterruptedException {

//        driver.terminateApp(PackageName);
//        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(clickOnElement("receive_btn",10));
        Assert.assertTrue(clickOnElement("copy_address_layout",10));
        Assert.assertTrue(clickOnElement("toolbar_back_btn",10));
        Assert.assertTrue(clickOnElement("send_funds_btn",10));
        Assert.assertTrue(clickOnElement("recipient_layout",10));
//        Assert.assertTrue(SendTextToField("search_src_text",RecoveryAccount, 20));
        Assert.assertTrue(clickOnElement("continue_btn",10));
        Assert.assertTrue(SendTextToField("amount","1", 20));
        Assert.assertTrue(clickOnElement("continue_btn",10));
        Assert.assertTrue(swipe());
        Assert.assertTrue(clickOnElement("send_funds",10));
        Assert.assertTrue(verifyPinAndPressOK());
        Assert.assertTrue(clickOnElement("finish",10));


    }
}

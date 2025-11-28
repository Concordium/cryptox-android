package cryptox_AndroidTest.Settings;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.accountRecovery.recoveryThroughPrivateKey.*;
import static pages.appOperations.commands.swipe;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class settingsCases {

    @Test
    public void Verify_if_users_can_remove_account_from_cryptoX() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn",10));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@text=\"Erase Data\"]\n",10));
        Assert.assertTrue(clickOnElement("confirm_button",10));
        Assert.assertTrue(verifyPinAndPressOK());

    }
}

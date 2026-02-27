package cryptox_AndroidTest.Settings;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.PackageName;
import static pages.generalMethods.clickOnElement;
import static pages.generalMethods.performScrollDown;
import static pages.login.loginCryptoX;
import static pages.verifyPIN.verifyPinAndPressOK;

public class removeAccount {

    @Test
    public void Verify_if_users_can_remove_account_from_cryptoX() throws MalformedURLException, InterruptedException {
        driver.terminateApp(PackageName);
        driver.activateApp(PackageName);
        Assert.assertTrue(loginCryptoX());
        Assert.assertTrue(clickOnElement("toolbar_menu_drawer_btn",10));
        Assert.assertTrue(performScrollDown());
        Assert.assertTrue(clickOnElement("erase_data_layout",20));
        Assert.assertTrue(clickOnElement("ok_button",10));
        Assert.assertTrue(verifyPinAndPressOK());
    }
}

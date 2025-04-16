package cryptox_AndroidTest;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static pages.Transactions.requestCCDs.clickOnAccountWidget;
import static pages.Transactions.requestCCDs.clickOnRequestCCDs;
import static pages.popUps.AcceptNotificationPopUp;

public class notificationTests {

    @Test
    public void B_notification() throws MalformedURLException {
        Assert.assertTrue(AcceptNotificationPopUp());

    }

    @Ignore
    public void Verify_Notification_for_CCD_Transaction() throws MalformedURLException {
        Assert.assertTrue(clickOnAccountWidget());
        Assert.assertTrue(clickOnRequestCCDs());

    }
}

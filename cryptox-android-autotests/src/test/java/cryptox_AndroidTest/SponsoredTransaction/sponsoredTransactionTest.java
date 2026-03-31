package cryptox_AndroidTest.SponsoredTransaction;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.stagePackageName;
import static pages.generalMethods.clickOnElementByXpath;
import static pages.login.loginCryptoX;
import static pages.sponsoredTransaction.*;
import static pages.verifyPIN.verifyPinAndPressOK;


public class sponsoredTransactionTest {

    public String amount = "1 CCD";
    public String sponsoredAccount = "3sZEtP1WePzkg7phrYx5BqrkkFREMmuK8BcF9kTZF8oKuKE7bh";
    public String recipientAddress = "3s9FrQovUTJ99J8zozfJRscu4qR2a8bU5oL42YLZvxcVAuBApW";

    @Test
    public void verify_end_to_end_sponsored_CCD_transfer_successful_Flow() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/amount_text_view\"]"), amount, 20));
        //Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@text=\"Free transaction\"]", 20));
        // Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/description\"]"), sponsoredAccount, 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/accAddress\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/approve_button\"]", 5));
        Assert.assertTrue(verifyPinAndPressOK());
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.ImageButton[@content-desc=\"Copy the hash\"]", 20));
        String appHash = driver.getClipboardText();
        System.out.println("Hash from App: " + appHash);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/finish_button\"]", 20));
        driver.activateApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {
            Set<String> contexts = driver.getContextHandles();
            for (String context : contexts) {
                System.out.println("Context found: " + context);
                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        driver.get("https://stagenet.ccdscan.io/transactions");
        Thread.sleep(5000);
        driver.navigate().refresh();
        Thread.sleep(30000);
        driver.navigate().refresh();
        Thread.sleep(5000);
        String webHash = driver.findElement(By.xpath("(//td[@class='py-3 px-3 whitespace-nowrap align-top'])[1]")).getText();
        System.out.println("Hash from Web: " + webHash);
        if (appHash.startsWith(webHash)) {
            System.out.println("Hashes match");
        } else {
            System.out.println("Hashes do NOT match");
        }
        driver.findElement(By.xpath("//tbody/tr[2]/td[1]/div[1]/button[1]/span[1]")).click();
        Thread.sleep(5000);
        String events = driver.findElement(By.xpath("//span[contains(text(),'from')]")).getText();
        System.out.println("Hash from Web: " + events);
        boolean result = verifyAccountsOnDashboard(sponsoredAccount, recipientAddress);
        System.out.println("Accounts verification result: " + result);
    }

    @Test
    public void verify_sponsored_transaction_when_user_decline_Test_bench_connection() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/decline_button\"]", 20));
        driver.activateApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        boolean isPresent = driver.findElements(By.xpath("//android.widget.Button[@text='Select Wallet']")).size() > 0;
        Assert.assertFalse(isPresent, "Select Wallet button should not appear");
    }

    @Test
    public void verify_sponsored_transaction_when_user_rejects_transaction_signing() throws Exception {
//        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
        for (int i = 0; i < 3; i++) {
            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        Thread.sleep(8000);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/amount_text_view\"]"), amount, 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/decline_button\"]", 5));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_incorrect_sponsor_private_key() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_recipient_address() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys("3s9FrQovscu4qR2a8bU5oL42YLZvxcVAuBApW");
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_sponsored_address() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys("3sZEtP1WekFREMmuK8BcF9kTZF8oKuKE7bh");
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_amount() throws Exception {
        driver.terminateApp(stagePackageName);
        driver.activateApp(stagePackageName);
        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
        System.out.println("Chrome launched with URL...");
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.stagenet:id/allow_button\"]", 20));
        switchBackToApp("com.android.chrome");
           for (int i = 0; i < 3; i++) {

            Set<String> contexts = driver.getContextHandles();

            for (String context : contexts) {

                System.out.println("Context found: " + context);

                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to WebView: " + context);
                    break;
                }
            }
            Thread.sleep(3000);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,24000)");
        Thread.sleep(2000);
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys("  ");
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.stagenet:id/receiver_text_view']"), recipientAddress, 20));
    }

}
package cryptox_AndroidTest.SponsoredTransaction;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static config.appiumconnection.driver;
import static config.appiumconnection.switchBackToApp;
import static cryptox_AndroidTest.baseClass.packageName;
import static pages.generalMethods.clickOnElement;
import static pages.generalMethods.clickOnElementByXpath;
import static pages.login.loginCryptoX;
import static pages.sponsoredTransaction.*;
import static pages.verifyPIN.verifyPinAndPressOK;


public class sponsoredTransactionTest {

    public String amount = "1 CCD";
    public String sponsoredAccount = "3sZEtP1WePzkg7phrYx5BqrkkFREMmuK8BcF9kTZF8oKuKE7bh";
    public String recipientAddress = "3s9FrQovUTJ99J8zozfJRscu4qR2a8bU5oL42YLZvxcVAuBApW";
    private static final String DAPP_URL = "https://wallet-test-bench.testnet.concordium.com/";
    private static final String PRIVATE_KEY = "7f06f1b4813254a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4";

    private void launchChromeWithUrl(String url) throws InterruptedException {
        try {
            ((AndroidDriver) driver).executeScript("mobile: shell",
                    new HashMap<String, Object>() {{
                        put("command", "am");
                        put("args", Arrays.asList(
                                "start",
                                "-a", "android.intent.action.VIEW",
                                "-d", url,
                                "--activity-clear-task",
                                "--activity-clear-top",
                                "com.android.chrome"
                        ));
                    }}
            );
            System.out.println("Chrome launched via intent: " + url);
        } catch (Exception e) {
            System.out.println("Intent launch failed: " + e.getMessage());
            try {
                String command = "adb -s emulator-5554 shell am start -a android.intent.action.VIEW -d \"" + url + "\" com.android.chrome";
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();
                System.out.println("Chrome launched via ADB fallback");
            } catch (Exception ex) {
                System.out.println("ADB fallback failed: " + ex.getMessage());
            }
        }
        Thread.sleep(5000);
    }

    private void switchToWebViewWithWait() throws InterruptedException {
        int maxRetries = 15;
        for (int i = 0; i < maxRetries; i++) {
            Set<String> contexts = driver.getContextHandles();
            System.out.println("Available contexts (attempt " + (i + 1) + "): " + contexts);
            for (String context : contexts) {
                if (context.contains("WEBVIEW") || context.contains("CHROMIUM")) {
                    driver.context(context);
                    System.out.println("Switched to context: " + context);
                    Thread.sleep(3000); // ✅ wait after switching for page to stabilize
                    return;
                }
            }
            Thread.sleep(3000);
        }
        System.out.println("WARNING: Could not switch to WebView context");
    }

    @Test
    public void verify_end_to_end_sponsored_CCD_transfer_successful_Flow() throws Exception {
        driver.activateApp(packageName);
    //    loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys(PRIVATE_KEY);
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 20));
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/amount_text_view\"]"), amount, 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/accAddress\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/approve_button\"]", 5));
        Assert.assertTrue(verifyPinAndPressOK());
        Thread.sleep(3000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.ImageButton[@content-desc=\"Copy the hash\"]", 20));
        String appHash = driver.getClipboardText();
        System.out.println("Hash from App: " + appHash);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/finish_button\"]", 20));
        driver.activateApp("com.android.chrome");
        switchToWebViewWithWait();
        driver.get("https://testnet.ccdscan.io/transactions");
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
        System.out.println("Events from Web: " + events);
        boolean result = verifyAccountsOnDashboard(sponsoredAccount, recipientAddress);
        System.out.println("Accounts verification result: " + result);
    }

    @Test
    public void verify_sponsored_transaction_when_user_decline_Test_bench_connection() throws Exception {
   //     driver.terminateApp(packageName);
        driver.activateApp(packageName);
 //       loginCryptoX();
        launchChromeWithUrl(DAPP_URL);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/decline_button\"]", 20));
        driver.activateApp("com.android.chrome");
        switchToWebViewWithWait();
        boolean isPresent = driver.findElements(By.xpath("//android.widget.Button[@text='Select Wallet']")).size() > 0;
        Assert.assertFalse(isPresent, "Select Wallet button should not appear");
    }

    @Test
    public void verify_sponsored_transaction_when_user_rejects_transaction_signing() throws Exception {
      //  driver.terminateApp(packageName);
//        driver.activateApp(packageName);
//        loginCryptoX();
//        launchChromeWithUrl(DAPP_URL);
//        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
//        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
//        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
//        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/allow_button\"]", 20));
//        driver.activateApp("com.android.chrome");
//        switchToWebViewWithWait();
        driver.activateApp(packageName);
        loginCryptoX();
        Assert.assertTrue(clickOnElement("ok_button", 5));
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys(PRIVATE_KEY);
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 30));
        Assert.assertTrue(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/amount_text_view\"]"), amount, 30));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@resource-id=\"com.pioneeringtechventures.wallet.testnet:id/decline_button\"]", 5));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_incorrect_sponsor_private_key() throws Exception {
        driver.activateApp(packageName);
//        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys("7f06f1b4a168f3cb306b550af50c988eeca61eda22bc56a7b2f5afa4b4");
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_recipient_address() throws Exception {
//        driver.terminateApp(packageName);
        driver.activateApp(packageName);
//        loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys(PRIVATE_KEY);
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys("3s9FrQovscu4qR2a8bU5oL42YLZvxcVAuBApW");
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_sponsored_address() throws Exception {
       // driver.terminateApp(packageName);
        driver.activateApp(packageName);
      //  loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys("3sZEtP1WekFREMmuK8BcF9kTZF8oKuKE7bh");
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys(PRIVATE_KEY);
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys(amount);
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 20));
    }

    @Test
    public void verify_sponsored_transaction_when_user_enter_invalid_amount() throws Exception {
        driver.activateApp(packageName);
     //   loginCryptoX();
        switchBackToApp("com.android.chrome");
        Activity chromeActivity = new Activity("com.android.chrome", "com.google.android.apps.chrome.Main");
        chromeActivity.setOptionalIntentArguments("https://wallet-test-bench.testnet.concordium.com/");
        ((AndroidDriver) driver).startActivity(chromeActivity);
         System.out.println("Chrome launched with URL...");
        Thread.sleep(5000);
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Use Wallet Connect\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Connect Mobile Wallet\"]", 20));
        Assert.assertTrue(clickOnElementByXpath("//android.widget.Button[@text=\"Select Wallet\"]", 20));
        Assert.assertTrue(clickOnElement("allow_button", 50));
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
        driver.findElement(By.xpath("(//input[@id='sponsorAccountCcd'])[1]")).sendKeys(sponsoredAccount);
        driver.findElement(By.xpath("//input[@id='sponsorPrivateKeyCcd']")).sendKeys(PRIVATE_KEY);
        driver.findElement(By.xpath("(//input[@id='sponsoredRecipientCcd'])[1]")).sendKeys(recipientAddress);
        driver.findElement(By.xpath("//input[@id='sponsoredCcdAmount']")).sendKeys("  ");
        WebElement submitBtn = driver.findElement(By.xpath("//button[normalize-space()='Sign And Submit Sponsored CCD Transfer']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(10000);
        Assert.assertFalse(verifyTextOnApp(By.xpath("//android.widget.TextView[@resource-id='com.pioneeringtechventures.wallet.testnet:id/receiver_text_view']"), recipientAddress, 20));
    }
}
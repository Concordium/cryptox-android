package config;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ITestContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static config.appiumconnection.driver;


public class CustomTestListener implements ITestListener {

    // List to store test results
    private final List<ITestResult> testResults = new ArrayList<>();
    private final List<ITestResult> failedTests = new ArrayList<>();


    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("Test started: " + result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("Test passed: " + result.getName());
        testResults.add(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("Test failed: " + result.getName());
        testResults.add(result);
        failedTests.add(result);

        File screenshotsDir = new File("screenshots");
        if (!screenshotsDir.exists()) screenshotsDir.mkdirs();

        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fileName = "screenshots/" + result.getName() + "_" + System.currentTimeMillis() + ".png";
            File destFile = new File(fileName);
            FileUtils.copyFile(srcFile, destFile);
            System.out.println("Screenshot saved: " + destFile.getAbsolutePath());
        } catch (WebDriverException e) {
            System.err.println("Could not take screenshot. UiAutomator2 may have crashed: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("Test skipped: " + result.getName());
        testResults.add(result);
    }

    @Override
    public void onStart(ITestContext context) {
        System.out.println("Test suite started: " + context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("Test suite finished: " + context.getName());
        sendSlackNotification();
    }

    public List<ITestResult> getTestResults() {
        return testResults;
    }

    private void sendSlackNotification() {
        List<ITestResult> results = getTestResults();

        StringBuilder resultMessage = new StringBuilder();
        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (ITestResult result : results) {
            switch (result.getStatus()) {
                case ITestResult.SUCCESS:
                    passed++;
                    break;
                case ITestResult.FAILURE:
                    failed++;
                    break;
                case ITestResult.SKIP:
                    skipped++;
                    break;
            }
        }

        resultMessage.append("Automation Tests for CryptoX Android - Stagenet:\n")
                .append("Executed at ").append(java.time.LocalDate.now()).append("\n")
                .append("Passed: ").append(passed).append("\n")
                .append("Failed: ").append(failed).append("\n")
                .append("Skipped: ").append(skipped).append("\n");
        if (!failedTests.isEmpty()) {
            resultMessage.append("List of Failed Test Cases:\n");
            for (ITestResult failedTest : failedTests) {
                resultMessage.append("- ").append(failedTest.getName()).append("\n");
            }
        }

        sendSlackMessage(resultMessage.toString());
    }

    private void sendSlackMessage(String message) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            final String SLACK_WEBHOOK_URL = System.getenv("SLACK_WEBHOOK");

            HttpPost post = new HttpPost(SLACK_WEBHOOK_URL);
            StringEntity entity = new StringEntity("{\"text\":\"" + message + "\"}");
            post.setEntity(entity);
            post.setHeader("Content-Type", "application/json");

            client.execute(post);
            System.out.println("Slack notification sent!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to send Slack notification.");
        }
    }
}

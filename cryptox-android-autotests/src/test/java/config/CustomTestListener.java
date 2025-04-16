package config;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import org.apache.commons.io.FileUtils;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ITestContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.slackUrl;

public class CustomTestListener implements ITestListener {

    // List to store test results
    private final List<ITestResult> testResults = new ArrayList<>();

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

        // Screenshot capture is currently disabled
        /*
        if (driver != null) {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            try {
                // Save screenshot to a file
                String screenshotPath = "screenshots/" + result.getName() + ".png";
                FileUtils.copyFile(screenshot, new File(screenshotPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
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

    private String getStatus(int status) {
        switch (status) {
            case ITestResult.SUCCESS:
                return "PASS";
            case ITestResult.FAILURE:
                return "FAIL";
            case ITestResult.SKIP:
                return "SKIPPED";
            default:
                return "UNKNOWN";
        }
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

        resultMessage.append("Automation Tests for CryptoX Android (Build 14.0):\n")
                .append("Executed at ").append(java.time.LocalDate.now()).append("\n")
                .append("Passed: ").append(passed).append("\n")
                .append("Failed: ").append(failed).append("\n")
                .append("Skipped: ").append(skipped).append("\n");

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

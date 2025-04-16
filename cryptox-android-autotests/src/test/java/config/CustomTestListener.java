package config;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.ITestContext;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static config.appiumconnection.driver;
import static cryptox_AndroidTest.baseClass.slackUrl;

public class CustomTestListener implements ITestListener {

    // List to store test results
    private final List<ITestResult> testResults = new ArrayList<>();

    // This method is called when a test starts
    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("Test started: " + result.getName());
    }

    // This method is called when a test passes
    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("Test passed: " + result.getName());
        testResults.add(result);  // Store the passed test result
    }

    // This method is called when a test fails
    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("Test failed: " + result.getName());
        testResults.add(result);  // Store the failed test result

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
    }

    // This method is called when a test is skipped
    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("Test skipped: " + result.getName());
        testResults.add(result);  // Store the skipped test result
    }

    // This method is called before the test suite starts
    @Override
    public void onStart(ITestContext context) {
        System.out.println("Test suite started: " + context.getName());
    }

    // This method is called after the test suite finishes
    @Override
    public void onFinish(ITestContext context) {
        System.out.println("Test suite finished: " + context.getName());
        sendSlackNotification();  // Send Slack notification at the end of the suite
    }

    // Utility method to get status string based on result status
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

    // Method to retrieve the collected test results
    public List<ITestResult> getTestResults() {
        return testResults;
    }

    // Method to send a Slack notification with the summary of results
    private void sendSlackNotification() {
        List<ITestResult> results = getTestResults();

        StringBuilder resultMessage = new StringBuilder();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        for (ITestResult result : results) {
            if (result.getStatus() == ITestResult.SUCCESS) {
                passed++;
            } else if (result.getStatus() == ITestResult.FAILURE) {
                failed++;
            } else if (result.getStatus() == ITestResult.SKIP) {
                skipped++;
            }
        }

        resultMessage.append("Automation Tests for CryptoX Android (Build 14.0):\n")
                .append("Executed at ").append(java.time.LocalDate.now()).append("\n")
                .append("Passed: ").append(passed).append("\n")
                .append("Failed: ").append(failed).append("\n")
                .append("Skipped: ").append(skipped).append("\n");

        sendSlackMessage(resultMessage.toString());
    }

    // Method to send the formatted result message to Slack
    private void sendSlackMessage(String message) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            final String SLACK_WEBHOOK_URL = slackUrl;

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

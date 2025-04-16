package config;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    private int count = 0;
    private static final int maxRetryCount = 3; // Number of retries

    @Override
    public boolean retry(ITestResult result) {
        if (count < maxRetryCount) {
            count++;
            System.out.println("Retrying test " + result.getName() + " for the " + count + " time(s).");
            return true; // Retry the test
        }
        return false; // No more retries, mark the test as failed
    }
}

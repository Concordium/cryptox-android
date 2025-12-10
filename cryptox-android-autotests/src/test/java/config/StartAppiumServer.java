package config;

import java.io.File;
import java.io.IOException;

public class StartAppiumServer {


    public static void startAppium(String AppiumPort) {
        ProcessBuilder processBuilder = new ProcessBuilder("node", "/usr/local/lib/node_modules/appium/build/lib/main.js","--port",AppiumPort);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(new File("")); // Redirect output to a log file

        try {
            Thread.sleep(5000); // Adjust the sleep time if necessary
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static boolean runBatchFile(String batchFilePath) {
        // Create a ProcessBuilder instance
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(batchFilePath);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(new File("batch_output.log")); // Redirect output to a log file

        try {
            // Start the process
            Process process = processBuilder.start();
            // Wait for the process to complete and get the exit code
            int exitCode = process.waitFor();
            System.out.println("Batch file executed with exit code: " + exitCode);
            return true;
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Process was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        return false;
    }
}

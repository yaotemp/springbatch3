package com.example.dataexport;

import com.example.dataexport.util.Provider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class InvalidTinGenerator {

    // Output file will be in the project root directory.
    // You might want to change this to an absolute path or a path relative to a specific output directory.
    private static final String OUTPUT_FILE_NAME = "all_invalid_tins.txt";

    public static void main(String[] args) {
        System.out.println("Starting generation of all invalid TINs. This will take a very long time.");
        System.out.println("Output file: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());

        long totalInvalidTinsCounter = 0;
        long totalCheckedCounter = 0;
        long startTime = System.currentTimeMillis();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_NAME))) {
            // --- Section 1: Generate Invalid SSN-formatted numbers (includes invalid ITIN-formatted) ---
            System.out.println("\n--- Starting SSN/ITIN Format Invalid Number Generation ---");
            long ssnItinStartTime = System.currentTimeMillis();
            long currentInvalidSsnItinCounter = 0;
            long currentCheckedSsnItinCounter = 0;

            for (int area = 0; area <= 999; area++) {
                if (area % 10 == 0) {
                    logProgress("SSN/ITIN Area", area, currentCheckedSsnItinCounter, currentInvalidSsnItinCounter, ssnItinStartTime);
                }
                for (int group = 0; group <= 99; group++) {
                    for (int serial = 0; serial <= 9999; serial++) {
                        currentCheckedSsnItinCounter++;
                        if (Provider.isSsnInvalidAccordingToMd(area, group, serial)) {
                            writer.write(String.format("%03d-%02d-%04d%n", area, group, serial));
                            currentInvalidSsnItinCounter++;
                        }
                    }
                }
            }
            totalInvalidTinsCounter += currentInvalidSsnItinCounter;
            totalCheckedCounter += currentCheckedSsnItinCounter;
            logCompletion("SSN/ITIN Format Invalid Number Generation", currentCheckedSsnItinCounter, currentInvalidSsnItinCounter, ssnItinStartTime);

            // --- Section 2: Generate Invalid EIN-formatted numbers ---
            System.out.println("\n--- Starting EIN Format Invalid Number Generation ---");
            long einStartTime = System.currentTimeMillis();
            long currentInvalidEinCounter = 0;
            long currentCheckedEinCounter = 0;

            for (int prefixVal = 0; prefixVal <= 99; prefixVal++) {
                String prefixStr = String.format("%02d", prefixVal);
                if (prefixVal % 1 == 0) { // Log every prefix for EINs, as there are only 100.
                    logProgress("EIN Prefix", prefixVal, currentCheckedEinCounter, currentInvalidEinCounter, einStartTime);
                }
                boolean isPrefixValid = Provider.VALID_EIN_PREFIXES.contains(prefixStr);
                if (isPrefixValid) { // If prefix is valid, all serials under it form valid EINs (structurally), so skip.
                    currentCheckedEinCounter += 10_000_000; // All 10M serials for this valid prefix
                    continue;
                }

                // If prefix is NOT valid, all serial numbers under it form an invalid EIN.
                for (int serial = 0; serial <= 9_999_999; serial++) {
                    currentCheckedEinCounter++;
                    writer.write(String.format("%s-%07d%n", prefixStr, serial));
                    currentInvalidEinCounter++;
                }
            }
            totalInvalidTinsCounter += currentInvalidEinCounter;
            totalCheckedCounter += currentCheckedEinCounter; // Note: checked counter for valid prefixes was added in bulk.
            logCompletion("EIN Format Invalid Number Generation", currentCheckedEinCounter, currentInvalidEinCounter, einStartTime);

            writer.flush();
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n--- Overall Generation Complete ---");
        System.out.printf("Total TINs checked (approx.): %d%n", totalCheckedCounter);
        System.out.printf("Total invalid TINs found and written: %d%n", totalInvalidTinsCounter);
        logTimeStats(startTime);
        System.out.println("Output file located at: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());
    }

    private static void logProgress(String stageLabel, int currentMajorStep, long checkedCounter, long invalidCounter, long stageStartTime) {
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - stageStartTime) / 1000.0;
        System.out.printf("Processing %s: %03d. Checked so far (current stage): %d. Invalid found (current stage): %d. Time elapsed (current stage): %.2f sec.%n",
                stageLabel, currentMajorStep, checkedCounter, invalidCounter, elapsedSeconds);
    }

    private static void logCompletion(String stageName, long checkedCounter, long invalidCounter, long stageStartTime) {
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - stageStartTime) / 1000.0;
        System.out.printf("--- %s Complete ---%n", stageName);
        System.out.printf("Checked in stage: %d%n", checkedCounter);
        System.out.printf("Invalid found in stage: %d%n", invalidCounter);
        System.out.printf("Time taken for stage: %.2f seconds (%.2f minutes)%n", totalTimeSeconds, totalTimeSeconds / 60.0);
    }
    
    private static void logTimeStats(long overallStartTime) {
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - overallStartTime) / 1000.0;
        System.out.printf("Total time taken: %.2f seconds (%.2f minutes / %.2f hours)%n",
                totalTimeSeconds, totalTimeSeconds / 60.0, totalTimeSeconds / 3600.0);
    }
} 
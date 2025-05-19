package com.example.dataexport.generators;

import com.example.dataexport.util.Provider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class InvalidSsnGenerator {

    private static final String OUTPUT_FILE_NAME = "all_invalid_ssns.txt";

    public static void main(String[] args) {
        System.out.println("Starting generation of all invalid SSN-formatted numbers (includes ITIN-formatted that are not valid ITINs).");
        System.out.println("Output file: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());

        long invalidSsnCounter = 0;
        long checkedSsnCounter = 0;
        long startTime = System.currentTimeMillis();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_NAME))) {
            System.out.println("--- Generating Invalid SSN/ITIN-formatted Numbers ---");

            for (int area = 0; area <= 999; area++) {
                if (area % 10 == 0) { // Log every 10 areas
                    logProgress("SSN/ITIN Area", area, checkedSsnCounter, invalidSsnCounter, startTime);
                }
                for (int group = 0; group <= 99; group++) {
                    for (int serial = 0; serial <= 9999; serial++) {
                        checkedSsnCounter++;
                        if (Provider.isSsnInvalidAccordingToMd(area, group, serial)) {
                            writer.write(String.format("%03d-%02d-%04d%n", area, group, serial));
                            invalidSsnCounter++;
                        }
                    }
                }
            }
            writer.flush();
            logCompletion("Invalid SSN/ITIN-formatted Number Generation", checkedSsnCounter, invalidSsnCounter, startTime);

        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Output file located at: " + Paths.get(OUTPUT_FILE_NAME).toAbsolutePath());
    }

    private static void logProgress(String stageLabel, int currentMajorStep, long checkedCounter, long invalidCounter, long stageStartTime) {
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - stageStartTime) / 1000.0;
        System.out.printf("Processing %s: %03d. Checked so far: %d. Invalid found: %d. Time elapsed: %.2f sec.%n",
                stageLabel, currentMajorStep, checkedCounter, invalidCounter, elapsedSeconds);
    }

    private static void logCompletion(String stageName, long checkedCounter, long invalidCounter, long stageStartTime) {
        long endTime = System.currentTimeMillis();
        double totalTimeSeconds = (endTime - stageStartTime) / 1000.0;
        System.out.printf("--- %s Complete ---%n", stageName);
        System.out.printf("Total checked: %d%n", checkedCounter);
        System.out.printf("Total invalid found: %d%n", invalidCounter);
        System.out.printf("Total time taken: %.2f seconds (%.2f minutes / %.2f hours)%n",
                totalTimeSeconds, totalTimeSeconds / 60.0, totalTimeSeconds / 3600.0);
    }
} 
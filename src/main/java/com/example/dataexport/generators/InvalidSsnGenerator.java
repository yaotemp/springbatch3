package com.example.dataexport.generators;

import com.example.dataexport.util.Provider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class InvalidSsnGenerator {

    private static final String OUTPUT_FILE_PREFIX = "all_invalid_ssns_part_";
    private static final String OUTPUT_FILE_SUFFIX = ".txt";
    private static final long TARGET_COUNT = 250_000_000L;
    private static final int RECORDS_PER_FILE = 10_000_000;
    private static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        System.out.println("Starting generation of 250 million invalid SSNs (all-digit and alpha-numeric). Each file: 10 million records.");

        long invalidSsnCounter = 0;
        long checkedSsnCounter = 0;
        long startTime = System.currentTimeMillis();
        Set<String> seen = new HashSet<>(1_000_000); // Only used for alpha-numeric to avoid duplicates
        int fileIndex = 1;
        int recordInFile = 0;
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(getFileName(fileIndex)));
            // 1. Generate all all-digit invalid SSNs
            outer:
            for (int area = 0; area <= 999; area++) {
                for (int group = 0; group <= 99; group++) {
                    for (int serial = 0; serial <= 9999; serial++) {
                        checkedSsnCounter++;
                        if (Provider.isSsnInvalidAccordingToMd(area, group, serial)) {
                            String ssn = String.format("%03d-%02d-%04d", area, group, serial);
                            writer.write(ssn + "\n");
                            invalidSsnCounter++;
                            recordInFile++;
                            if (invalidSsnCounter % 1_000_000 == 0) {
                                logProgress("All-digit", invalidSsnCounter, startTime, fileIndex);
                            }
                            if (recordInFile >= RECORDS_PER_FILE) {
                                writer.flush();
                                writer.close();
                                fileIndex++;
                                if (invalidSsnCounter < TARGET_COUNT) {
                                    writer = new BufferedWriter(new FileWriter(getFileName(fileIndex)));
                                    recordInFile = 0;
                                } else {
                                    break outer;
                                }
                            }
                            if (invalidSsnCounter >= TARGET_COUNT) {
                                break outer;
                            }
                        }
                    }
                }
            }
            // 2. If not enough, generate random alpha-numeric invalid SSNs
            while (invalidSsnCounter < TARGET_COUNT) {
                char first = UPPERCASE[RANDOM.nextInt(UPPERCASE.length)];
                int d2 = RANDOM.nextInt(10);
                int d3 = RANDOM.nextInt(10);
                int g1 = RANDOM.nextInt(10);
                int g2 = RANDOM.nextInt(10);
                int s1 = RANDOM.nextInt(10);
                int s2 = RANDOM.nextInt(10);
                int s3 = RANDOM.nextInt(10);
                int s4 = RANDOM.nextInt(10);
                String ssn = String.format("%c%1d%1d-%1d%1d-%1d%1d%1d%1d", first, d2, d3, g1, g2, s1, s2, s3, s4);
                if (seen.add(ssn)) {
                    writer.write(ssn + "\n");
                    invalidSsnCounter++;
                    recordInFile++;
                    if (invalidSsnCounter % 1_000_000 == 0) {
                        logProgress("Alpha-numeric", invalidSsnCounter, startTime, fileIndex);
                    }
                    if (recordInFile >= RECORDS_PER_FILE) {
                        writer.flush();
                        writer.close();
                        fileIndex++;
                        if (invalidSsnCounter < TARGET_COUNT) {
                            writer = new BufferedWriter(new FileWriter(getFileName(fileIndex)));
                            recordInFile = 0;
                        } else {
                            break;
                        }
                    }
                }
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            logCompletion(invalidSsnCounter, startTime, fileIndex);
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Output files: " + OUTPUT_FILE_PREFIX + "*" + OUTPUT_FILE_SUFFIX);
    }

    private static String getFileName(int fileIndex) {
        return OUTPUT_FILE_PREFIX + fileIndex + OUTPUT_FILE_SUFFIX;
    }

    private static void logProgress(String stage, long count, long startTime, int fileIndex) {
        long now = System.currentTimeMillis();
        double elapsed = (now - startTime) / 1000.0;
        System.out.printf("[%s] Generated: %d, Elapsed: %.2f sec, File: %d\n", stage, count, elapsed, fileIndex);
    }

    private static void logCompletion(long count, long startTime, int fileIndex) {
        long now = System.currentTimeMillis();
        double elapsed = (now - startTime) / 1000.0;
        System.out.printf("--- Generation Complete ---\nTotal generated: %d\nTotal files: %d\nTotal time: %.2f sec (%.2f min)\n", count, fileIndex, elapsed, elapsed / 60.0);
    }
} 
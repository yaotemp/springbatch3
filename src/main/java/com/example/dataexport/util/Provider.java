package com.example.dataexport.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Provider {

    public static final String INVALID_SSN_TYPE = "INVALID_SSN";
    public static final String SSN_TYPE = "SSN";
    public static final String ITIN_TYPE = "ITIN";
    public static final String EIN_TYPE = "EIN";

    public static final Set<String> VALID_EIN_PREFIXES = new HashSet<>(Arrays.asList(
        "01","02","03","04","05","06","10","11","12","13","14","15","16",
        "20","21","22","23","24","25","26","27","30","31","32","33","34",
        "35","36","37","38","39","40","41","42","43","44","45","46","47",
        "48","50","51","52","53","54","55","56","57","58","59","60","61",
        "62","63","64","65","66","67","68","71","72","73","74","75","76",
        "77","80","81","82","83","84","85","86","87","88","90","91","92",
        "93","94","95","98","99"
    ));

    private final Random random = new Random();

    private int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public String itin() {
        int area = randomInt(900, 999);
        int serial = randomInt(0, 9999);
        
        int group;
        do {
            group = randomInt(70, 99);
        } while (group == 89 || group == 93);

        return String.format("%03d-%02d-%04d", area, group, serial);
    }

    public String ein() {
        List<String> einPrefixChoicesList = new ArrayList<>(VALID_EIN_PREFIXES);
        String einPrefix = einPrefixChoicesList.get(random.nextInt(einPrefixChoicesList.size()));
        int sequence = randomInt(0, 9999999);
        return String.format("%s-%07d", einPrefix, sequence);
    }

    public String invalidSsn() {
        List<Integer> itinGroupNumbers = Arrays.asList(
            70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,
            90,91,92,94,95,96,97,98,99
        );

        int area = randomInt(0, 999);
        int group;
        int serial;

        if (area < 900 && area != 666 && area != 0) {
            int randomGroupOrSerial = randomInt(1, 1000);
            if (randomGroupOrSerial <= 500) {
                group = 0;
                serial = randomInt(0, 9999);
            } else {
                group = randomInt(0, 99);
                serial = 0;
            }
        } else if (area == 666 || area == 0) {
            group = randomInt(0, 99);
            serial = randomInt(0, 9999);
        } else {
            do {
                group = randomInt(0, 99);
            } while (itinGroupNumbers.contains(group));
            serial = randomInt(0, 9999);
        }

        return String.format("%03d-%02d-%04d", area, group, serial);
    }

    public String ssn(String taxpayerIdentificationNumberType) {
        switch (taxpayerIdentificationNumberType) {
            case ITIN_TYPE:
                return itin();
            case EIN_TYPE:
                return ein();
            case INVALID_SSN_TYPE:
                return invalidSsn();
            case SSN_TYPE:
                int area = randomInt(1, 899);
                if (area == 666) area++;
                int group = randomInt(1, 99);
                int serial = randomInt(1, 9999);
                return String.format("%03d-%02d-%04d", area, group, serial);
            default:
                throw new IllegalArgumentException("Invalid type");
        }
    }

    // 测试方法
    public static void main(String[] args) {
        Provider provider = new Provider();
        
        System.out.println("ITIN: " + provider.itin());
        System.out.println("EIN: " + provider.ein());
        System.out.println("Single Random Invalid SSN: " + provider.invalidSsn());
        System.out.println("Valid SSN: " + provider.ssn(SSN_TYPE));

        // Test new validation methods
        System.out.println("\\nTesting new validation methods (based on ssn.md):");
        // Example Tests for isSsnInvalidAccordingToMd
        // Rule 1: Area 000
        System.out.println("000-12-3456 is invalid (Area 000): " + isSsnInvalidAccordingToMd(0, 12, 3456));
        // Rule 2: Area 666
        System.out.println("666-12-3456 is invalid (Area 666): " + isSsnInvalidAccordingToMd(666, 12, 3456));
        // Rule 3: Area 900-999 AND not a valid ITIN
        System.out.println("910-50-1234 is invalid (Area 9xx, non-ITIN group): " + isSsnInvalidAccordingToMd(910, 50, 1234)); // Group 50 is not ITIN
        System.out.println("910-75-1234 is invalid (Valid ITIN should be false): " + isSsnInvalidAccordingToMd(910, 75, 1234)); // Group 75 is ITIN
        // Rule 4: Group number 00
        System.out.println("123-00-3456 is invalid (Group 00): " + isSsnInvalidAccordingToMd(123, 0, 3456));
        // Rule 5: Serial number 0000
        System.out.println("123-45-0000 is invalid (Serial 0000): " + isSsnInvalidAccordingToMd(123, 45, 0));
        // Valid SSN check
        System.out.println("123-45-6789 is invalid (Valid SSN should be false): " + isSsnInvalidAccordingToMd(123, 45, 6789));

        // Test VALID_EIN_PREFIXES
        System.out.println("\\nTesting VALID_EIN_PREFIXES set:");
        System.out.println("Prefix '01' is valid: " + VALID_EIN_PREFIXES.contains("01"));
        System.out.println("Prefix '07' is valid: " + VALID_EIN_PREFIXES.contains("07")); // Should be false
        System.out.println("Prefix '99' is valid: " + VALID_EIN_PREFIXES.contains("99"));
    }

    /**
     * Checks if the given area and group constitute a valid ITIN structure based on ssn.md.
     * Valid ITIN: Area 900-999, Group [70-88, 90-92, 94-99].
     * @param area The area number (0-999).
     * @param group The group number (0-99).
     * @return true if it matches a valid ITIN area/group structure, false otherwise.
     */
    public static boolean isValidItin(int area, int group) {
        if (!(area >= 900 && area <= 999)) {
            return false;
        }
        // Valid ITIN group numbers from ssn.md: 70-99, but excluding 89 and 93.
        // This means: [70-88] OR [90-92] OR [94-99]
        return (group >= 70 && group <= 88) ||
               (group >= 90 && group <= 92) ||
               (group >= 94 && group <= 99);
    }

    /**
     * Checks if an SSN is invalid according to the rules in ssn.md.
     * An SSN is invalid if it meets any of the specified invalid conditions
     * AND it is NOT a valid ITIN.
     * Invalid conditions:
     * - Area number starts with 000.
     * - Area number is 666.
     * - Area number is 900-999.
     * - Group number is 00.
     * - Serial number is 0000.
     * Additional Constraint: An invalid SSN must not be a valid ITIN.
     *
     * @param area The area number (0-999).
     * @param group The group number (0-99).
     * @param serial The serial number (0-9999).
     * @return true if the SSN is invalid according to ssn.md, false otherwise.
     */
    public static boolean isSsnInvalidAccordingToMd(int area, int group, int serial) {
        // "Additional Constraint: An invalid SSN must not be a valid ITIN"
        if (isValidItin(area, group)) {
            // If it's a valid ITIN (based on Area and Group), it's not considered an "invalid SSN"
            // regardless of other conditions like serial 0000 or group 00.
            // ssn.md: "An invalid SSN must not be a valid ITIN (i.e., the area number cannot be 900-999,
            // AND the group number cannot be within the valid range for ITINs)."
            // This implies if area IS 900-999 AND group IS in valid range, it's an ITIN, so not an invalid SSN.
            return false;
        }

        // Now check the explicit invalid conditions from ssn.md
        if (area == 0) return true;                 // Area number starts with 000
        if (area == 666) return true;               // Area number is 666
        // Area 900-999 condition:
        // If it's in this range AND not a valid ITIN (checked above), then it's invalid.
        if (area >= 900 && area <= 999) return true;
        if (group == 0) return true;                // Group number is 00
        if (serial == 0) return true;               // Serial number is 0000

        return false;
    }
}
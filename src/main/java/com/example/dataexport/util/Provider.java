package com.example.dataexport.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Provider {

    public static final String INVALID_SSN_TYPE = "INVALID_SSN";
    public static final String SSN_TYPE = "SSN";
    public static final String ITIN_TYPE = "ITIN";
    public static final String EIN_TYPE = "EIN";

    private final Random random = new Random();

    private int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private int randomElement(List<Integer> list) {
        return list.get(random.nextInt(list.size()));
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
        List<String> einPrefixChoices = Arrays.asList(
            "01","02","03","04","05","06","10","11","12","13","14","15","16",
            "20","21","22","23","24","25","26","27","30","31","32","33","34",
            "35","36","37","38","39","40","41","42","43","44","45","46","47",
            "48","50","51","52","53","54","55","56","57","58","59","60","61",
            "62","63","64","65","66","67","68","71","72","73","74","75","76",
            "77","80","81","82","83","84","85","86","87","88","90","91","92",
            "93","94","95","98","99"
        );
        String einPrefix = einPrefixChoices.get(random.nextInt(einPrefixChoices.size()));
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
        
        // Test ITIN format
        System.out.println("ITIN: " + provider.itin());
        // Test EIN format
        System.out.println("EIN: " + provider.ein());
        // Test invalid SSN
        System.out.println("Invalid SSN: " + provider.invalidSsn());
        // Test valid SSN
        System.out.println("Valid SSN: " + provider.ssn(SSN_TYPE));
    }
}
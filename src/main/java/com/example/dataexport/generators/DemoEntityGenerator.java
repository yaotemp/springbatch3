package com.example.dataexport.generators;

import com.example.dataexport.model.DemoEntity;
import com.github.javafaker.Faker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DemoEntityGenerator {

    private static final Faker faker = new Faker(new Locale("en-US"));
    private static final Provider provider = new Provider();

    public static DemoEntity generateDemoEntity() {
        DemoEntity demoEntity = new DemoEntity();

        demoEntity.setId(faker.number().randomDigitNotZero());
        demoEntity.setEntityType(faker.options().option("I", "B"));

        if ("B".equals(demoEntity.getEntityType())) {
            // Business-specific generation
            String companyName = faker.company().name();
            demoEntity.setLegalName(companyName);
            demoEntity.setFirstName(null);
            demoEntity.setMiddleName(null);
            demoEntity.setLastName(null);
            demoEntity.setBirthdate(generateBusinessFoundingDate());
            demoEntity.setTin(provider.itin());
            demoEntity.setEmail(faker.internet().emailAddress(companyName.replaceAll("\\s+", "").toLowerCase()));
        } else {
            // Individual-specific generation
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            demoEntity.setFirstName(firstName);
            demoEntity.setLastName(lastName);
            demoEntity.setLegalName(firstName + " " + lastName);
            demoEntity.setMiddleName(generateMiddleName());
            demoEntity.setBirthdate(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            demoEntity.setTin(faker.idNumber().ssnValid());
            demoEntity.setEmail(faker.internet().emailAddress());
        }
        demoEntity.setStreetAddress(faker.address().streetAddress());
        demoEntity.setCity(faker.address().city());
        demoEntity.setState(faker.address().stateAbbr());
        demoEntity.setZipCode(faker.address().zipCode());
        demoEntity.setCountry("United States");
        demoEntity.setCreateTs(faker.date().past(365, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());


        return demoEntity;
    }

    public static String generateMiddleName() {
        // 70% chance of having a middle name
        if (faker.number().numberBetween(1, 101) <= 70) {
            return faker.name().firstName(); // Use first name generator for middle names
        }
        return ""; // No middle name
    }

    public static String generatePhoneNumber() {
        // Generate area code (200-999, excluding 555)
        int areaCode;
        do {
            areaCode = faker.number().numberBetween(200, 999);
        } while (areaCode == 555); // Avoid 555 area code
        
        // Generate exchange code (200-999)
        int exchange = faker.number().numberBetween(200, 999);
        
        // Generate last 4 digits
        int lastFour = faker.number().numberBetween(0, 9999);
        
        return String.format("(%03d) %03d-%04d", areaCode, exchange, lastFour);
    }

    public static LocalDate generateBusinessFoundingDate() {
        LocalDate today = LocalDate.now();
        int yearsBack = faker.number().numberBetween(1, 50);
        LocalDate foundingYear = today.minusYears(yearsBack);
        
        // Add randomness to month and day
        int randomDaysBack = faker.number().numberBetween(0, 365);
        return foundingYear.minusDays(randomDaysBack);
    }
}

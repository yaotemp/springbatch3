package com.example.dataexport.util;

import com.example.dataexport.generators.DemoEntityGenerator;
import com.example.dataexport.model.DemoEntity;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

public class Db2InsertExample {

    // --- IMPORTANT: Replace with your actual DB2 connection details ---
    private static final String DB2_URL = "jdbc:db2://localhost:50000/yourdb"; // e.g., "jdbc:db2://yourserver:50000/YOURDB"
    private static final String DB2_USER = "db2admin"; // e.g., "db2inst1"
    private static final String DB2_PASSWORD = "password";

    // --- Optimization Parameters ---
    private static final int TOTAL_RECORDS_TO_INSERT = 100_000; // Total records to generate and insert
    private static final int BATCH_SIZE = 1000;                 // Number of records to send in each batch
    
    private static final String INSERT_SQL = "INSERT INTO DTRBDDBA.DEMO_ENTITIES " +
            "(TIN, ENTITY_TYPE, LEGAL_NAME, FIRST_NAME, MIDDLE_NAME, LAST_NAME, " +
            "EMAIL, PHONE, BIRTHDATE, STREET_ADDRESS, CITY, STATE, ZIP_CODE, COUNTRY) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    /**
     * Inserts a single, hardcoded DemoEntity record for simple testing purposes.
     * This method opens its own connection and handles its own transaction.
     */
    public static void insertSingleTestRecord() {
        System.out.println("--- Starting Single Record Insert Test ---");
        
        // Create a hardcoded entity for the test
        DemoEntity testEntity = new DemoEntity();
        testEntity.setTin("999-99-9999");
        testEntity.setEntityType("I");
        testEntity.setLegalName("Test User");
        testEntity.setFirstName("Test");
        testEntity.setLastName("User");
        testEntity.setEmail("test.user@example.com");
        testEntity.setPhone("1234567890");
        testEntity.setBirthdate(LocalDate.of(2000, 1, 1));
        testEntity.setStreetAddress("1 Test Ave");
        testEntity.setCity("Testville");
        testEntity.setState("TS");
        testEntity.setZipCode("98765");
        testEntity.setCountry("Testland");

        try (Connection conn = DriverManager.getConnection(DB2_URL, DB2_USER, DB2_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            
            System.out.println("Successfully connected for single insert test.");

            pstmt.setString(1, testEntity.getTin());
            pstmt.setString(2, testEntity.getEntityType());
            pstmt.setString(3, testEntity.getLegalName());
            pstmt.setString(4, testEntity.getFirstName());
            pstmt.setNull(5, Types.VARCHAR); // Middle name is null
            pstmt.setString(6, testEntity.getLastName());
            pstmt.setString(7, testEntity.getEmail());
            pstmt.setString(8, testEntity.getPhone());
            pstmt.setDate(9, Date.valueOf(testEntity.getBirthdate()));
            pstmt.setString(10, testEntity.getStreetAddress());
            pstmt.setString(11, testEntity.getCity());
            pstmt.setString(12, testEntity.getState());
            pstmt.setString(13, testEntity.getZipCode());
            pstmt.setString(14, testEntity.getCountry());

            int rowsAffected = pstmt.executeUpdate();
            System.out.println(rowsAffected + " test row(s) inserted successfully.");

        } catch (SQLException e) {
            System.err.println("Error during single record insert test:");
            e.printStackTrace();
        }
        System.out.println("--- Single Record Insert Test Finished ---\n");
    }


    public static void main(String[] args) {
        // First, run the single record insert for testing.
        insertSingleTestRecord();
        
        // Then, proceed with the main bulk insert logic.
        System.out.println("--- Starting Bulk Insert Operation ---");
        long startTime = System.currentTimeMillis();
        int totalRowsAffected = 0;

        // Use try-with-resources for the connection to ensure it's always closed.
        try (Connection conn = DriverManager.getConnection(DB2_URL, DB2_USER, DB2_PASSWORD)) {
            // 1. Disable auto-commit for manual transaction control
            conn.setAutoCommit(false);
            System.out.println("Successfully connected to DB2 for bulk insert. Auto-commit disabled.");

            // Use try-with-resources for the PreparedStatement.
            try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
                System.out.printf("Starting bulk insert of %,d records in batches of %,d...%n", TOTAL_RECORDS_TO_INSERT, BATCH_SIZE);

                for (int i = 1; i <= TOTAL_RECORDS_TO_INSERT; i++) {
                    // Generate a random entity for each iteration
                    DemoEntity entityToInsert = DemoEntityGenerator.generateDemoEntity();

                    // 2. Set parameters for the PreparedStatement
                    pstmt.setString(1, entityToInsert.getTin());
                    pstmt.setString(2, entityToInsert.getEntityType());
                    pstmt.setString(3, entityToInsert.getLegalName());

                    // Handle potential nulls for name fields (for business entities)
                    if (entityToInsert.getFirstName() != null) {
                        pstmt.setString(4, entityToInsert.getFirstName());
                    } else {
                        pstmt.setNull(4, Types.VARCHAR);
                    }
                    if (entityToInsert.getMiddleName() != null) {
                        pstmt.setString(5, entityToInsert.getMiddleName());
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }
                    if (entityToInsert.getLastName() != null) {
                        pstmt.setString(6, entityToInsert.getLastName());
                    } else {
                        pstmt.setNull(6, Types.VARCHAR);
                    }

                    pstmt.setString(7, entityToInsert.getEmail());
                    pstmt.setString(8, entityToInsert.getPhone());
                    pstmt.setDate(9, Date.valueOf(entityToInsert.getBirthdate()));
                    pstmt.setString(10, entityToInsert.getStreetAddress());
                    pstmt.setString(11, entityToInsert.getCity());
                    pstmt.setString(12, entityToInsert.getState());
                    pstmt.setString(13, entityToInsert.getZipCode());
                    pstmt.setString(14, entityToInsert.getCountry());

                    // 3. Add the statement to the current batch
                    pstmt.addBatch();

                    // 4. Execute batch when BATCH_SIZE is reached or on the last record
                    if (i % BATCH_SIZE == 0 || i == TOTAL_RECORDS_TO_INSERT) {
                        int[] batchResult = pstmt.executeBatch();
                        totalRowsAffected += batchResult.length;
                        System.out.printf("Executed batch #%,d. Total records inserted so far: %,d%n", (i / BATCH_SIZE), totalRowsAffected);
                        
                        // 5. Commit the successful batch transaction
                        conn.commit();
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error during batch insert. Rolling back transaction.");
                // 6. Rollback the transaction on error
                conn.rollback();
                throw e; // Re-throw exception after rollback
            }

        } catch (SQLException e) {
            System.err.println("Database connection or transaction failure occurred:");
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.printf("%n--- Bulk Insert Complete ---%n");
        System.out.printf("Successfully inserted %,d records in %.2f seconds.%n", totalRowsAffected, duration);
    }
}

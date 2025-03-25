package com.example.dataexport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
@EnableBatchProcessing
public class DataExportApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataExportApplication.class, args);
    }
} 
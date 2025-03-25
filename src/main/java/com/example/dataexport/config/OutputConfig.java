package com.example.dataexport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Configuration
public class OutputConfig {

    @Value("${app.output.file}")
    private String outputFilePath;
    
    private String cleanedPath;
    
    @PostConstruct
    public void init() {
        // Clean the path string by trimming any whitespace
        this.cleanedPath = outputFilePath.trim();
        
        System.out.println("Output file path: [" + this.cleanedPath + "]");
    }
    
    @Bean
    public FileSystemResource outputResource() {
        // Create a FileSystemResource with the cleaned path
        File outputFile = new File(cleanedPath);
        
        // Log the absolute path for debugging
        System.out.println("Absolute output file path: " + outputFile.getAbsolutePath());
        
        return new FileSystemResource(outputFile);
    }
} 
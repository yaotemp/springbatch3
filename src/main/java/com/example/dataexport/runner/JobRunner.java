package com.example.dataexport.runner;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JobRunner implements CommandLineRunner, ExitCodeGenerator {

    private final JobLauncher jobLauncher;
    private final Job exportUserDataJob;
    private final ApplicationContext applicationContext;
    private int exitCode = 0;

    @Autowired
    public JobRunner(JobLauncher jobLauncher, Job exportUserDataJob, ApplicationContext applicationContext) {
        this.jobLauncher = jobLauncher;
        this.exportUserDataJob = exportUserDataJob;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting export job...");
        
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addDate("startTime", new Date())
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(exportUserDataJob, parameters);
            
            System.out.println("Job Status: " + jobExecution.getStatus());
            System.out.println("Job completed at: " + jobExecution.getEndTime());
            
            // Exit application after job is completed
            if (jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
                System.out.println("Export job completed successfully. Shutting down application...");
                exitCode = 0;
            } else {
                System.out.println("Export job failed with status: " + jobExecution.getExitStatus().getExitCode());
                exitCode = 1;
            }
        } catch (Exception e) {
            System.err.println("Error executing job: " + e.getMessage());
            e.printStackTrace();
            exitCode = 1;
        } finally {
            // Schedule the application to exit
            System.out.println("Scheduling application shutdown...");
            int exitCode = SpringApplication.exit(applicationContext, this);
            System.exit(exitCode);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
} 
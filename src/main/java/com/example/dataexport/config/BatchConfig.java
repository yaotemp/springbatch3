package com.example.dataexport.config;

import com.example.dataexport.model.UserData;
import com.example.dataexport.reader.UserDataReader;
import com.example.dataexport.writer.UserDataLineAggregator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Configuration
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    @Qualifier("appDataSource")
    private DataSource appDataSource;

    @Autowired
    private UserPartitioner partitioner;
    
    @Autowired
    private FileSystemResource outputResource;

    @Value("${app.thread-pool.size:4}")
    private int threadPoolSize;

    @Value("${app.chunk-size:1000}")
    private int chunkSize;

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("spring_batch");
        taskExecutor.setConcurrencyLimit(threadPoolSize);
        return taskExecutor;
    }

    @Bean
    public Job exportUserDataJob() throws Exception {
        return jobBuilderFactory.get("exportUserDataJob")
                .incrementer(new RunIdIncrementer())
                .start(headerWriterStep())      // First write the header
                .next(masterStep())             // Then run the partitioned processing
                .build();
    }
    
    @Bean
    public Step headerWriterStep() {
        return stepBuilderFactory.get("headerWriterStep")
                .tasklet(headerWriterTasklet())
                .build();
    }
    
    @Bean
    public Tasklet headerWriterTasklet() {
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                // Delete the file if it exists
                File file = outputResource.getFile();
                if (file.exists()) {
                    file.delete();
                }
                
                // Create parent directory if it doesn't exist
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Print debug information
                System.out.println("Writing header to: " + file.getAbsolutePath());
                
                // Write the header to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("ret_unique_id,username,email,order_id,order_date,amount,address_id,city,street");
                    writer.newLine();
                }
                
                return RepeatStatus.FINISHED;
            }
        };
    }

    @Bean
    public Step masterStep() throws Exception {
        return stepBuilderFactory.get("masterStep")
                .partitioner("slaveStep", partitioner)
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setGridSize(threadPoolSize);
        handler.setTaskExecutor(taskExecutor());
        handler.setStep(slaveStep());
        return handler;
    }

    @Bean
    public Step slaveStep() throws Exception {
        return stepBuilderFactory.get("slaveStep")
                .<UserData, UserData>chunk(chunkSize)
                .reader(reader(null, null))
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<UserData> reader(
            @Value("#{stepExecutionContext['minValue']}") Integer minValue,
            @Value("#{stepExecutionContext['maxValue']}") Integer maxValue) {
        return new UserDataReader(appDataSource, minValue, maxValue);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<UserData> writer() {
        return new FlatFileItemWriterBuilder<UserData>()
                .name("userDataWriter")
                .resource(outputResource)
                .lineAggregator(new UserDataLineAggregator())
                .append(true)    // This will append without writing header
                .build();
    }
} 
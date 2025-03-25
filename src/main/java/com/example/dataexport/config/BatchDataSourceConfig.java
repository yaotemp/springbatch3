package com.example.dataexport.config;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource appDataSource() {
        return appDataSourceProperties().initializeDataSourceBuilder().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.batch.datasource")
    public DataSourceProperties batchDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Qualifier("batchDataSource")
    @ConfigurationProperties("spring.batch.datasource")
    public DataSource batchDataSource() {
        return batchDataSourceProperties().initializeDataSourceBuilder().build();
    }
    
    @Bean
    public DataSourceInitializer batchDataSourceInitializer(@Qualifier("batchDataSource") DataSource batchDataSource) {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
        databasePopulator.setIgnoreFailedDrops(true);
        
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(batchDataSource);
        initializer.setDatabasePopulator(databasePopulator);
        return initializer;
    }
    
    @Bean
    public BatchConfigurer batchConfigurer(@Qualifier("batchDataSource") DataSource batchDataSource) {
        return new DefaultBatchConfigurer(batchDataSource) {
            @Override
            public PlatformTransactionManager getTransactionManager() {
                return new DataSourceTransactionManager(batchDataSource);
            }
            
            @Override
            protected JobRepository createJobRepository() throws Exception {
                JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
                factory.setDataSource(batchDataSource);
                factory.setTransactionManager(getTransactionManager());
                factory.setDatabaseType("H2");
                factory.afterPropertiesSet();
                return factory.getObject();
            }
        };
    }
} 
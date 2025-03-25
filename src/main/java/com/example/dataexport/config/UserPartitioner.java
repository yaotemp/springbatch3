package com.example.dataexport.config;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserPartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private final int gridSize;

    public UserPartitioner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.gridSize = 4; // Default number of partitions
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // Get min and max ret_unique_id values
        Integer minId = jdbcTemplate.queryForObject(
                "SELECT MIN(ret_unique_id) FROM users", Integer.class);
        Integer maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(ret_unique_id) FROM users", Integer.class);

        if (minId == null || maxId == null) {
            // No data available
            return createSinglePartition();
        }

        // Calculate size of each partition
        int targetSize = Math.max(1, (maxId - minId + 1) / gridSize);
        
        Map<String, ExecutionContext> result = new HashMap<>();
        int partitionCount = 0;
        int currentStart = minId;

        // Create partitions
        while (currentStart <= maxId) {
            int currentEnd = Math.min(maxId, currentStart + targetSize - 1);
            
            ExecutionContext context = new ExecutionContext();
            context.putInt("minValue", currentStart);
            context.putInt("maxValue", currentEnd);
            
            result.put("partition" + partitionCount, context);
            
            currentStart = currentEnd + 1;
            partitionCount++;
        }

        return result;
    }

    private Map<String, ExecutionContext> createSinglePartition() {
        Map<String, ExecutionContext> result = new HashMap<>();
        ExecutionContext context = new ExecutionContext();
        context.putInt("minValue", 0);
        context.putInt("maxValue", 0);
        result.put("partition0", context);
        return result;
    }
} 
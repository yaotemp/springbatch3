# Spring Batch Data Export Application Summary

## Application Structure

The application is a Spring Batch job that exports data from three MySQL tables, merges them, and exports to a CSV file. It uses a partitioning strategy to process data in parallel.

## Key Components

### Model Classes
- `User`: Represents the users table
- `Order`: Represents the orders table
- `Address`: Represents the addresses table
- `UserData`: Combined model for merged data export

### Batch Components
- `UserPartitioner`: Partitions data based on `ret_unique_id` ranges
- `UserDataReader`: Custom reader that loads and merges data for each partition
- `UserDataLineAggregator`: Formats the output for CSV writing

### Configuration
- `BatchConfig`: Sets up the Spring Batch job with partitioning
- `JobRunner`: Triggers the job on application startup

### Data Flow
1. `UserPartitioner` divides the data into partitions based on `ret_unique_id` ranges
2. Each partition runs in parallel using the `TaskExecutorPartitionHandler`
3. `UserDataReader` loads records from all three tables for the partition and merges them in memory
4. `FlatFileItemWriter` writes the merged data to CSV using the `UserDataLineAggregator`

### Performance Considerations
- Avoids database JOIN operations by using parallel in-memory merging
- Processes data in chunks of 1000 records
- Uses multi-threading with configurable thread pool size
- Uses efficient data structures for merging (HashMap-based lookups)

### Configuration Properties
- Database connection settings
- Thread pool size for parallel processing
- Chunk size for batch writing
- Output file location 
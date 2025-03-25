# Data Export Batch Application

A Spring Batch application that exports data from MySQL tables (users, orders, addresses) to a CSV file using a partitioning strategy.

## Features

- Partitioning by `ret_unique_id` ranges for parallel processing
- Concurrent querying of multiple tables to avoid database JOINs
- In-memory data merging
- Chunk-based CSV writing with configurable chunk size

## Prerequisites

- Java 8 or higher
- Maven
- MySQL database with the required tables:
  - `users`
  - `orders`
  - `addresses`

## Configuration

Edit `src/main/resources/application.properties` to configure:

```properties
# Database settings
spring.datasource.url=jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=admin

# Batch settings
app.thread-pool.size=4    # Number of parallel partitions
app.chunk-size=1000       # Items per chunk for writing
app.output.file=output/users_data.csv  # Output file path
```

## Running the Application

1. Build the application:

```
mvn clean package
```

2. Run the application:

```
java -jar target/dataexport-0.0.1-SNAPSHOT.jar
```

The application will automatically:
1. Connect to the MySQL database
2. Partition the data based on `ret_unique_id` ranges
3. Query and merge data from all three tables
4. Write the results to the specified CSV file

## API Endpoints

- `GET /start-export`: Manually trigger the export job

## Output CSV Format

The CSV file will contain the following columns:
- `ret_unique_id` (Users table)
- `username` (Users table)
- `email` (Users table)
- `order_id` (Orders table)
- `order_date` (Orders table)
- `amount` (Orders table)
- `address_id` (Addresses table)
- `city` (Addresses table)
- `street` (Addresses table)

## Error Handling

- The application handles missing related records (orders, addresses) gracefully
- Partition boundaries ensure all data is processed without overlap
- CSV escaping is properly handled for special characters 
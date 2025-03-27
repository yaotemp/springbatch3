package com.example.dataexport.reader;

import com.example.dataexport.mapper.AddressRowMapper;
import com.example.dataexport.mapper.OrderRowMapper;
import com.example.dataexport.mapper.UserRowMapper;
import com.example.dataexport.model.Address;
import com.example.dataexport.model.Order;
import com.example.dataexport.model.User;
import com.example.dataexport.model.UserData;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class UserDataReader implements ItemReader<UserData> {

    private final ItemReader<UserData> delegate;

    public UserDataReader(DataSource dataSource, int minValue, int maxValue) {
        // Load data from all three tables for the given range of ret_unique_id
        List<UserData> userData = loadAndMergeDataConcurrently(dataSource, minValue, maxValue);
        this.delegate = new IteratorItemReader<>(userData);
    }

    @Override
    public UserData read() throws Exception {
        return delegate.read();
    }

    private List<UserData> loadAndMergeDataConcurrently(DataSource dataSource, int minValue, int maxValue) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        ExecutorService executorService = Executors.newFixedThreadPool(3); // Thread pool for 3 tables

        try {
            // First, load users within the partition (this needs to be done first to get retUniqueIds)
            String usersSql = "SELECT * FROM users WHERE ret_unique_id BETWEEN ? AND ?";
            List<User> users = jdbcTemplate.query(usersSql, new UserRowMapper(), minValue, maxValue);

            // If no users found, return empty list
            if (users.isEmpty()) {
                return new ArrayList<>();
            }

            // Extract all ret_unique_ids to use in subsequent queries
            String retUniqueIds = users.stream()
                    .map(user -> String.valueOf(user.getRetUniqueId()))
                    .collect(Collectors.joining(","));

            if (retUniqueIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Create asynchronous tasks for loading orders and addresses concurrently
            CompletableFuture<Map<Integer, Order>> ordersFuture = CompletableFuture.supplyAsync(() -> {
                String ordersSql = "SELECT * FROM orders WHERE ret_unique_id IN (" + retUniqueIds + ")";
                List<Order> orders = jdbcTemplate.query(ordersSql, new OrderRowMapper());
                
                Map<Integer, Order> orderMap = new HashMap<>();
                orders.forEach(order -> orderMap.put(order.getRetUniqueId(), order));
                return orderMap;
            }, executorService);

            CompletableFuture<Map<Integer, Address>> addressesFuture = CompletableFuture.supplyAsync(() -> {
                String addressesSql = "SELECT * FROM addresses WHERE ret_unique_id IN (" + retUniqueIds + ")";
                List<Address> addresses = jdbcTemplate.query(addressesSql, new AddressRowMapper());
                
                Map<Integer, Address> addressMap = new HashMap<>();
                addresses.forEach(address -> addressMap.put(address.getRetUniqueId(), address));
                return addressMap;
            }, executorService);

            // Wait for both futures to complete and get results
            Map<Integer, Order> orderMap = ordersFuture.join();
            Map<Integer, Address> addressMap = addressesFuture.join();

            // Merge data based on ret_unique_id
            return users.stream()
                    .map(user -> {
                        int retUniqueId = user.getRetUniqueId();
                        Order order = orderMap.get(retUniqueId);
                        Address address = addressMap.get(retUniqueId);
                        return UserData.from(user, order, address);
                    })
                    .collect(Collectors.toList());
        } finally {
            // Shutdown the executor service when done
            executorService.shutdown();
        }
    }
} 
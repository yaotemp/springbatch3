package com.example.dataexport.mapper;

import com.example.dataexport.model.Order;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class OrderRowMapper implements RowMapper<Order> {
    @Override
    public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setRetUniqueId(rs.getInt("ret_unique_id"));
        
        // Handle date conversion safely
        java.sql.Date date = rs.getDate("order_date");
        order.setOrderDate(date != null ? date.toLocalDate() : null);
        
        order.setAmount(rs.getBigDecimal("amount"));
        return order;
    }
} 
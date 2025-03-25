package com.example.dataexport.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int orderId;
    private int retUniqueId;
    private LocalDate orderDate;
    private BigDecimal amount;
} 
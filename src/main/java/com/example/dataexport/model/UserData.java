package com.example.dataexport.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData {
    // User data
    private int retUniqueId;
    private String username;
    private String email;
    
    // Order data
    private Integer orderId;
    private LocalDate orderDate;
    private BigDecimal amount;
    
    // Address data
    private Integer addressId;
    private String city;
    private String street;
    
    // Factory method to create from components
    public static UserData from(User user, Order order, Address address) {
        UserData userData = new UserData();
        
        // User data
        userData.setRetUniqueId(user.getRetUniqueId());
        userData.setUsername(user.getUsername());
        userData.setEmail(user.getEmail());
        
        // Order data, may be null
        if (order != null) {
            userData.setOrderId(order.getOrderId());
            userData.setOrderDate(order.getOrderDate());
            userData.setAmount(order.getAmount());
        }
        
        // Address data, may be null
        if (address != null) {
            userData.setAddressId(address.getAddressId());
            userData.setCity(address.getCity());
            userData.setStreet(address.getStreet());
        }
        
        return userData;
    }
} 
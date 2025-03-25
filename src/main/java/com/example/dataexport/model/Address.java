package com.example.dataexport.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private int addressId;
    private int retUniqueId;
    private String city;
    private String street;
} 
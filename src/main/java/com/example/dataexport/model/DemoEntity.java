package com.example.dataexport.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoEntity {

    private Integer id;
    private String legalName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate birthdate;
    private String tin;
    private String entityType;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private LocalDateTime createTs;
}

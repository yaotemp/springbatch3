package com.example.dataexport.writer;

import com.example.dataexport.model.UserData;
import org.springframework.batch.item.file.transform.LineAggregator;

import java.time.format.DateTimeFormatter;

public class UserDataLineAggregator implements LineAggregator<UserData> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DELIMITER = ",";
    private static final String NULL_VALUE = "";

    @Override
    public String aggregate(UserData userData) {
        StringBuilder sb = new StringBuilder();
        
        // User data
        sb.append(userData.getRetUniqueId()).append(DELIMITER)
          .append(escapeField(userData.getUsername())).append(DELIMITER)
          .append(escapeField(userData.getEmail())).append(DELIMITER);
        
        // Order data
        sb.append(userData.getOrderId() != null ? userData.getOrderId() : NULL_VALUE).append(DELIMITER)
          .append(userData.getOrderDate() != null ? userData.getOrderDate().format(DATE_FORMATTER) : NULL_VALUE).append(DELIMITER)
          .append(userData.getAmount() != null ? userData.getAmount() : NULL_VALUE).append(DELIMITER);
        
        // Address data
        sb.append(userData.getAddressId() != null ? userData.getAddressId() : NULL_VALUE).append(DELIMITER)
          .append(escapeField(userData.getCity())).append(DELIMITER)
          .append(escapeField(userData.getStreet()));
        
        return sb.toString();
    }
    
    private String escapeField(String field) {
        if (field == null) {
            return NULL_VALUE;
        }
        
        // If field contains delimiter, newline or double quote, wrap in quotes and escape double quotes
        if (field.contains(DELIMITER) || field.contains("\n") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
} 
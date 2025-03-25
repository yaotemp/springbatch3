package com.example.dataexport.mapper;

import com.example.dataexport.model.Address;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddressRowMapper implements RowMapper<Address> {
    @Override
    public Address mapRow(ResultSet rs, int rowNum) throws SQLException {
        Address address = new Address();
        address.setAddressId(rs.getInt("address_id"));
        address.setRetUniqueId(rs.getInt("ret_unique_id"));
        address.setCity(rs.getString("city"));
        address.setStreet(rs.getString("street"));
        return address;
    }
} 
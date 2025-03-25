package com.example.dataexport.mapper;

import com.example.dataexport.model.User;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setRetUniqueId(rs.getInt("ret_unique_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        return user;
    }
} 
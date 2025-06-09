package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean register(String email, String password, String name) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);

        if (count != null && count > 0) {
            return false;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

        jdbcTemplate.update("INSERT INTO member(email, password, name) VALUES (?, ?, ?)",
        		email, hashed, name);

        return true;
    }

    public boolean authenticate(String email, String password) {
        try {
            String hashed = jdbcTemplate.queryForObject(
                    "SELECT password FROM member WHERE email = ?", String.class, email);

            return BCrypt.checkpw(password, hashed);
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getNameByUsername(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT name FROM member WHERE email = ?", String.class, email);
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean deleteUserByEmail(String email) {
        int rows = jdbcTemplate.update("DELETE FROM member WHERE email = ?", email);
        return rows > 0;
    }
    
    public void updatePassword(String email, String newPassword) {
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        jdbcTemplate.update("UPDATE member SET password = ? WHERE email = ?", hashed, email);
    }
    
}

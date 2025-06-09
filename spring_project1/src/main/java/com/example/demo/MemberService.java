package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt; // 確保有這個 import
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Project1 原有的註冊方法，將被新的驗證碼註冊流程取代，但可保留用於其他情況
    public boolean register(String email, String password, String name) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);

        if (count != null && count > 0) {
            return false; // Email 已存在
        }

        // 密碼雜湊
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        jdbcTemplate.update("INSERT INTO member(email, password, name) VALUES (?, ?, ?)",
                email, hashedPassword, name);

        return true;
    }

    // 新增方法：處理驗證碼註冊流程中最終的用戶註冊
    public boolean registerUserAfterVerification(String email, String password, String name) {
        // 在這裡不需要再次檢查 email 是否存在，因為 VerificationService.generateAndSendCode
        // 已經檢查過，並且驗證碼流程確保了新用戶。
        // 但是，為了一致性，我們仍然可以檢查一次，或者簡化直接插入。
        // 這裡選擇直接插入，因為假定調用者已完成驗證碼和 email 唯一性檢查。

        // 密碼雜湊
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        int rowsAffected = jdbcTemplate.update("INSERT INTO member(email, password, name) VALUES (?, ?, ?)",
                email, hashedPassword, name);

        return rowsAffected > 0;
    }

    public boolean authenticate(String email, String password) {
        try {
            // 從數據庫獲取雜湊後的密碼
            String hashedPassword = jdbcTemplate.queryForObject(
                    "SELECT password FROM member WHERE email = ?", String.class, email);

            // 處理 Google 登入的特殊情況 (密碼為 "google")
            if ("google".equals(hashedPassword)) {
                // 如果是 Google 帳號，直接返回 true，因為其登入由 Google 驗證
                return true;
            }

            // 驗證輸入的密碼是否與雜湊後的密碼匹配
            return BCrypt.checkpw(password, hashedPassword);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 沒有找到該 email 的用戶
            return false;
        } catch (Exception e) {
            // 其他異常，如多個結果等
            e.printStackTrace();
            return false;
        }
    }

    public String getNameByUsername(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT name FROM member WHERE email = ?", String.class, email);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean deleteUserByEmail(String email) {
        int rows = jdbcTemplate.update("DELETE FROM member WHERE email = ?", email);
        return rows > 0;
    }

    public void updatePassword(String email, String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        jdbcTemplate.update("UPDATE member SET password = ? WHERE email = ?", hashedPassword, email);
    }
}
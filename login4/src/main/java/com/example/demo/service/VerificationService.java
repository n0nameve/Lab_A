package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class VerificationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailService emailService;

    public void generateAndSendCode(String email) {
        // 檢查 email 是否已存在於 member table
        Integer existingUserCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);
        if (existingUserCount != null && existingUserCount > 0) {
            // 如果 email 已註冊，可以選擇拋出異常或不發送驗證碼
            // 為了簡潔，這裡直接返回，您可以根據需求處理
            System.out.println("Email: " + email + " 已經存在於 member table，不發送驗證碼。");
            return;
        }

        String code = String.valueOf(new Random().nextInt(900000) + 100000); // 確保是六位數
        LocalDateTime now = LocalDateTime.now();

        // 使用 ON DUPLICATE KEY UPDATE 來更新已存在的驗證碼，如果不存在則插入
        jdbcTemplate.update(
            "INSERT INTO verification_code (email, code, created_at) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE code = ?, created_at = ?",
            email, code, Timestamp.valueOf(now), code, Timestamp.valueOf(now)
        );

        emailService.sendVerificationCode(email, code);
    }

    public boolean verifyCode(String email, String inputCode) {
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT code, created_at FROM verification_code WHERE email = ?", email);

            if (result != null) {
                String code = (String) result.get("code");
                // 從數據庫讀取 Timestamp 並轉換為 LocalDateTime
                LocalDateTime createdAt = (LocalDateTime) result.get("created_at");
                LocalDateTime now = LocalDateTime.now();
                
                System.out.println("資料庫中的 code: " + code);
                System.out.println("輸入的 code: " + inputCode);
                System.out.println("建立時間: " + createdAt);
                System.out.println("現在時間: " + now);
                System.out.println("時間差（分鐘）: " + Duration.between(createdAt, now).toMinutes());

                // 檢查驗證碼是否匹配且在 5 分鐘內有效
                return code.equals(inputCode) && Duration.between(createdAt, now).toMinutes() < 5;
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 沒有找到該 email 的驗證碼記錄
            System.out.println("Error: No verification code found for email: " + email);
        } catch (Exception e) {
            System.err.println("驗證碼驗證錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 新增方法：驗證碼使用後刪除，或者過期自動刪除
    public void deleteVerificationCode(String email) {
        jdbcTemplate.update("DELETE FROM verification_code WHERE email = ?", email);
    }
}
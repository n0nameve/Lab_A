package com.example.demo;

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
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        LocalDateTime now = LocalDateTime.now();

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
                LocalDateTime createdAt = (LocalDateTime) result.get("created_at");
                LocalDateTime now = LocalDateTime.now();
                
                System.out.println("資料庫中的 code: " + code);
                System.out.println("輸入的 code: " + inputCode);
                System.out.println("建立時間: " + createdAt);
                System.out.println("現在時間: " + now);
                System.out.println("時間差（分鐘）: " + Duration.between(createdAt, now).toMinutes());

                return code.equals(inputCode) && Duration.between(createdAt, now).toMinutes() < 5;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }
}
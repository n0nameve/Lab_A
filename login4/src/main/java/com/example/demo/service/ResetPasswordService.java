package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger; // 導入日誌類
import org.slf4j.LoggerFactory; // 導入日誌工廠

@Service
public class ResetPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordService.class); // 初始化日誌器

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MemberService memberService; // 用於檢查email是否存在和更新密碼

    private static final long EXPIRY_HOURS = 1; // Token 有效期為 1 小時

    /**
     * 生成並發送重設密碼連結。
     * @param email 用戶的電子郵件。
     * @param baseUrl 應用程式的基礎 URL (例如: http://localhost:8081)。
     * @return 如果成功發送連結，則為 true；否則為 false。
     */
    public boolean generateAndSendResetToken(String email, String baseUrl) {
        // 1. 檢查 Email 是否存在於 member table
        if (memberService.getNameByUsername(email) == null) {
            logger.warn("Attempted to generate reset token for non-existent email: {}", email); // 使用日誌
            return false; // Email 不存在
        }

        // 2. 生成唯一的 Token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(EXPIRY_HOURS);

        // 3. 將 Token 存入 reset_password_tokens 資料表
        // 使用 ON DUPLICATE KEY UPDATE 以處理 Email 已存在的情況
        jdbcTemplate.update(
            "INSERT INTO reset_password_tokens (email, token, expiry_date) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE token = ?, expiry_date = ?",
            email, token, Timestamp.valueOf(expiryDate), token, Timestamp.valueOf(expiryDate)
        );
        logger.info("Generated reset token for email: {}, token: {}", email, token); // 使用日誌

        // 4. 生成重設密碼連結
        String resetLink = baseUrl + "/reset-password?token=" + token;

        // 5. 發送重設密碼郵件
        try {
            emailService.sendResetPasswordEmail(email, resetLink);
            logger.info("Sent reset password email to: {} with link: {}", email, resetLink); // 使用日誌
            return true;
        } catch (Exception e) {
            logger.error("Failed to send reset password email to {}: {}", email, e.getMessage(), e); // 使用日誌
            return false;
        }
    }

    /**
     * 驗證重設密碼 token 的有效性。
     * @param token 要驗證的 token。
     * @return 如果 token 有效且未過期，則返回該 token 相關聯的 email；否則返回 null。
     */
    public String validateToken(String token) {
        try {
            // 從資料庫查詢 token 和 expiry_date
            Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT email, expiry_date FROM reset_password_tokens WHERE token = ?", token);

            if (result == null || result.isEmpty()) { // 檢查查詢結果是否為空
                logger.warn("No reset token found for token: {}. Result map is null or empty.", token); 
                return null;
            }

            String email = (String) result.get("email");
            
            // 從 Map 中獲取資料庫欄位 "expiry_date" 的值，注意這裡必須是資料庫的實際欄位名稱
            Object expiryDateObj = result.get("expiry_date"); 
            LocalDateTime expiryDate = null; // 初始化為 null

            // 安全地檢查並轉換 expiry_dateObj 到 LocalDateTime
            if (expiryDateObj == null) {
                // 如果 expiry_date 欄位在資料庫中為 NULL
                logger.warn("Expiry date for token {} (email: {}) is NULL in database. Token invalid.", token, email);
                return null; // expiry_date 為 null，直接視為無效
            } else if (expiryDateObj instanceof Timestamp) {
                // 如果資料庫返回的是 Timestamp 類型，轉換為 LocalDateTime
                expiryDate = ((Timestamp) expiryDateObj).toLocalDateTime();
            } else if (expiryDateObj instanceof LocalDateTime) {
                // 如果資料庫直接返回 LocalDateTime 類型 (新版 JDBC 驅動常見)
                expiryDate = (LocalDateTime) expiryDateObj;
            } else {
                // 處理所有其他意外的資料類型
                logger.error("Unexpected type for expiry_date: {} for token {} (email: {}). Expected Timestamp or LocalDateTime.", expiryDateObj.getClass().getName(), token, email);
                return null;
            }
            
            // 在進行日期比較之前，再次確認 expiryDate 是否為 null。
            // 儘管上面的邏輯盡力確保了不為 null，但這是額外的安全檢查。
            if (expiryDate == null) {
                logger.warn("Failed to convert expiry date object to LocalDateTime for token {} (email: {}). Resulting LocalDateTime is null. Token invalid.", token, email);
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            
            // 這條日誌會顯示解析後的 expiryDate，它應該不再是 null
            logger.info("Token found for email: {}, Expiry Date: {}, Current Time: {}", email, expiryDate, now);

            // 檢查 token 是否過期 (現在時間是否在過期時間之前)
            // 這是您發生 NullPointerException 的那一行
            if (now.isBefore(expiryDate)) {
                logger.info("Token {} for email {} is valid and not expired.", token, email);
                return email; // Token 有效
            } else {
                logger.warn("Token expired for email: {}, token: {}. Current time: {}, Expiry Date: {}", email, token, now, expiryDate);
                return null; // Token 已過期
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            logger.warn("No reset token found for token: {}. Data access exception.", token);
        } catch (Exception e) {
            logger.error("Unhandled error validating reset token {}: {}", token, e.getMessage(), e); // 捕獲其他未知錯誤
            // 為了方便排錯，仍然打印堆棧追蹤，但在生產環境中可能會考慮更精簡的日誌
            e.printStackTrace(); 
        }
        return null; // Token 無效或已過期
    }

    /**
     * 重設用戶密碼。
     * @param token 用於驗證的 token。
     * @param newPassword 新密碼。
     * @return 如果密碼成功重設，則為 true；否則為 false。
     */
    public boolean resetPassword(String token, String newPassword) {
        String email = validateToken(token); // 驗證 token 並取得 email

        if (email != null) {
            try {
                memberService.updatePassword(email, newPassword); // 更新密碼
                deleteResetToken(token); // 密碼更新成功後刪除 token
                logger.info("Password successfully reset for email: {}", email);
                return true;
            } catch (Exception e) {
                logger.error("Error resetting password for email {} with token {}: {}", email, token, e.getMessage(), e);
                e.printStackTrace();
                return false;
            }
        }
        logger.warn("Failed to reset password: Invalid or expired token was used.");
        return false;
    }

    /**
     * 刪除已使用的重設密碼 token。
     * @param token 要刪除的 token。
     */
    public void deleteResetToken(String token) {
        jdbcTemplate.update("DELETE FROM reset_password_tokens WHERE token = ?", token);
        logger.info("Deleted reset token: {}", token);
    }
}

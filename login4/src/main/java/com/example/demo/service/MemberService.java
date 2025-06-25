package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ThemeService themeService;
    @Autowired
    private UserThemeService userThemeService;

    // Project1 原有的註冊方法，將被新的驗證碼註冊流程取代，但可保留用於其他情況
    @Transactional
    public boolean register(String email, String password, String username) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);

        if (count != null && count > 0) {
            return false; // Email 已存在
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        jdbcTemplate.update("INSERT INTO member(email, password, username, coin, current_theme,) VALUES (?, ?, ?, ?, ?, ?)",
                email, hashedPassword, username, 100L, "default", false); // <--- 這裡設置為 false

        Long userId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
        Optional<Theme> defaultThemeOptional = themeService.findByThemeName("default");

        if (userId != null && defaultThemeOptional.isPresent()) {
            Theme defaultTheme = defaultThemeOptional.get();
            userThemeService.addUserTheme(userId, defaultTheme.getId());
        } else {
            System.err.println("Error: Default theme not found or user ID could not be retrieved during registration for email: " + email);
            throw new RuntimeException("Registration failed: Could not assign default theme or retrieve user ID.");
        }

        return true;
    }

    @Transactional
    public boolean registerUserAfterVerification(String email, String password, String username) {
        if (findUserByEmail(email).isPresent()) {
            return false; // Email 已存在
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        int rowsAffected = jdbcTemplate.update("INSERT INTO member(email, password, username, coin, current_theme,) VALUES (?, ?, ?, ?, ?, ?)",
                email, hashedPassword, username, 100L, "default", false); // <--- 這裡設置為 false

        if (rowsAffected > 0) {
            Long userId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
            Optional<Theme> defaultThemeOptional = themeService.findByThemeName("default");

            if (userId != null && defaultThemeOptional.isPresent()) {
                Theme defaultTheme = defaultThemeOptional.get();
                userThemeService.addUserTheme(userId, defaultTheme.getId());
                return true;
            } else {
                System.err.println("Error: Default theme not found or user ID could not be retrieved during verification registration for email: " + email);
                throw new RuntimeException("Verification registration failed: Could not assign default theme or retrieve user ID.");
            }
        }
        return false;
    }

    // 新增或更新 Google 登入用戶的方法
    @Transactional
    public User saveGoogleUser(String email, String username) {
        Optional<User> existingUserOptional = findUserByEmail(email);

        User user;
        if (existingUserOptional.isPresent()) {
            user = existingUserOptional.get();
            // 可以更新 name，如果 Google 傳回的名字更為準確
            if (!user.getUsername().equals(username)) { // 避免不必要的更新
                 jdbcTemplate.update("UPDATE member SET username = ? WHERE id = ?", username, user.getId());
                 user.setUsername(username);
            }
            System.out.println("🔁 Existing user logged in via Google: " + email);
        } else {
            // 新用戶 → 自動註冊
            user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(null); // Google 登入的用戶，本地沒有密碼，設置為 null
            user.setCoin(100L); // 預設金幣
            user.setCurrentTheme("default"); // 預設主題

            // 使用 JdbcTemplate 插入數據，並獲取生成的主鍵 ID
            // 注意：這裡假設你的 member 表 ID 是自增的
            jdbcTemplate.update("INSERT INTO member(email, password, username, coin, current_theme,) VALUES (?, ?, ?, ?, ?, ?)",
                    user.getEmail(), user.getPassword(), user.getUsername(), user.getCoin(), user.getCurrentTheme());

            // 獲取剛剛插入的用戶 ID
            Long userId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
            user.setId(userId); // 設定 User 物件的 ID

            // 為新用戶新增 'default' 主題的購買記錄
            Optional<Theme> defaultThemeOptional = themeService.findByThemeName("default");
            if (userId != null && defaultThemeOptional.isPresent()) {
                Theme defaultTheme = defaultThemeOptional.get();
                userThemeService.addUserTheme(userId, defaultTheme.getId());
            } else {
                System.err.println("Error: Default theme not found or user ID could not be retrieved during Google registration for email: " + email);
                throw new RuntimeException("Google registration failed: Could not assign default theme or retrieve user ID.");
            }
            System.out.println("✅ New Google user registered: " + email);
        }
        return user;
    }


    public boolean authenticate(String email, String password) {
        try {
            // 首先嘗試獲取整個 User 對象，以便檢查 googleConnected 狀態
            Optional<User> userOptional = findUserByEmail(email);

            if (userOptional.isEmpty()) {
                return false; // Email 不存在
            }

            User user = userOptional.get();

            // 對於非 Google 連接的帳戶，進行 BCrypt 密碼比對
            return BCrypt.checkpw(password, user.getPassword());

        } catch (Exception e) {
            System.err.println("Authentication error for email: " + email + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public String getNameByUsername(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT username FROM member WHERE email = ?", String.class, email);
        } catch (EmptyResultDataAccessException e) {
            return null; // Email 不存在
        } catch (Exception e) {
            System.err.println("Error getting username for email: " + email + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // --- 修改：根據 Email 獲取完整的 UserEntity ---
    // 更改了欄位名稱 'theme' 為 'current_theme' 以匹配資料庫和 User 實體
    public Optional<User> findUserByEmail(String email) {
        String sql = "SELECT id, email, password, username, coin, current_theme FROM member WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty(); // 用戶不存在
        } catch (Exception e) {
            System.err.println("Error finding user by email: " + email + " - " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // 自定義 RowMapper 來映射 ResultSet 到 User 對象
    private static final class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User userEntity = new User();
            userEntity.setId(rs.getLong("id"));
            userEntity.setEmail(rs.getString("email"));
            userEntity.setPassword(rs.getString("password")); // 允許為 null
            userEntity.setUsername(rs.getString("username"));
            userEntity.setCoin(rs.getLong("coin"));
            userEntity.setCurrentTheme(rs.getString("current_theme"));
            return userEntity;
        }
    }


    // --- 新增方法：更新用戶的金幣 ---
    @Transactional
    public void updateUserCoin(User user) {
        String sql = "UPDATE member SET coin = ? WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, user.getCoin(), user.getId());
        if (rowsAffected == 0) {
            System.err.println("Failed to update coin for user ID: " + user.getId() + ". User not found.");
        }
    }

    // --- 修改：更新用戶的佈景主題 ---
    @Transactional
    public void updateThemeByEmail(String email, String newTheme) {
        String sql = "UPDATE member SET current_theme = ? WHERE email = ?";
        int rowsAffected = jdbcTemplate.update(sql, newTheme, email);
        if (rowsAffected == 0) {
            System.err.println("Failed to update current_theme for user: " + email + ". User not found or no change.");
        }
    }

    @Transactional
    public boolean deleteUserByEmail(String email) {
        Optional<User> userOptional = findUserByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            jdbcTemplate.update("DELETE FROM user_themes WHERE user_id = ?", user.getId());
            int rows = jdbcTemplate.update("DELETE FROM member WHERE email = ?", email);
            return rows > 0;
        }
        return false;
    }

    public void updatePassword(String email, String newPassword) {
        int rowsAffected = jdbcTemplate.update("UPDATE member SET password = ? WHERE email = ? , hashedPassword, email");
        if (rowsAffected == 0) {
             System.err.println("Failed to update password for user: " + email + ". User not found or is Google connected.");
        }
    }
}
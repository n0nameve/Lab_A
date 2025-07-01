package com.example.demo.service;

import com.example.demo.entity.UserTheme;
// 已經不需要在這裡引入 User 實體或 Theme 實體，因為它們是在 RowMapper 內部創建的輕量級物件
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException; // 引入此異常，以防萬一

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class UserThemeService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 您已經正確地移除了 MemberService 和 ThemeService 的循環依賴注入，保持這個狀態。

    // 自定義 RowMapper 來映射 ResultSet 到 UserTheme 對象
    // 這個 RowMapper 只處理 user_themes 表自身的欄位，以及 JOIN 過來的 theme 資訊
    private class UserThemeRowMapper implements RowMapper<UserTheme> {
        @Override
        public UserTheme mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserTheme userTheme = new UserTheme();
            userTheme.setId(rs.getLong("id"));
            userTheme.setPurchasedAt(rs.getTimestamp("purchased_at"));

            // 創建並設定 User 物件的 ID
            com.example.demo.entity.User user = new com.example.demo.entity.User();
            user.setId(rs.getLong("user_id"));
            userTheme.setUser(user);

            // 創建並設定 Theme 物件的屬性 (從 JOIN 的結果中獲取)
            com.example.demo.entity.Theme theme = new com.example.demo.entity.Theme();
            theme.setId(rs.getLong("theme_id")); // 從 JOIN 中獲取 theme_id
            theme.setThemeName(rs.getString("theme_name"));
            theme.setDisplayName(rs.getString("display_name"));
            theme.setPrice(rs.getInt("price"));
            userTheme.setTheme(theme);

            return userTheme;
        }
    }

    /**
     * 獲取用戶所有已購買的主題記錄，包含主題的詳細資訊。
     * @param userId 用戶 ID
     * @return 包含 UserTheme 對象的列表
     */
    public List<UserTheme> getUserPurchasedThemes(Long userId) {
        String sql = "SELECT ut.id, ut.user_id, ut.theme_id, ut.purchased_at, t.theme_name, t.display_name, t.price " +
                     "FROM user_themes ut JOIN themes t ON ut.theme_id = t.id WHERE ut.user_id = ?";
        // 修改 deprecated 的 query 寫法：將參數直接放在 RowMapper 之後
        return jdbcTemplate.query(sql, new UserThemeRowMapper(), userId);
    }

    /**
     * 檢查用戶是否已購買某個主題。
     * @param userId 用戶 ID
     * @param themeName 主題名稱
     * @return 如果已購買則返回 true，否則 false
     */
    public boolean hasUserPurchasedTheme(Long userId, String themeName) {
        String sql = "SELECT COUNT(*) FROM user_themes ut " +
                     "JOIN themes t ON ut.theme_id = t.id " +
                     "WHERE ut.user_id = ? AND t.theme_name = ?";
        try {
            // 修改 deprecated 的 queryForObject 寫法：將參數直接放在 Integer.class 之後
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, themeName);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            // 如果查詢結果為空，表示沒有匹配的記錄，因此返回 false
            return false;
        }
    }

    /**
     * 為用戶添加一條主題購買記錄。
     * @param userId 用戶 ID
     * @param themeId 主題 ID
     */
    public void addUserTheme(Long userId, Long themeId) {
        String sql = "INSERT INTO user_themes (user_id, theme_id, purchased_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, themeId, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * 獲取用戶所有已購買的主題名稱列表。
     * 這通常用於前端下拉選單或簡潔列表。
     * @param userId 用戶 ID
     * @return 已購買主題名稱的列表
     */
    public List<String> getPurchasedThemeNamesByUserId(Long userId) {
        String sql = "SELECT t.theme_name FROM user_themes ut " +
                     "JOIN themes t ON ut.theme_id = t.id WHERE ut.user_id = ?";
        // queryForList 方法本身就支持變數參數，所以這裡的寫法是正確且推薦的
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    /**
     * 根據用戶 ID 刪除該用戶的所有主題購買記錄。
     * 這通常在刪除用戶帳號時被調用。
     * @param userId 用戶 ID
     */
    public void deleteUserThemesByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM user_themes WHERE user_id = ?", userId);
    }
}
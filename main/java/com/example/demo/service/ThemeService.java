package com.example.demo.service;

import com.example.demo.entity.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class ThemeService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 自定義 RowMapper 來映射 ResultSet 到 Theme 對象
    private static final class ThemeRowMapper implements RowMapper<Theme> {
        @Override
        public Theme mapRow(ResultSet rs, int rowNum) throws SQLException {
            Theme theme = new Theme();
            theme.setId(rs.getLong("id"));
            theme.setThemeName(rs.getString("theme_name"));
            theme.setDisplayName(rs.getString("display_name"));
            theme.setPrice(rs.getInt("price"));
            return theme;
        }
    }

    public List<Theme> getAllThemes() {
        String sql = "SELECT id, theme_name, display_name, price FROM themes";
        return jdbcTemplate.query(sql, new ThemeRowMapper());
    }

    public Optional<Theme> findByThemeName(String themeName) {
        String sql = "SELECT id, theme_name, display_name, price FROM themes WHERE theme_name = ?";
        try {
            // 推薦的寫法：將參數直接放在 RowMapper 之後
            Theme theme = jdbcTemplate.queryForObject(sql, new ThemeRowMapper(), themeName);
            return Optional.ofNullable(theme);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Theme> findById(Long id) {
        String sql = "SELECT id, theme_name, display_name, price FROM themes WHERE id = ?";
        try {
            // 推薦的寫法：將參數直接放在 RowMapper 之後
            Theme theme = jdbcTemplate.queryForObject(sql, new ThemeRowMapper(), id);
            return Optional.ofNullable(theme);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 可以在這裡新增方法來插入預設主題，以防資料庫中沒有
    public void createDefaultThemeIfNotExists() {
        if (findByThemeName("default").isEmpty()) {
            String sql = "INSERT INTO themes (theme_name, display_name, price) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, "default", "預設主題", 0);
            System.out.println("Default theme 'default' inserted into themes table.");
        }
    }
}
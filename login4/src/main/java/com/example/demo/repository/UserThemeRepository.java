package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.UserTheme;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserThemeRepository extends JpaRepository<UserTheme, Long> {
    List<UserTheme> findByUser_Id(Long userId); // 找到某用戶所有已購買的主題
    Optional<UserTheme> findByUser_IdAndTheme_Id(Long userId, Long themeId); // 檢查用戶是否已購買某主題
    boolean existsByUser_IdAndTheme_ThemeName(Long userId, String themeName); // 檢查用戶是否已購買某主題名稱
}
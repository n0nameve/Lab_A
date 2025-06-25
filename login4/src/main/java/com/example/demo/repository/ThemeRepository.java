package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Theme;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Theme findByThemeName(String themeName);
}
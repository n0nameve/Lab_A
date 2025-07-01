package com.example.demo.entity;

import jakarta.persistence.*; // 確保引入所有需要的 JPA 註解

@Entity // <--- 新增這個註解，告訴 JPA 這是一個實體
@Table(name = "themes") // <--- 新增這個註解，指定對應的資料庫表格名稱
public class Theme {

    @Id // <--- 標記為主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <--- 設定主鍵生成策略為資料庫自動增長
    private Long id;

    @Column(nullable = false, unique = true) // <--- 確保 theme_name 是唯一的且非空
    private String themeName; // 例如: "dark", "blue", "default"

    @Column(nullable = false)
    private String displayName; // 例如: "深色模式", "海洋藍", "預設主題" (用於 UI 顯示)

    @Column(nullable = false)
    private Integer price; // 主題價格，單位為金幣

    // 無參數建構子 (JPA 需要)
    public Theme() {
    }

    // 全參數建構子 (可選，方便初始化)
    public Theme(Long id, String themeName, String displayName, Integer price) {
        this.id = id;
        this.themeName = themeName;
        this.displayName = displayName;
        this.price = price;
    }

    // --- Getter & Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
}
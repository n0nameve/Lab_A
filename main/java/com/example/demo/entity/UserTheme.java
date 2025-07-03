package com.example.demo.entity;

import jakarta.persistence.*; // 確保引入所有需要的 JPA 註解
import java.sql.Timestamp; // 用於 purchasedAt

@Entity // <--- 新增這個註解
@Table(name = "user_themes") // <--- 新增這個註解，對應資料庫表格名稱
public class UserTheme {

    @Id // <--- 標記為主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // <--- 設定主鍵生成策略
    private Long id;

    // 定義與 User 實體的關聯
    @ManyToOne // 一個 User 可以有多個 UserTheme 記錄，一個 UserTheme 記錄只屬於一個 User
    @JoinColumn(name = "user_id", nullable = false) // <--- 設置外鍵欄位名稱，並標記為非空
    private User user;

    // 定義與 Theme 實體的關聯
    @ManyToOne // 一個 Theme 可以被多個 UserTheme 記錄引用，一個 UserTheme 記錄只關聯一個 Theme
    @JoinColumn(name = "theme_id", nullable = false) // <--- 設置外鍵欄位名稱，並標記為非空
    private Theme theme;

    @Column(name = "purchased_at", nullable = false) // <--- 購買時間欄位
    private Timestamp purchasedAt;

    // 無參數建構子 (JPA 需要)
    public UserTheme() {
    }

    // 全參數建構子 (可選，方便初始化)
    public UserTheme(Long id, User user, Theme theme, Timestamp purchasedAt) {
        this.id = id;
        this.user = user;
        this.theme = theme;
        this.purchasedAt = purchasedAt;
    }

    // --- Getter & Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public Timestamp getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(Timestamp purchasedAt) {
        this.purchasedAt = purchasedAt;
    }
}
package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class LoginController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, Model model) {
        String sql = "SELECT COUNT(*) FROM member WHERE email = ? AND password = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, password);

        if (count != null && count > 0) {
            return "redirect:/welcome"; // 登入成功後跳轉
        } else {
            model.addAttribute("error", "Invalid credentials.");
            return "login"; // 回到登入頁顯示錯誤
        }
    }
}
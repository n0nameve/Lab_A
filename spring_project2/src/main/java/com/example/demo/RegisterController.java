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

import jakarta.servlet.http.HttpSession;

@Controller
public class RegisterController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/register/request-code")
    public String requestCode(@RequestParam String email, HttpSession session) {
        verificationService.generateAndSendCode(email);
        session.setAttribute("email", email);
        return "redirect:/verify";
    }

    @PostMapping("/register/verify")
    public String verifyCodeAndRegister(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String password,
            @RequestParam String name,
            Model model) {
    	
    	 if (verificationService.verifyCode(email, code)) {
    	        jdbcTemplate.update("INSERT INTO member (email, password, name) VALUES (?, ?, ?)", email, password, name);
    	        return "redirect:/login";  // 核心：轉向 login.html
    	    } else {
    	        model.addAttribute("error", "Invalid or expired code.");
    	        model.addAttribute("email", email); 
    	        return "verify"; // 回到驗證頁顯示錯誤
    	    }
    	}

}
package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

	@GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @GetMapping("/verify")
    public String showVerifyPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        model.addAttribute("email", email); // 帶入 email 給 verify.html
        return "verify";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/welcome")
    public String showWelcomePage() {
        return "welcome";
    }
}


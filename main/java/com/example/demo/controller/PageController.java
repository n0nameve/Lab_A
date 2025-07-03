package com.example.demo.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/main")
    public String mainPage() {
        return "main";  // Thymeleaf 会渲染 templates/main.html
    }
}

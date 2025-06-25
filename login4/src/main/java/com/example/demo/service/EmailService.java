package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("您的驗證碼"); // 繁體中文
        message.setText("您的驗證碼是: " + code + "\n它在 5 分鐘內有效。"); // 繁體中文
        mailSender.send(message);
    }
    
    public void sendResetPasswordEmail(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("重設您的密碼"); // 繁體中文
        message.setText("請點擊以下連結重設您的密碼：\n" + resetLink + "\n此連結將在 1 小時後失效。"); // 繁體中文
        mailSender.send(message);
    }
}
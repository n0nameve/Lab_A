package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private MemberService memberService;

    // 刪除帳號
    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session) {
        String email = (String) session.getAttribute("email");

        if (email != null && memberService.deleteUserByEmail(email)) {
            session.invalidate();
            logger.info("Account {} deleted successfully.", email);
            return "redirect:/login?deleted";
        } else {
            logger.error("Failed to delete account for email {}.", email);
            return "redirect:/login?error";
        }
    }

    // 顯示修改密碼頁面
    @GetMapping("/change-password")
    public String showChangePasswordPage() {
        return "change-password";
    }

    // 處理修改密碼
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 Model model) {

        String email = (String) session.getAttribute("email");

        if (email == null) {
            logger.warn("Unauthorized access to change password page. Redirecting to login.");
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            logger.warn("Change password failed for {}: new password and confirm password mismatch.", email);
            return "change-password";
        }

        // 可以考慮在這裡加入新密碼的驗證 (例如長度、複雜度)
        // if (!PasswordUtil.isValid(newPassword)) { ... }

        if (!memberService.authenticate(email, currentPassword)) {
            model.addAttribute("error", "原密碼錯誤");
            logger.warn("Change password failed for {}: current password incorrect.", email);
            return "change-password";
        }

        memberService.updatePassword(email, newPassword);
        model.addAttribute("success", "密碼已成功修改");
        logger.info("Password successfully changed for user {}.", email);
        return "change-password";
    }
}
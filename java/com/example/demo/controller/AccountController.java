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
    public String showChangePasswordPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            logger.warn("Unauthorized access to change password page. Redirecting to login.");
            return "redirect:/login";
        }
        // 確保初始載入時載入主題資訊
        model.addAttribute("availableThemes", memberService.getAvailableThemes(email));
        memberService.getCurrentThemeNameForUser(email).ifPresent(currentTheme ->
            model.addAttribute("currentTheme", currentTheme)
        );
        
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
        
        Runnable loadThemeData = () -> {
            model.addAttribute("availableThemes", memberService.getAvailableThemes(email));
            memberService.getCurrentThemeNameForUser(email).ifPresent(currentTheme ->
                model.addAttribute("currentTheme", currentTheme)
            );
        };


     // --- 1. 新密碼與確認密碼是否一致 ---
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            logger.warn("Change password failed for {}: new password and confirm password mismatch.", email);
            loadThemeData.run();
            return "change-password";
        }

        // --- 2. 新密碼長度驗證 ---
        if (newPassword.length() < 6) {
            model.addAttribute("error", "新密碼至少需要 6 個字元");
            logger.warn("Change password failed for {}: new password too short.", email);
            loadThemeData.run();
            return "change-password";
        }

        // ✨ 3. 驗證「原密碼」是否正確 (這是最重要的安全檢查，如果舊密碼不對，後續的檢查都無意義) ✨
        if (!memberService.authenticate(email, currentPassword)) {
            model.addAttribute("error", "原密碼錯誤");
            logger.warn("Change password failed for {}: current password incorrect.", email);
            loadThemeData.run();
            return "change-password";
        }
        
        // ✨ 4. 檢查「新密碼」是否與資料庫中的「原密碼」相同 ✨
        // 只有在原密碼驗證成功後，才檢查新密碼是否與資料庫中的原密碼相同
        if (memberService.authenticate(email, newPassword)) { 
             model.addAttribute("error", "新密碼不能與原密碼相同");
             logger.warn("Change password failed for {}: new password is same as current password.", email);
             loadThemeData.run();
             return "change-password";
        }
        
        if (!newPassword.matches(".*[A-Z].*")) {
            model.addAttribute("error", "新密碼至少需要一個大寫英文字母");
            logger.warn("Change password failed for {}: new password does not contain an uppercase letter.", email);
            loadThemeData.run();
            return "change-password";
        }

        memberService.updatePassword(email, newPassword);
        model.addAttribute("success", "密碼已成功修改");
        logger.info("Password successfully changed for user {}.", email);
        loadThemeData.run();
        return "change-password";
    }

}
package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MemberController {

    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private ResetPasswordService resetPasswordService;

    // 首頁處理
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email != null) {
            String name = memberService.getNameByUsername(email);
            model.addAttribute("name", name);
            return "welcome";
        }
        return "redirect:/login";
    }

    // 顯示登入頁面
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "login";
    }

    // 處理傳統登入
    @PostMapping("/login")
    public String doLogin(@ModelAttribute("userForm") UserForm userForm,
                          Model model,
                          HttpSession session) {

        String email = userForm.getEmail();
        String password = userForm.getPasswd();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error1", "帳號與密碼不得為空");
            logger.warn("Login attempt with empty email or password.");
            return "login";
        }

        if (memberService.authenticate(email, password)) {
            session.setAttribute("email", email);
            String name = memberService.getNameByUsername(email);
            session.setAttribute("name", name);
            logger.info("User {} logged in successfully.", email);
            return "redirect:/welcome";
        } else {
            model.addAttribute("error", "登入失敗，請檢查Email或密碼");
            logger.warn("Login failed for email: {}", email);
            return "login";
        }
    }

    // 顯示註冊頁面 (請求驗證碼)
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "register";
    }

    // 處理發送驗證碼請求
    @PostMapping("/register/request-code")
    public String requestCode(@ModelAttribute("userForm") @Valid UserForm userForm,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model) {
        if (bindingResult.hasErrors() && bindingResult.getFieldError("email") != null) {
            model.addAttribute("error", bindingResult.getFieldError("email").getDefaultMessage());
            logger.warn("Register request with invalid email: {}", userForm.getEmail());
            return "register";
        }

        String email = userForm.getEmail();
        if (memberService.getNameByUsername(email) != null) {
            model.addAttribute("error", "該Email已經被註冊。");
            logger.warn("Register request for already registered email: {}", email);
            return "register";
        }

        verificationService.generateAndSendCode(email);
        session.setAttribute("emailForVerification", email);
        logger.info("Verification code requested for email: {}", email);
        return "redirect:/verify";
    }

    // 顯示驗證碼輸入頁面
    @GetMapping("/verify")
    public String showVerifyPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("emailForVerification");
        if (email == null) {
            logger.warn("Access to verify page without email in session.");
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        model.addAttribute("userForm", new UserForm());
        return "verify";
    }

    // 處理驗證碼並完成註冊
    @PostMapping("/register/verify")
    public String verifyCodeAndRegister(@ModelAttribute("userForm") @Valid UserForm userForm,
                                        BindingResult bindingResult,
                                        @RequestParam String email,
                                        @RequestParam String code,
                                        HttpSession session,
                                        Model model) {

        if (bindingResult.hasErrors()) {
            String errorMessage = "請檢查所有欄位是否正確填寫。";
            if (bindingResult.getFieldError("passwd") != null) {
                errorMessage = bindingResult.getFieldError("passwd").getDefaultMessage();
            } else if (bindingResult.getFieldError("name") != null) {
                errorMessage = bindingResult.getFieldError("name").getDefaultMessage();
            }
            model.addAttribute("error", errorMessage);
            model.addAttribute("email", email);
            logger.warn("Verification failed due to form errors for email {}: {}", email, bindingResult.getAllErrors());
            return "verify";
        }


        if (verificationService.verifyCode(email, code)) {
            boolean registered = memberService.registerUserAfterVerification(email, userForm.getPasswd(), userForm.getName());
            if (registered) {
                verificationService.deleteVerificationCode(email);
                session.removeAttribute("emailForVerification");
                model.addAttribute("message", "註冊成功，請登入！");
                logger.info("User {} registered successfully.", email);
                return "redirect:/login?registered";
            } else {
                model.addAttribute("error", "註冊失敗，請稍後再試。");
                model.addAttribute("email", email);
                logger.error("User registration failed after verification for email {}.", email);
                return "verify";
            }
        } else {
            model.addAttribute("error", "驗證碼錯誤或已過期。");
            model.addAttribute("email", email);
            logger.warn("Verification code invalid or expired for email {}.", email);
            return "verify";
        }
    }


    // 登出
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String email = (String) session.getAttribute("email");
        session.invalidate();
        logger.info("User {} logged out.", email);
        return "redirect:/login?logout";
    }

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
            logger.warn("Unauthorized access to change password page.");
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            logger.warn("Change password failed for {}: new password and confirm password mismatch.", email);
            return "change-password";
        }

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

    @GetMapping("/welcome")
    public String welcomePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            logger.warn("Access to welcome page without active session. Redirecting to login.");
            return "redirect:/login";
        }
        
        String name = (String) session.getAttribute("name");
        if (name == null) {
            name = memberService.getNameByUsername(email);
            session.setAttribute("name", name);
            logger.info("Retrieved name for user {} from database and set in session.", email);
        }
        
        model.addAttribute("name", name);
        return "welcome";
    }

    // --- 忘記密碼功能新增 ---

    // 顯示忘記密碼頁面 (輸入Email)
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("email", "");
        return "forgot-password";
    }

    // 處理忘記密碼請求 (發送重設連結)
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        Model model,
                                        HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        logger.info("Received forgot password request for email: {} from base URL: {}", email, baseUrl);

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email 不得為空");
            logger.warn("Forgot password request with empty email.");
            return "forgot-password";
        }

        boolean success = resetPasswordService.generateAndSendResetToken(email, baseUrl);
        if (success) {
            model.addAttribute("message", "重設密碼連結已發送到您的Email，請檢查您的信箱 (包含垃圾郵件)。");
            logger.info("Reset password link sent to email: {}", email);
        } else {
            model.addAttribute("message", "重設密碼連結已發送到您的Email，請檢查您的信箱 (包含垃圾郵件)。");
            logger.warn("Failed to generate and send reset token for email: {}. (Email might not exist or other issue)", email);
        }
        return "forgot-password";
    }

    // 顯示重設密碼頁面 (帶有 Token)
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token,
                                        Model model) {
        logger.info("Accessing reset-password page with token: {}", token);
        String email = resetPasswordService.validateToken(token);
        if (email != null) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token); // 將 token 設定到 ResetPasswordForm 物件中
            model.addAttribute("resetPasswordForm", form); // 將包含 token 的 ResetPasswordForm 物件加入 model
            logger.info("Valid token received. Displaying reset password form for email associated with token.");
            return "reset-password";
        } else {
            model.addAttribute("error", "無效或已過期的重設密碼連結。請重新申請。");
            logger.warn("Invalid or expired reset password token received: {}. Redirecting to login.", token);
            return "redirect:/login"; // 導回登入頁面
        }
    }

    // 處理重設密碼請求
    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute("resetPasswordForm") @Valid ResetPasswordForm resetPasswordForm,
                                       BindingResult bindingResult,
                                       Model model) {
        logger.info("Processing reset password request for token: {}", resetPasswordForm.getToken());

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getFieldError("newPassword").getDefaultMessage());
            model.addAttribute("token", resetPasswordForm.getToken()); // 重新傳遞 token
            return "reset-password";
        }

        if (!resetPasswordForm.getNewPassword().equals(resetPasswordForm.getConfirmPassword())) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            model.addAttribute("token", resetPasswordForm.getToken()); // 重新傳遞 token
            return "reset-password";
        }

        boolean success = resetPasswordService.resetPassword(resetPasswordForm.getToken(), resetPasswordForm.getNewPassword());
        if (success) {
            model.addAttribute("message", "密碼已成功重設，請使用新密碼登入。");
            logger.info("Password successfully reset for token: {}", resetPasswordForm.getToken());
            return "redirect:/login?passwordReset";
        } else {
            model.addAttribute("error", "重設密碼失敗或連結已失效，請重新申請。");
            model.addAttribute("token", resetPasswordForm.getToken());
            logger.error("Failed to reset password for token {}. Invalid or expired token, or database error.", resetPasswordForm.getToken());
            return "reset-password";
        }
    }
}

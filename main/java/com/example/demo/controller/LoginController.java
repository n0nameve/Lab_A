package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.ResetPasswordForm;
import com.example.demo.entity.UserForm;

import com.example.demo.service.MemberService;
import com.example.demo.service.ResetPasswordService;
import com.example.demo.service.VerificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private ResetPasswordService resetPasswordService;

    // 首頁處理 (已登入導向歡迎頁，未登入導向 /index)
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email != null) {
            String username = memberService.getNameByUsername(email);
            model.addAttribute("username", username);
            logger.info("User {} is logged in. Redirecting to /welcome.", email);
            return "redirect:/welcome"; // 已登入導向 /welcome
        }
        logger.info("User not logged in. Redirecting to /index (unauthenticated homepage).");
        return "redirect:/index"; // 未登入導向 /index (新的未登入首頁)
    }

    // 顯示未登入首頁 (新增)
    @GetMapping("/index")
    public String index() {
        return "index"; // 對應到你的 src/main/resources/templates/index.html
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
            String username = memberService.getNameByUsername(email);
            session.setAttribute("username", username);
            logger.info("User {} logged in successfully.", email);
            return "redirect:/welcome";
        } else {
            model.addAttribute("error", "登入失敗，請檢查Email或密碼");
            logger.warn("Login failed for email: {}", email);
            return "login";
        }
    }

    // 1. 顯示註冊頁面 (輸入 Email 以請求驗證碼，對應 register.html)
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "register"; // 返回 register.html
    }

    // 處理發送驗證碼請求 (由 register.html 提交到此)
    @PostMapping("/register/request-code")
    public String requestCode(@ModelAttribute("userForm") @Valid UserForm userForm,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model) {
        if (bindingResult.hasErrors() && bindingResult.getFieldError("email") != null) {
            model.addAttribute("error", bindingResult.getFieldError("email").getDefaultMessage());
            logger.warn("Register request with invalid email: {}", userForm.getEmail());
            return "register"; // 返回 register.html 顯示錯誤
        }

        String email = userForm.getEmail();
        if (memberService.findUserByEmail(email).isPresent()) {
            model.addAttribute("error", "該Email已經被註冊。");
            logger.warn("Register request for already registered email: {}", email);
            return "register"; // 返回 register.html 顯示錯誤
        }

        verificationService.generateAndSendCode(email);
        session.setAttribute("emailForVerification", email); // 儲存 Email 到 Session
        // 確保清除任何先前可能存在的 verifiedEmail 狀態
        session.removeAttribute("verifiedEmail");
        logger.info("Verification code requested for email: {}", email);
        return "redirect:/verification"; // 重定向到 verification.html 的 GET 請求
    }

    // 2. 顯示驗證碼輸入頁面 (對應 verification.html)
    @GetMapping("/verification")
    public String showVerificationPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("emailForVerification");
        if (email == null) {
            logger.warn("Access to verification page without email in session. Redirecting to /register.");
            return "redirect:/register"; // 沒有 Email 資訊，導回註冊起始頁
        }
        // Email 不需在頁面上輸入，但可能需要顯示給使用者看 (只讀)
        model.addAttribute("email", email); 
        return "verification"; // 返回 verification.html
    }
    
    // 處理驗證碼驗證 (由 verification.html 提交到此)
    @PostMapping("/verify/code")
    public String verifyCodeAndProceed(@RequestParam String code, // ***關鍵修正：不再接收 @RequestParam String email***
                                       HttpSession session,
                                       Model model) {
        // 從 Session 中獲取 Email
        String email = (String) session.getAttribute("emailForVerification"); 
        if (email == null) {
            model.addAttribute("error", "驗證Email會話已過期，請重新開始。");
            logger.warn("Verification attempt with no email in session.");
            return "redirect:/register"; // Session 失效，導回註冊頁面
        }

        if (verificationService.verifyCode(email, code)) { // 使用從 Session 中取出的 Email
            verificationService.deleteVerificationCode(email); // 驗證成功，刪除驗證碼
            session.setAttribute("verifiedEmail", email); // 將已驗證的 Email 存入 Session
            session.removeAttribute("emailForVerification"); // 移除第一階段 Email 屬性
            logger.info("Verification code successful for email: {}. Redirecting to username/password input.", email);
            return "redirect:/verify"; // 重定向到 verify.html 的 GET 請求
        } else {
            model.addAttribute("error", "驗證碼錯誤或已過期。");
            model.addAttribute("email", email); // 將 Email 傳回前端以供顯示
            logger.warn("Verification code invalid or expired for email {}.", email);
            return "verification"; // 返回 verification.html 顯示錯誤
        }
    }
    
    // 3. 顯示使用者名稱和密碼輸入頁面 (對應 verify.html)
    @GetMapping("/verify") // 這個 @GetMapping /verify 現在負責處理姓名密碼輸入階段
    public String showNamePasswordRegistrationPage(HttpSession session, Model model) {
        String verifiedEmail = (String) session.getAttribute("verifiedEmail");
        if (verifiedEmail == null) {
            logger.warn("Access to username/password registration page without verified email in session.");
            // 如果沒有 verifiedEmail，可能直接訪問或會話過期，導回註冊起始頁或驗證碼頁
            return "redirect:/register"; // 或者 "redirect:/verification" 如果你希望回到驗證碼頁
        }

        model.addAttribute("email", verifiedEmail); // 顯示已驗證的 Email
        model.addAttribute("userForm", new UserForm()); // 用於綁定使用者名稱和密碼
        return "verify"; // 返回你的 verify.html (現在用於姓名密碼輸入)
    }

    @PostMapping("/register/complete")
    public String completeRegistration(@ModelAttribute("userForm") @Valid UserForm userForm,
                                         BindingResult bindingResult,
                                         HttpSession session,
                                         Model model) {

        String verifiedEmail = (String) session.getAttribute("verifiedEmail");

        // *** 新增這行日誌 ***
        logger.info("檢查點 1: 在 /register/complete 中從 Session 取出的 verifiedEmail 是: {}", verifiedEmail);


        if (verifiedEmail == null) {
            model.addAttribute("error", "註冊會話已過期，請重新開始。");
            logger.error("錯誤: Attempted to complete registration with no verified email in session.");
            return "redirect:/register"; // 確保此處有回傳
        }

        // *** 新增這行日誌 ***
        logger.info("檢查點 2: 將 verifiedEmail ({}) 設定到 userForm 的 email 欄位之前，userForm.getEmail() 是: {}", verifiedEmail, userForm.getEmail());

        userForm.setEmail(verifiedEmail); // 將 Session 中的 Email 設置回 UserForm

        // *** 新增這行日誌 ***
        logger.info("檢查點 3: 將 verifiedEmail ({}) 設定到 userForm 的 email 欄位之後，userForm.getEmail() 是: {}", verifiedEmail, userForm.getEmail());


        // 檢查密碼和確認密碼是否一致
        if (!userForm.getPasswd().equals(userForm.getConfirmPasswd())) {
            bindingResult.rejectValue("confirmPasswd", "error.userForm.confirmPasswd", "密碼與確認密碼不一致");
            logger.warn("警告: 密碼與確認密碼不一致 for email: {}", verifiedEmail);
        }

        // 格式驗證使用者名稱和密碼 (以及現在包含的密碼一致性錯誤)
        if (bindingResult.hasErrors()) {
            String errorMessage = "請檢查所有欄位是否正確填寫。";
            // 嘗試獲取更精確的錯誤訊息
            if (bindingResult.getFieldError("email") != null) {
                errorMessage = bindingResult.getFieldError("email").getDefaultMessage();
            } else if (bindingResult.getFieldError("passwd") != null) {
                errorMessage = bindingResult.getFieldError("passwd").getDefaultMessage();
            } else if (bindingResult.getFieldError("username") != null) {
                errorMessage = bindingResult.getFieldError("username").getDefaultMessage();
            } else if (bindingResult.getFieldError("confirmPasswd") != null) {
                 errorMessage = bindingResult.getFieldError("confirmPasswd").getDefaultMessage();
            }

            logger.warn("註冊表單錯誤詳情 for email {}: {}", verifiedEmail, bindingResult.getAllErrors());
            model.addAttribute("error", errorMessage);
            model.addAttribute("email", verifiedEmail); // 傳回 Email 讓前端顯示
            return "verify"; // 確保此處有回傳
        }

        // --- 這裡開始是「後續的註冊成功邏輯」的完整實現 ---
        try {
            // 呼叫註冊服務
            // 確保 memberService.registerUserAfterVerification 接受 verifiedEmail, userForm.getPasswd(), userForm.getName()
            boolean registered = memberService.registerUserAfterVerification(verifiedEmail, userForm.getPasswd(), userForm.getUsername());
            if (registered) {
                session.removeAttribute("verifiedEmail"); // 清除已驗證的 Email
                // 如果你想在重定向後顯示訊息，應使用 RedirectAttributes
                // model.addAttribute("message", "註冊成功，請登入！"); // 如果是重定向，這個 message 會丟失
                logger.info("使用者 {} 註冊成功。", verifiedEmail);
                return "redirect:/login?registered"; // 確保成功時重導向並回傳
            } else {
                // 註冊服務回傳 false，表示註冊失敗 (例如服務層面的邏輯失敗)
                model.addAttribute("error", "註冊失敗，請稍後再試。");
                model.addAttribute("email", verifiedEmail);
                logger.error("使用者註冊失敗 for email {}。", verifiedEmail);
                return "verify"; // 確保此處有回傳 (返回 verify.html 顯示錯誤)
            }
        } catch (Exception e) {
            // 註冊服務執行時發生例外 (例如資料庫連線問題等)
            logger.error("註冊過程中發生錯誤 for email {}: {}", verifiedEmail, e.getMessage(), e); // 打印完整堆棧追蹤
            model.addAttribute("error", "註冊過程中發生錯誤，請稍後再試。");
            model.addAttribute("email", verifiedEmail);
            return "verify"; // 確保此處有回傳 (返回 verify.html 顯示錯誤)
        }
        // --- 結束後續邏輯 ---

        // 備用回傳語句（理論上，如果所有邏輯分支都已覆蓋，這裡不會被執行，但有時編譯器會要求）
        // 如果你已經很確定所有 if/else 和 try-catch 都已經處理了所有可能的回傳，
        // 這行可以不用。但如果編譯器還是報錯，加上這行作為最終的 fallback。
        // return "errorPage"; // 或 "redirect:/error"; 根據你的錯誤處理策略
    }

    // 登出
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String email = (String) session.getAttribute("email");
        session.invalidate();
        logger.info("User {} logged out.", email);
        return "redirect:/login?logout";
    }

    // 顯示忘記密碼頁面
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("email", "");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    @ResponseBody  // 🔑 加這行！
    public String processForgotPassword(@RequestParam String email,
                                        Model model,
                                        HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        logger.info("Received forgot password request for email: {} from base URL: {}", email, baseUrl);

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Forgot password request with empty email.");
            return "empty";
        }

        boolean success = resetPasswordService.generateAndSendResetToken(email, baseUrl);
        if (success) {
            logger.info("Reset password link sent to email: {}", email);
            return "sent";
        } else {
            logger.warn("Email not registered: {}", email);
            return "not_registered";
        }
    }


    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token,
                                        Model model) {
        logger.info("Accessing reset-password page with token: {}", token);
        String email = resetPasswordService.validateToken(token);
        if (email != null) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
            logger.info("Valid token received. Displaying reset password form for email associated with token.");
            return "reset-password";
        } else {
            model.addAttribute("error", "無效或已過期的重設密碼連結。請重新申請。");
            logger.warn("Invalid or expired reset password token received: {}. Redirecting to login.", token);
            return "redirect:/login";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute("resetPasswordForm") @Valid ResetPasswordForm resetPasswordForm,
                                       BindingResult bindingResult,
                                       Model model) {
        logger.info("Processing reset password request for token: {}", resetPasswordForm.getToken());

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getFieldError("newPassword").getDefaultMessage());
            model.addAttribute("token", resetPasswordForm.getToken());
            return "reset-password";
        }

        if (!resetPasswordForm.getNewPassword().equals(resetPasswordForm.getConfirmPassword())) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            model.addAttribute("token", resetPasswordForm.getToken());
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
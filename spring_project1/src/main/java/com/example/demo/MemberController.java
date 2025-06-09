package com.example.demo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // 確保有這個 import

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // 確保有這個 import
import org.springframework.web.bind.annotation.*;

@Controller
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private VerificationService verificationService; // 引入 VerificationService

    // 首頁處理
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email != null) {
            String name = memberService.getNameByUsername(email);
            model.addAttribute("name", name);
            return "welcome"; // 跳轉到 welcome.html
        }
        return "redirect:/login"; // 未登入導向登入頁
    }

    // 顯示登入頁面
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("userForm", new UserForm()); // 用於傳統登入表單綁定
        return "login"; // 對應到 login.html
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
            return "login";
        }

        if (memberService.authenticate(email, password)) {
            // 登入成功，設定 Session
            session.setAttribute("email", email);
            String name = memberService.getNameByUsername(email);
            session.setAttribute("name", name);
            return "redirect:/welcome"; // 導向 welcome 頁面
        } else {
            model.addAttribute("error", "登入失敗，請檢查Email或密碼");
            return "login"; // 登入失敗回到登入頁
        }
    }

    // 顯示註冊頁面 (請求驗證碼)
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("userForm", new UserForm()); // 用於 email 填寫
        return "register"; // 對應到新的 register.html
    }

    // 處理發送驗證碼請求
    @PostMapping("/register/request-code")
    public String requestCode(@ModelAttribute("userForm") @Valid UserForm userForm,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model) {
        // 只驗證 email
        if (bindingResult.hasErrors() && bindingResult.getFieldError("email") != null) {
            // 如果 email 驗證失敗，返回註冊頁
            model.addAttribute("error", bindingResult.getFieldError("email").getDefaultMessage());
            return "register";
        }

        String email = userForm.getEmail();
        // 檢查 email 是否已經存在於 member table，避免發送驗證碼給已註冊用戶
        if (memberService.getNameByUsername(email) != null) {
            model.addAttribute("error", "該Email已經被註冊。");
            return "register";
        }

        verificationService.generateAndSendCode(email);
        session.setAttribute("emailForVerification", email); // 暫時儲存 email
        return "redirect:/verify"; // 導向驗證碼輸入頁
    }

    // 顯示驗證碼輸入頁面
    @GetMapping("/verify")
    public String showVerifyPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("emailForVerification");
        if (email == null) {
            // 如果沒有 email 在 session 中，表示未經過發送驗證碼步驟，導回註冊頁
            return "redirect:/register";
        }
        model.addAttribute("email", email); // 帶入 email 到驗證頁面
        model.addAttribute("userForm", new UserForm()); // 用於綁定 code, password, name
        return "verify"; // 對應到新的 verify.html
    }

    // 處理驗證碼並完成註冊
    @PostMapping("/register/verify")
    public String verifyCodeAndRegister(@ModelAttribute("userForm") @Valid UserForm userForm,
                                        BindingResult bindingResult,
                                        @RequestParam String email, // 從 hidden input 獲取
                                        @RequestParam String code,
                                        HttpSession session,
                                        Model model) {

        // 驗證密碼和姓名（email 已經在 request-code 驗證過了）
        // 我們需要針對 verify 頁面提交的 UserForm 進行部分驗證
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "請檢查所有欄位是否正確填寫。");
            // 由於 UserForm 包含 email, passwd, name，我們需要更細緻地判斷哪個欄位有錯
            if (bindingResult.getFieldError("passwd") != null) {
                model.addAttribute("error", bindingResult.getFieldError("passwd").getDefaultMessage());
            } else if (bindingResult.getFieldError("name") != null) {
                model.addAttribute("error", bindingResult.getFieldError("name").getDefaultMessage());
            } else if (bindingResult.getFieldError("email") != null) {
                 // 這裡的 email 應該是來自 hidden input，理論上不應該有錯，但以防萬一
                model.addAttribute("error", bindingResult.getFieldError("email").getDefaultMessage());
            }
            model.addAttribute("email", email); // 確保 email 傳回頁面
            return "verify";
        }


        if (verificationService.verifyCode(email, code)) {
            // 驗證碼正確，執行註冊
            boolean registered = memberService.registerUserAfterVerification(email, userForm.getPasswd(), userForm.getName());
            if (registered) {
                verificationService.deleteVerificationCode(email); // 註冊成功後刪除驗證碼
                session.removeAttribute("emailForVerification"); // 移除 session 中的 email
                model.addAttribute("message", "註冊成功，請登入！");
                return "redirect:/login?registered"; // 導向登入頁面
            } else {
                model.addAttribute("error", "註冊失敗，請稍後再試。"); // 這種情況通常不會發生，因為之前已經檢查過 email 唯一性
                model.addAttribute("email", email);
                return "verify";
            }
        } else {
            model.addAttribute("error", "驗證碼錯誤或已過期。");
            model.addAttribute("email", email);
            return "verify"; // 驗證失敗回到驗證頁
        }
    }


    // 登出
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 清除 Session
        return "redirect:/login?logout";
    }

    // 刪除帳號
    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session) {
        String email = (String) session.getAttribute("email");

        if (email != null && memberService.deleteUserByEmail(email)) {
            session.invalidate(); // 登出
            return "redirect:/login?deleted";
        } else {
            return "redirect:/login?error";
        }
    }

    // 顯示修改密碼頁面
    @GetMapping("/change-password")
    public String showChangePasswordPage() {
        return "change-password"; // 對應到 change-password.html
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
            return "redirect:/login"; // 未登入導向登入頁
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            return "change-password";
        }

        // 檢查原密碼是否正確
        if (!memberService.authenticate(email, currentPassword)) {
            model.addAttribute("error", "原密碼錯誤");
            return "change-password";
        }

        // 更新密碼
        memberService.updatePassword(email, newPassword);
        model.addAttribute("success", "密碼已成功修改");
        return "change-password";
    }

    @GetMapping("/welcome")
    public String welcomePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            // 如果 Session 中沒有 email，表示用戶可能未登入或 Session 已過期，重定向到登入頁
            return "redirect:/login";
        }
        
        // 從 Session 中獲取 name，因為登入成功時已經存入 Session
        String name = (String) session.getAttribute("name");
        if (name == null) {
            // 萬一 Session 中的 name 不見了（但不應該發生，因為 email 還在），
            // 可以從資料庫重新查詢並存入 Session
            name = memberService.getNameByUsername(email);
            session.setAttribute("name", name); // 重新存回 Session
        }
        
        model.addAttribute("name", name); // 將 name 傳遞給 welcome.html
        return "welcome"; // 返回 welcome.html 模板
    }
}
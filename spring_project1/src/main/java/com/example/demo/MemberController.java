package com.example.demo;

import com.example.demo.UserForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class MemberController {

    @Autowired
    private MemberService memberService;

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

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "login";
    }

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
            session.setAttribute("email", email);
            return "redirect:/";
        } else {
            model.addAttribute("error", "帳號或密碼錯誤");
            return "login";
        }
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("userForm") UserForm userForm,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        boolean success = memberService.register(userForm.getEmail(), userForm.getPasswd(),userForm.getName());
        if (!success) {
            model.addAttribute("error", "電子郵件已存在");
            return "register";
        }

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    
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
    
    @GetMapping("/change-password")
    public String showChangePasswordPage() {
        return "change-password"; // 對應到 change-password.html
    }
    
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 Model model) {

        String email = (String) session.getAttribute("email");

        if (email == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密碼與確認密碼不一致");
            return "change-password";
        }

        if (!memberService.authenticate(email, currentPassword)) {
            model.addAttribute("error", "原密碼錯誤");
            return "change-password";
        }

        memberService.updatePassword(email, newPassword);
        model.addAttribute("success", "密碼已成功修改");
        return "change-password";
    }

}

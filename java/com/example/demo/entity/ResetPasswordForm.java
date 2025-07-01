package com.example.demo.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordForm {

    private String token; // 用於傳遞重設密碼的 token

    @NotBlank(message = "密碼不得為空")
    @Size(min = 8, message = "密碼長度至少8碼以上")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", message = "密碼需包含至少一個大寫英文和一個小寫英文")
    private String newPassword;

    private String confirmPassword; // 用於確認新密碼

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}


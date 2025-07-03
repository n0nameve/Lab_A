package com.example.demo.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserForm {

    private String email;

    @NotBlank(message = "密碼不得為空")
    @Size(min = 8, message = "密碼長度至少8碼以上")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", message = "密碼需包含至少一個大寫英文和一個小寫英文")
    private String passwd;

    @NotBlank(message = "姓名不得為空")
    private String username;
    
    private String confirmPasswd;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getConfirmPasswd() {
		return confirmPasswd;
	}

	public void setConfirmPasswd(String confirmPasswd) {
		this.confirmPasswd = confirmPasswd;
	}  
}
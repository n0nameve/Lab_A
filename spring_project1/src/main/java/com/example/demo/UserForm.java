package com.example.demo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserForm {

    @NotBlank(message = "Email不得為空")
    @Email(message = "請輸入有效的Email")
    private String email;

    @NotBlank(message = "密碼不得為空")
    @Size(min = 8, message = "密碼長度至少8碼以上")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", message = "密碼需包含至少一個大寫英文和一個小寫英文")
    private String passwd;

    @NotBlank(message = "姓名不得為空")
    private String name;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    // Getters and Setters
    
}
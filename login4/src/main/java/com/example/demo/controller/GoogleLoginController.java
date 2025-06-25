package com.example.demo.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@Controller  // ✅ 改成 Controller（不是 RestController）
public class GoogleLoginController {

    private static final String CLIENT_ID = "66087248968-6h4ahpd26h2hofimaub8hd45l8svg8um.apps.googleusercontent.com";

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @PostMapping("/google-login")
    public void googleLogin(@RequestBody Map<String, String> body,
                            HttpServletResponse response,
                            HttpSession session) throws IOException {
        String idToken = body.get("idToken");

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        try {
            GoogleIdToken idTokenObj = verifier.verify(idToken);
            if (idTokenObj != null) {
                GoogleIdToken.Payload payload = idTokenObj.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                // 檢查使用者是否已存在
                String sqlCheck = "SELECT COUNT(*) FROM member WHERE email = :email";
                MapSqlParameterSource checkParam = new MapSqlParameterSource("email", email);
                Integer count = jdbc.queryForObject(sqlCheck, checkParam, Integer.class);

                if (count == 0) {
                    // 第一次登入 → 自動註冊
                    String sqlInsert = "INSERT INTO member (email, password, name) " +
                            "VALUES (:email, :password, :name)";
                    MapSqlParameterSource insertParam = new MapSqlParameterSource()
                            .addValue("email", email)
                            .addValue("password", "google")
                            .addValue("name", name);
                    jdbc.update(sqlInsert, insertParam);
                    System.out.println("✅ 新增 Google 使用者：" + email);
                } else {
                    System.out.println("🔁 已存在使用者：" + email);
                }

                // 存入 Session
                session.setAttribute("name", name);
                session.setAttribute("email", email);

                // 導向 welcome
                response.sendRedirect("/welcome");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "無效的 Google token");
            }
        } catch (GeneralSecurityException | IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Google 登入驗證失敗");
        }
    }
}
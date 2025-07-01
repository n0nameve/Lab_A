package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 通常會加上 @Repository 註解

import java.util.Optional; // 引入 Optional

@Repository // 建議加上 @Repository 註解
public interface UserRepository extends JpaRepository<User, Long> {

    // 將 findByUsername(String username) 改為 findByEmail(String email)
    // 因為你的 User 實體中是用 'email' 作為用戶名/唯一識別
    Optional<User> findByEmail(String email); // <--- 修正這裡！

    // 如果還有其他需要根據 email 查詢的方法，也請使用 email

    // 你可能還會有的其他方法，例如根據 ID 查詢，這些不受影響
    // Optional<User> findById(Long id);
}
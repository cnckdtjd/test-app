package com.jacob.userservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }
    
    @GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }
    
    @GetMapping("/logout")
    public String logout() {
        // 실제 로그아웃 처리는 클라이언트 측에서 토큰을 삭제하는 방식으로 구현
        return "redirect:/login";
    }
} 
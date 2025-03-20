package com.jacob.testapp.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.jacob.testapp.admin.controller")
public class AdminControllerAdvice {

    /**
     * 모든 관리자 컨트롤러에 requestURI 모델 속성을 자동으로 추가합니다.
     * Thymeleaf 3.1 이상에서는 #request 객체에 직접 접근할 수 없기 때문에 필요합니다.
     */
    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request, Model model) {
        model.addAttribute("requestURI", request.getRequestURI());
    }
} 
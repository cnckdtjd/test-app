package com.jacob.testapp.common.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // 요청 경로에 따라 관리자 페이지 또는 일반 사용자 페이지 오류 처리
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        boolean isAdminPage = requestUri != null && requestUri.startsWith("/admin");

        // 에러 정보 가져오기
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> errorInfo = this.errorAttributes.getErrorAttributes(
                webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
        
        // 에러 상태 코드
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("status", statusCode);
            
            // 404 오류 처리
            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("message", "요청하신 페이지를 찾을 수 없습니다.");
            }
            // 500 오류 처리
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("message", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            }
            // 401 오류 처리
            else if(statusCode == HttpStatus.UNAUTHORIZED.value()) {
                model.addAttribute("message", "로그인이 필요한 페이지입니다.");
            }
            // 403 오류 처리
            else if(statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("message", "접근 권한이 없습니다.");
            }
            // 기타 오류
            else {
                model.addAttribute("message", errorInfo.get("message"));
            }
        }
        
        // 에러의 타임스탬프, 에러 메시지, 트레이스 정보 추가
        model.addAttribute("timestamp", errorInfo.get("timestamp"));
        
        // 개발 환경에서만 스택 트레이스 노출
        if (errorInfo.containsKey("trace")) {
            model.addAttribute("trace", errorInfo.get("trace"));
        }
        
        // 관리자 페이지 또는 일반 페이지 에러 템플릿 선택
        if (isAdminPage) {
            return "admin/error";
        } else {
            return "error";
        }
    }
} 
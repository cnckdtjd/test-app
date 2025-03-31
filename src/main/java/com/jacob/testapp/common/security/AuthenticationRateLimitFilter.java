package com.jacob.testapp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 인증 요청 모니터링을 위한 필터 
 * (속도 제한 로직 제거됨 - 단순 로깅만 수행)
 */
@Component
@Slf4j
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 로그인 관련 URL만 로깅
        if ("/login-process".equals(request.getRequestURI()) || 
            ("/login".equals(request.getRequestURI()) && "POST".equals(request.getMethod()))) {
            
            String username = request.getParameter("username");
            String requestId = request.getHeader("X-Request-ID");
            String sessionId = request.getSession().getId();
            
            if (username != null) {
                log.info("로그인 요청 감지: 사용자={}, 세션ID={}, 요청ID={}", 
                        username, sessionId, requestId);
            }
        }
        
        // 필터 체인 계속 진행
        filterChain.doFilter(request, response);
    }
} 
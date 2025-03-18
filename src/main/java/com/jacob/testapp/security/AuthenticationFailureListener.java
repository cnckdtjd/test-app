package com.jacob.testapp.security;

import com.jacob.testapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 인증 실패 이벤트를 처리하는 리스너
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {

    private final UserService userService;
    
    @Override
    public void onApplicationEvent(AbstractAuthenticationFailureEvent event) {
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            // 세션에서 사용자 이름을 가져옵니다
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = (String) request.getSession().getAttribute("LAST_USERNAME");
                
                if (username != null) {
                    // 로그인 실패 처리
                    boolean isLocked = userService.handleLoginFailure(username);
                    if (isLocked) {
                        // 계정이 잠겼다는 로그 기록
                        System.out.println("Account locked: " + username);
                    }
                }
            }
        }
    }
} 
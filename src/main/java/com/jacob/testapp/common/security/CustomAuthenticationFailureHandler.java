package com.jacob.testapp.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다";
        String errorCode = "BAD_CREDENTIALS";
        boolean isAjaxRequest = isAjaxRequest(request);

        // 요청 디버깅
        String sessionId = request.getSession().getId();
        String requestId = request.getHeader("X-Request-ID");
        log.warn("인증 실패 처리: 사용자={}, 예외={}, 메시지={}, AJAX={}, 세션ID={}, 요청ID={}", 
                username, exception.getClass().getSimpleName(), exception.getMessage(), 
                isAjaxRequest, sessionId, requestId);

        // 예외 타입 및 메시지에 따라 메시지 설정
        if (exception instanceof LockedException) {
            errorCode = "ACCOUNT_LOCKED";
            errorMessage = "계정이 잠겼습니다. 관리자에게 문의하세요";
        } else if (exception instanceof DisabledException) {
            errorCode = "ACCOUNT_DISABLED";
            errorMessage = "계정이 비활성화되었습니다. 관리자에게 문의하세요";
        } else if (exception instanceof BadCredentialsException) {
            errorCode = "BAD_CREDENTIALS";
            errorMessage = "비밀번호가 올바르지 않습니다. 다시 확인해주세요";
            
            // 이벤트 발행
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(
                        new UsernamePasswordAuthenticationToken(username, ""), exception));
            }
        } else if (exception instanceof InternalAuthenticationServiceException) {
            // 내부 인증 서비스 오류는 코드와 메시지 분리 처리
            String message = exception.getMessage();
            if (message != null && message.contains("|")) {
                String[] parts = message.split("\\|", 2);
                errorCode = parts[0];
                errorMessage = parts.length > 1 ? parts[1] : message;
            } else {
                errorCode = "INTERNAL_ERROR";
                errorMessage = "로그인 처리 중 오류가 발생했습니다";
            }
        } else if (exception instanceof UsernameNotFoundException) {
            // 사용자 이름 찾을 수 없음 예외 처리
            String message = exception.getMessage();
            if (message != null && message.contains("|")) {
                String[] parts = message.split("\\|", 2);
                errorCode = parts[0];
                errorMessage = parts.length > 1 ? parts[1] : message;
            } else {
                errorCode = "USERNAME_NOT_FOUND";
                errorMessage = "입력하신 아이디가 존재하지 않습니다. 아이디를 확인해주세요";
            }
        }
        
        // AJAX 요청 또는 요청 헤더에 X-Request-ID가 있는 경우 JSON으로 응답
        if (isAjaxRequest) {
            sendJsonResponse(response, errorMessage, errorCode);
        } else {
            // 일반 요청인 경우 리다이렉트
            String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, "/login?error=" + encodedMessage);
        }
    }
    
    /**
     * JSON 형식으로 응답을 반환
     */
    private void sendJsonResponse(HttpServletResponse response, String errorMessage, String errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // JSON 응답 작성
        String jsonResponse = String.format(
                "{\"error\":true,\"code\":\"%s\",\"message\":\"%s\",\"redirect\":\"/login?error=%s\"}",
                errorCode,
                errorMessage,
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
        );
        
        // 응답 출력 및 종료
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        response.getWriter().close();
    }
    
    /**
     * AJAX 요청인지 확인
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");
        String requestId = request.getHeader("X-Request-ID");
        
        // 고유 요청 ID가 있으면 AJAX 요청으로 간주 (클라이언트에서 설정한 경우)
        boolean hasRequestId = requestId != null && requestId.startsWith("login-");
        
        // 다양한 방식으로 AJAX 요청 식별
        return hasRequestId || 
               "XMLHttpRequest".equals(requestedWith) ||
               (contentType != null && contentType.contains("application/json")) ||
               (accept != null && accept.contains("application/json"));
    }
}

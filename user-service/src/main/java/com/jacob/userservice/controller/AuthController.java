package com.jacob.userservice.controller;

import com.jacob.userservice.model.User;
import com.jacob.userservice.security.JwtTokenProvider;
import com.jacob.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * 로그인 요청 처리
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        try {
            // 사용자 계정이 잠겨있는지 확인
            if (userService.isAccountLocked(username)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "계정이 잠겼습니다. 관리자에게 문의하세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // 인증 요청
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // 인증 성공 시 토큰 생성
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            String token = jwtTokenProvider.createToken(username, role);
            
            // 로그인 시도 횟수 초기화
            userService.resetLoginAttempts(username);
            
            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            response.put("role", role);
            
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("name", user.getName());
                response.put("email", user.getEmail());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            // 로그인 실패 처리
            boolean accountLocked = userService.handleLoginFailure(username);
            
            Map<String, String> errorResponse = new HashMap<>();
            if (accountLocked) {
                errorResponse.put("message", "로그인 시도 횟수가 초과되어 계정이 잠겼습니다. 관리자에게 문의하세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            } else {
                errorResponse.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (AuthenticationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * 토큰 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> tokenRequest) {
        String token = tokenRequest.get("token");
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Map<String, Object> response = jwtTokenProvider.getUserInfo(token);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "유효하지 않은 토큰입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
} 
package com.jacob.userservice.controller;

import com.jacob.userservice.model.User;
import com.jacob.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인한 사용자의 프로필 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // 비밀번호는 응답에서 제외
        user.setPassword(null);
        
        return ResponseEntity.ok(user);
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보 업데이트
     */
    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@Valid @RequestBody User user, Principal principal) {
        User updatedUser = userService.updateUser(user, principal);
        
        // 비밀번호는 응답에서 제외
        updatedUser.setPassword(null);
        
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            
            // 비밀번호는 응답에서 제외
            registeredUser.setPassword(null);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 사용자 ID로 사용자 조회 (관리자 전용)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        
        // 비밀번호는 응답에서 제외
        user.setPassword(null);
        
        return ResponseEntity.ok(user);
    }

    /**
     * 모든 사용자 목록 조회 (관리자 전용)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        
        // 비밀번호는 응답에서 제외
        users.forEach(user -> user.setPassword(null));
        
        return ResponseEntity.ok(users);
    }

    /**
     * 사용자 삭제 (관리자 전용)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 계정 잠금 해제 (관리자 전용)
     */
    @PostMapping("/{username}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlockAccount(@PathVariable String username) {
        userService.unlockAccount(username);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자명 또는 이메일 중복 확인
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        
        Map<String, Boolean> response = new HashMap<>();
        
        if (username != null && !username.isEmpty()) {
            response.put("username", userService.findByUsername(username).isPresent());
        }
        
        if (email != null && !email.isEmpty()) {
            response.put("email", userService.findByEmail(email).isPresent());
        }
        
        return ResponseEntity.ok(response);
    }
} 
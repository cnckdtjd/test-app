package com.jacob.userservice.service;

import com.jacob.userservice.model.User;
import com.jacob.userservice.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * 모든 사용자 목록 조회
     */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    /**
     * 페이지네이션이 적용된 사용자 목록 조회
     */
    public List<User> findAllWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.findAllWithPagination(offset, size);
    }

    /**
     * 사용자 수 조회
     */
    public long countUsers() {
        return userMapper.countAll();
    }
    
    /**
     * 특정 상태의 사용자 수 조회
     */
    public long countUsersByStatus(User.Status status) {
        return userMapper.countByStatus(status);
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    public User findById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id);
        }
        return user;
    }

    /**
     * 사용자명으로 사용자 조회
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userMapper.findByEmail(email));
    }

    /**
     * 사용자 저장
     */
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            // 신규 사용자일 경우 비밀번호 암호화
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // 현재 시간 설정
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            
            userMapper.save(user);
        } else {
            // 기존 사용자 업데이트
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.update(user);
        }
        return user;
    }

    /**
     * 사용자 등록
     */
    @Transactional
    public User register(User user) {
        if (userMapper.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다");
        }
        if (userMapper.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }
        
        // 비밀번호 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 초기값 설정
        user.setEnabled(true);
        user.setRole(User.Role.ROLE_USER);
        user.setLoginAttempts(0);
        user.setAccountLocked(false);
        user.setStatus(User.Status.ACTIVE);
        
        // 시간 설정
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(now);
        
        // 사용자 저장
        userMapper.save(user);
        
        return user;
    }

    /**
     * 로그인 시도 횟수 업데이트
     */
    @Transactional
    public void updateLoginAttempts(String username, int attempts) {
        userMapper.updateLoginAttempts(username, attempts);
    }

    /**
     * 계정 잠금
     */
    @Transactional
    public void lockAccount(String username) {
        userMapper.updateAccountLocked(username, true);
    }

    /**
     * 계정 잠금 해제
     */
    @Transactional
    public void unlockAccount(String username) {
        userMapper.updateAccountLocked(username, false);
        userMapper.updateLoginAttempts(username, 0);
    }

    /**
     * 로그인 실패 처리
     */
    @Transactional
    public boolean handleLoginFailure(String username) {
        User user = userMapper.findByUsername(username);
        if (user != null) {
            int attempts = user.getLoginAttempts() + 1;
            userMapper.updateLoginAttempts(username, attempts);
            
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                userMapper.updateAccountLocked(username, true);
                return true; // 계정 잠김
            }
        }
        return false;
    }

    /**
     * 로그인 시도 횟수 초기화
     */
    @Transactional
    public void resetLoginAttempts(String username) {
        userMapper.updateLoginAttempts(username, 0);
    }

    /**
     * 계정 잠금 상태 확인
     */
    public boolean isAccountLocked(String username) {
        User user = userMapper.findByUsername(username);
        return user != null && user.isAccountLocked();
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    /**
     * 사용자 프로필 업데이트
     */
    @Transactional
    public User updateUser(User user, Principal principal) {
        // 현재 로그인한 사용자의 아이디로 사용자 정보를 조회
        String currentUsername = principal.getName();
        User existingUser = Optional.ofNullable(userMapper.findByUsername(currentUsername))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. username: " + currentUsername));

        // 기존 사용자 정보에 새로운 정보를 반영
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        existingUser.setUpdatedAt(LocalDateTime.now());

        // 수정된 사용자 정보를 저장
        userMapper.update(existingUser);
        
        return existingUser;
    }
} 
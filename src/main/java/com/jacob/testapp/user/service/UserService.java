package com.jacob.testapp.user.service;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.repository.CartRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 서비스
 */
@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // 생성자를 통한 의존성 주입, @Lazy 어노테이션 사용
    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder, CartRepository cartRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name().substring(5)) // "ROLE_" 접두사 제거
                .accountLocked(user.getStatus() == User.Status.LOCKED)
                .disabled(user.getStatus() == User.Status.INACTIVE)
                .build();
    }

    /**
     * 모든 사용자 목록 조회
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 사용자 수 조회
     */
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * 특정 상태의 사용자 수 조회
     */
    public long countUsersByStatus(User.Status status) {
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == status)
                .count();
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 사용자 저장
     */
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            // 신규 사용자일 경우 비밀번호 암호화
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setRole(User.Role.ROLE_USER);
        user.setLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLastLoginAt(LocalDateTime.now());
        
        // 사용자 저장
        User savedUser = userRepository.save(user);
        
        // 사용자의 장바구니 생성
        Cart cart = Cart.builder()
                .user(savedUser)
                .totalPrice(BigDecimal.ZERO)
                .build();
        cartRepository.save(cart);
        
        return savedUser;
    }

    @Transactional
    public void updateLoginAttempts(String username, int attempts) {
        userRepository.updateLoginAttempts(username, attempts);
    }

    @Transactional
    public void lockAccount(String username) {
        userRepository.updateAccountLocked(username, true);
    }

    @Transactional
    public void unlockAccount(String username) {
        userRepository.updateAccountLocked(username, false);
        userRepository.updateLoginAttempts(username, 0);
    }

    @Transactional
    public boolean handleLoginFailure(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            int attempts = user.getLoginAttempts() + 1;
            userRepository.updateLoginAttempts(username, attempts);
            
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                userRepository.updateAccountLocked(username, true);
                return true; // Account is locked
            }
        }
        return false;
    }

    @Transactional
    public void resetLoginAttempts(String username) {
        userRepository.updateLoginAttempts(username, 0);
    }

    @Transactional
    public boolean isAccountLocked(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::isAccountLocked).orElse(false);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
} 
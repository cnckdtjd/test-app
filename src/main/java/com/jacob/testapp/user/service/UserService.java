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
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder, CartRepository cartRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findUserByUsername(username);
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getAuthority())
                .accountLocked(user.getStatus() == User.Status.LOCKED)
                .disabled(!user.isActive())
                .build();
    }

    /**
     * 사용자명으로 사용자 찾기
     */
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 모든 사용자 목록 조회
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 페이지네이션을 적용한 사용자 목록 조회
     */
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 전체 사용자 수 조회
     */
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * 특정 상태의 사용자 수 조회
     */
    public long countUsersByStatus(User.Status status) {
        return userRepository.countByStatus(status);
    }

    /**
     * ID로 사용자 조회
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 사용자명으로 사용자 조회
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 사용자 저장
     */
    @Transactional
    public User save(User user) {
        encodePasswordIfNeeded(user);
        return userRepository.save(user);
    }

    /**
     * 비밀번호 암호화 처리
     */
    private void encodePasswordIfNeeded(User user) {
        if (user.getId() == null || (user.getPassword() != null && !user.getPassword().startsWith("$2a$"))) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
    }

    /**
     * 사용자 등록
     */
    @Transactional
    public User register(User user) {
        validateNewUser(user);
        
        // 비밀번호 암호화 및 상태 초기화
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setRole(User.Role.ROLE_USER);
        user.resetLoginAttempts();
        user.setStatus(User.Status.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now());

        // 사용자 저장
        User savedUser = userRepository.save(user);

        // 사용자의 장바구니 생성
        createCartForUser(savedUser);

        return savedUser;
    }
    
    /**
     * 신규 사용자 유효성 검사
     */
    private void validateNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }
    }
    
    /**
     * 사용자의 장바구니 생성
     */
    private void createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .build();
        cartRepository.save(cart);
    }

    /**
     * 로그인 시도 횟수 업데이트
     */
    @Transactional
    public void updateLoginAttempts(String username, int attempts) {
        userRepository.updateLoginAttempts(username, attempts);
    }

    /**
     * 계정 잠금 처리
     */
    @Transactional
    public void lockAccount(String username) {
        userRepository.updateAccountLockStatus(username, true);
    }

    /**
     * 계정 잠금 해제 처리
     */
    @Transactional
    public void unlockAccount(String username) {
        userRepository.updateAccountLockStatus(username, false);
        userRepository.updateLoginAttempts(username, 0);
    }

    /**
     * 로그인 실패 처리
     * @return 계정 잠금 여부
     */
    @Transactional
    public boolean handleLoginFailure(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    boolean shouldLock = user.incrementLoginAttempts();
                    updateLoginAttempts(username, user.getLoginAttempts());
                    
                    if (shouldLock) {
                        lockAccount(username);
                    }
                    
                    return shouldLock;
                })
                .orElse(false);
    }

    /**
     * 로그인 시도 초기화
     */
    @Transactional
    public void resetLoginAttempts(String username) {
        userRepository.updateLoginAttempts(username, 0);
    }

    /**
     * 계정 잠금 상태 확인
     */
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(User::isAccountLocked)
                .orElse(false);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * 사용자 정보 업데이트
     */
    @Transactional
    public User updateUser(User user, Principal principal) {
        User existingUser = findUserByUsername(principal.getName());
        
        // 기존 사용자 정보 업데이트
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        
        return userRepository.save(existingUser);
    }
    
    /**
     * 로그인 성공 처리
     */
    @Transactional
    public void handleLoginSuccess(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        userOpt.ifPresent(user -> {
            user.resetLoginAttempts();
            user.updateLastLoginTime();
            userRepository.updateLoginAttempts(username, 0);
            userRepository.updateLastLoginAt(username, LocalDateTime.now());
        });
    }
} 
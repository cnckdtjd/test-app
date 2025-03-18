package com.jacob.testapp.user.service;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // 생성자를 통한 의존성 주입, @Lazy 어노테이션 사용
    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User save(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
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
        return userRepository.save(user);
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

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
} 
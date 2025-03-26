package com.jacob.testapp.common.security;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("사용자 로그인 시도: {}", username);
        
        // 사용자가 존재하는지 확인
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            log.warn("사용자를 찾을 수 없음: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        User user = userOptional.get();
        log.info("사용자 정보: ID={}, Status={}, AccountLocked={}, Enabled={}", 
                 user.getId(), user.getStatus(), user.isAccountLocked(), user.isEnabled());

        if (user.isAccountLocked()) {
            log.warn("계정이 잠겨 있음: {}", username);
            throw new RuntimeException("User account is locked");
        }
        
        if (!user.isEnabled()) {
            log.warn("계정이 비활성화됨: {}", username);
            throw new RuntimeException("User account is disabled");
        }
        
        if (user.getStatus() == User.Status.INACTIVE) {
            log.warn("계정 상태가 비활성 상태임: {}", username);
            throw new RuntimeException("User account is inactive");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        log.info("사용자 권한: {}", user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                !user.isAccountLocked(),
                authorities
        );
    }
} 
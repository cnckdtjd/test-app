package com.jacob.userservice.security;

import com.jacob.userservice.model.User;
import com.jacob.userservice.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            log.warn("빈 사용자명으로 로그인 시도");
            throw new UsernameNotFoundException("아이디를 입력해주세요.");
        }
        
        log.info("사용자 로그인 시도: {}", username);
        
        // 사용자 조회
        User user = userMapper.findByUsername(username);

        if (user == null) {
            log.warn("사용자를 찾을 수 없음: {}", username);
            throw new UsernameNotFoundException("입력하신 아이디가 존재하지 않습니다. 아이디를 확인해주세요.");
        }

        log.info("사용자 정보: ID={}, Status={}, AccountLocked={}, Enabled={}",
                user.getId(), user.getStatus(), user.isAccountLocked(), user.isEnabled());

        if (user.isAccountLocked()) {
            log.warn("계정이 잠겨 있음: {}", username);
            throw new InternalAuthenticationServiceException("계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        if (!user.isEnabled()) {
            log.warn("계정이 비활성화됨: {}", username);
            throw new InternalAuthenticationServiceException("계정이 비활성화되었습니다. 관리자에게 문의하세요.");
        }

        if (user.getStatus() == User.Status.INACTIVE) {
            log.warn("계정 상태가 비활성 상태임: {}", username);
            throw new InternalAuthenticationServiceException("계정이 비활성 상태입니다. 관리자에게 문의하세요.");
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
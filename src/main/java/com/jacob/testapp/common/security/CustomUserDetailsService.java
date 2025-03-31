package com.jacob.testapp.common.security;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
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
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            log.warn("빈 사용자명으로 로그인 시도");
            throw new UsernameNotFoundException("INPUT_EMPTY|아이디를 입력해주세요.");
        }
        
        log.info("사용자 로그인 시도: {}", username);
        
        // 요청 추적을 위한 로깅
        String sessionId = request.getSession().getId();
        log.debug("인증 요청: 세션ID={}", sessionId);
        
        // 이전 인증 실패 여부 확인
        Object authFailed = request.getSession().getAttribute("AUTH_FAILED");
        if (authFailed != null && Boolean.TRUE.equals(authFailed)) {
            log.warn("이전 인증 실패가 감지되었습니다. 세션을 무효화합니다. 세션ID={}", sessionId);
            // 세션 무효화
            request.getSession().invalidate();
        }
        
        // 사용자 조회
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            log.warn("사용자를 찾을 수 없음: {}", username);
            // 세션에 인증 실패 표시
            request.getSession().setAttribute("AUTH_FAILED", true);
            // 401 응답 코드 설정
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new UsernameNotFoundException("USERNAME_NOT_FOUND|입력하신 아이디가 존재하지 않습니다. 아이디를 확인해주세요.");
        }

        User user = userOptional.get();
        log.info("사용자 정보: ID={}, Status={}, AccountLocked={}, Enabled={}",
                user.getId(), user.getStatus(), user.isAccountLocked(), user.isEnabled());

        if (user.isAccountLocked()) {
            log.warn("계정이 잠겨 있음: {}", username);
            throw new InternalAuthenticationServiceException("ACCOUNT_LOCKED|계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        if (!user.isEnabled()) {
            log.warn("계정이 비활성화됨: {}", username);
            throw new InternalAuthenticationServiceException("ACCOUNT_DISABLED|계정이 비활성화되었습니다. 관리자에게 문의하세요.");
        }

        if (user.getStatus() == User.Status.INACTIVE) {
            log.warn("계정 상태가 비활성 상태임: {}", username);
            throw new InternalAuthenticationServiceException("ACCOUNT_INACTIVE|계정이 비활성 상태입니다. 관리자에게 문의하세요.");
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
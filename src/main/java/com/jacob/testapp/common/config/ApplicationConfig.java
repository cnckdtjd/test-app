package com.jacob.testapp.common.config;

import com.jacob.testapp.common.security.AuthenticationRateLimitFilter;
import com.jacob.testapp.common.security.CustomAuthenticationFailureHandler;
import com.jacob.testapp.common.security.CustomAuthenticationSuccessHandler;
import com.jacob.testapp.common.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

/**
 * 애플리케이션 설정 클래스
 * 웹 설정과 보안 설정을 통합
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ApplicationConfig implements WebMvcConfigurer {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;

    // 생성자를 통한 의존성 주입, @Lazy 어노테이션 사용
    public ApplicationConfig(@Lazy CustomUserDetailsService userDetailsService,
                             @Lazy CustomAuthenticationFailureHandler authenticationFailureHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationFailureHandler = authenticationFailureHandler;
    }
    /**
     * Spring Security 설정
     */
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationRateLimitFilter authenticationRateLimitFilter) throws Exception {
        http
            .addFilterBefore(authenticationRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable) // 부하 테스트를 위해 CSRF 비활성화
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/register", "/login", "/users/register", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                .requestMatchers("/products/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login-process")
                .failureUrl("/login?error=true")
                .successHandler(customAuthenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
            )
            .requestCache(cache -> cache.disable());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        // 중요: 사용자가 발견되지 않을 때 예외를 숨기지 않도록 설정
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring MVC 설정
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestURIInterceptor());
    }

    /**
     * 모든 요청에 requestURI 변수를 모델에 추가하는 인터셉터
     */
    public static class RequestURIInterceptor implements HandlerInterceptor {
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
            if (modelAndView != null) {
                modelAndView.addObject("requestURI", request.getRequestURI());
            }
        }
    }
} 
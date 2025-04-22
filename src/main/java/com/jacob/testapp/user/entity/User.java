package com.jacob.testapp.user.entity;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "login_attempts")
    private int loginAttempts;

    @Column(name = "account_locked")
    private boolean accountLocked;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "cash_balance")
    private Long cashBalance;

    @Column(length = 255)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    /**
     * 사용자 역할
     */
    public enum Role {
        ROLE_USER, 
        ROLE_ADMIN;
        
        /**
         * Spring Security에서 사용할 역할 이름 반환 (ROLE_ 접두사 제거)
         */
        public String getAuthority() {
            return name().substring(5);
        }
    }

    /**
     * 사용자 상태
     */
    public enum Status {
        ACTIVE,   // 활성화 상태
        INACTIVE, // 비활성화 상태 
        LOCKED;   // 잠금 상태
        
        /**
         * 사용자가 활성 상태인지 확인
         */
        public boolean isActive() {
            return this == ACTIVE;
        }
        
        /**
         * 사용자가 잠긴 상태인지 확인
         */
        public boolean isLocked() {
            return this == LOCKED;
        }
    }

    /**
     * 계정이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status.isActive() && enabled && !accountLocked;
    }
    
    /**
     * 로그인 시도 증가
     * @return 최대 시도 횟수(5) 초과 여부
     */
    public boolean incrementLoginAttempts() {
        this.loginAttempts++;
        return this.loginAttempts >= 5;
    }
    
    /**
     * 로그인 시도 초기화
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
    }
    
    /**
     * 계정 잠금 처리
     */
    public void lock() {
        this.accountLocked = true;
        this.status = Status.LOCKED;
        this.lockTime = LocalDateTime.now();
    }
    
    /**
     * 계정 잠금 해제
     */
    public void unlock() {
        this.accountLocked = false;
        this.status = Status.ACTIVE;
        this.lockTime = null;
        resetLoginAttempts();
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }
} 
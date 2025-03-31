package com.jacob.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Role role;
    private boolean enabled;
    private int loginAttempts;
    private boolean accountLocked;
    private LocalDateTime lockTime;
    private Long cashBalance;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status;
    private LocalDateTime lastLoginAt;

    public enum Role {
        ROLE_USER, ROLE_ADMIN
    }

    public enum Status {
        ACTIVE, INACTIVE, LOCKED
    }
} 
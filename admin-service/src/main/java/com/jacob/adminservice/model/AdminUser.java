package com.jacob.adminservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter @Setter @ToString
public class AdminUser {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
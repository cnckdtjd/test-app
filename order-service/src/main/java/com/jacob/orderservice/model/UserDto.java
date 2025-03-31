package com.jacob.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String role;
    private boolean enabled;
    private Long cashBalance;
} 
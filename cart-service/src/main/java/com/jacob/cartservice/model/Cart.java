package com.jacob.cartservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter @Setter @ToString
public class Cart {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
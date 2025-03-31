package com.jacob.cartservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @ToString
public class CartItem {
    private Long id;
    private Long cartId;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private String productName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
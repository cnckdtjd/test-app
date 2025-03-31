package com.jacob.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private Double price;
    private Integer quantity;
    private String productOption;
} 
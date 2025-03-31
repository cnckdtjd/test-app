package com.jacob.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistory {
    private Long id;
    private Long orderId;
    private Order.OrderStatus statusFrom;
    private Order.OrderStatus statusTo;
    private String statusText;
    private String memo;
    private String createdBy;
    private LocalDateTime createdAt;
} 
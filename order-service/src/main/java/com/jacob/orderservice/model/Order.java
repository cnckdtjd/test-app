package com.jacob.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private OrderStatus status;
    private List<OrderItem> items = new ArrayList<>();
    private String paymentMethod;
    private String email;
    private String phoneNumber;
    private String receiverName;
    private String receiverPhone;
    private String receiverZipcode;
    private String receiverAddress1;
    private String receiverAddress2;
    private String deliveryMessage;
    private String trackingNumber;
    private String carrier;
    private String adminMemo;
    private Double totalAmount;
    private Double subtotalAmount;
    private Double shippingAmount;
    private Double discountAmount;
    private List<OrderHistory> history;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public enum OrderStatus {
        PENDING("결제대기"),
        PAID("결제완료"),
        SHIPPING("배송중"),
        COMPLETED("배송완료"),
        CANCELLED("취소됨"),
        DELETED("삭제됨");

        private final String displayName;

        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 주문번호 생성
     */
    public void generateOrderNumber() {
        if (orderNumber == null) {
            orderNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
    }

    /**
     * 주문 항목 추가
     */
    public void addItem(OrderItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        recalculateTotalAmount();
    }

    /**
     * 주문 항목 제거
     */
    public void removeItem(OrderItem item) {
        if (items != null) {
            items.remove(item);
            recalculateTotalAmount();
        }
    }

    /**
     * 주문 총액 재계산
     */
    private void recalculateTotalAmount() {
        double itemTotal = 0.0;
        if (items != null) {
            itemTotal = items.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
        }
        
        // 배송비와 할인액 적용
        if (subtotalAmount == null) subtotalAmount = itemTotal;
        if (shippingAmount == null) shippingAmount = 0.0;
        if (discountAmount == null) discountAmount = 0.0;
        
        totalAmount = subtotalAmount + shippingAmount - discountAmount;
    }
} 
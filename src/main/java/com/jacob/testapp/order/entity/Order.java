package com.jacob.testapp.order.entity;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private String paymentMethod;

    @Column
    private String email;

    @Column
    private String phoneNumber;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhone;

    @Column(nullable = false)
    private String receiverZipcode;

    @Column(nullable = false)
    private String receiverAddress1;

    @Column
    private String receiverAddress2;

    @Column
    private String deliveryMessage;

    @Column
    private String trackingNumber;

    @Column
    private String carrier;

    @Column
    private String adminMemo;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double subtotalAmount;

    @Column(nullable = false)
    private Double shippingAmount;

    @Column(nullable = false)
    private Double discountAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> history = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Getter
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

    @PrePersist
    public void prePersist() {
        if (orderNumber == null) {
            orderNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotalAmount();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotalAmount();
    }

    private void recalculateTotalAmount() {
        totalAmount = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        
        // 배송비와 할인액 적용
        if (subtotalAmount == null) subtotalAmount = totalAmount;
        if (shippingAmount == null) shippingAmount = 0.0;
        if (discountAmount == null) discountAmount = 0.0;
        
        totalAmount = subtotalAmount + shippingAmount - discountAmount;
    }
} 
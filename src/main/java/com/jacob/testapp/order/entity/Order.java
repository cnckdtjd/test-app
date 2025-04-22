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

/**
 * 주문 엔티티
 * 사용자의 주문 정보와 주문 상품 목록을 관리
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder.Default
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

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> history = new ArrayList<>();

    /**
     * 주문 상태 열거형
     */
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
    }

    /**
     * 엔티티 생성 전 실행되는 메서드
     * 주문번호가 없는 경우 자동 생성
     */
    @PrePersist
    public void prePersist() {
        if (orderNumber == null) {
            orderNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
        
        // 초기 값이 null일 경우 기본값 설정
        if (subtotalAmount == null) subtotalAmount = 0.0;
        if (shippingAmount == null) shippingAmount = 0.0;
        if (discountAmount == null) discountAmount = 0.0;
        if (totalAmount == null) recalculateTotalAmount();
    }

    /**
     * 주문에 상품 추가
     * @param item 추가할 주문 상품
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotalAmount();
    }

    /**
     * 주문에서 상품 제거
     * @param item 제거할 주문 상품
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotalAmount();
    }

    /**
     * 주문 상태 변경
     * @param newStatus 변경할 상태
     * @param createdBy 변경자
     * @param memo 변경 메모
     */
    public OrderHistory changeStatus(OrderStatus newStatus, String createdBy, String memo) {
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        
        OrderHistory historyEntry = OrderHistory.createHistory(this, oldStatus, newStatus, createdBy, memo);
        this.history.add(historyEntry);
        
        return historyEntry;
    }
    
    /**
     * 주문 취소 가능 여부 확인
     * @return 취소 가능하면 true
     */
    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PAID;
    }
    
    /**
     * 주문 삭제 가능 여부 확인
     * @return 삭제 가능하면 true
     */
    public boolean isDeletable() {
        return this.status != OrderStatus.SHIPPING && this.status != OrderStatus.COMPLETED;
    }

    /**
     * 주문 총액 다시 계산
     */
    public void recalculateTotalAmount() {
        if (items != null && !items.isEmpty()) {
            double itemsTotal = items.stream()
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();
            
            this.subtotalAmount = itemsTotal;
        }
        
        // 배송비와 할인액 적용
        this.totalAmount = (subtotalAmount != null ? subtotalAmount : 0.0) 
                         + (shippingAmount != null ? shippingAmount : 0.0) 
                         - (discountAmount != null ? discountAmount : 0.0);
    }
} 

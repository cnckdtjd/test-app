package com.jacob.testapp.order.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

/**
 * 주문 상태 변경 이력 엔티티
 * 주문의 상태 변경을 추적하기 위한 엔티티
 */
@Entity
@Table(name = "order_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from")
    private Order.OrderStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to")
    private Order.OrderStatus statusTo;

    @Column(name = "status_text")
    private String statusText;

    @Column
    private String memo;

    @Column(name = "created_by")
    private String createdBy;
    
    /**
     * 주문 상태 변경 이력 생성
     * @param order 주문
     * @param statusFrom 이전 상태
     * @param statusTo 변경된 상태
     * @param createdBy 변경자
     * @param memo 메모
     * @return 생성된 주문 이력
     */
    public static OrderHistory createHistory(Order order, Order.OrderStatus statusFrom, 
                                           Order.OrderStatus statusTo, String createdBy, String memo) {
        return OrderHistory.builder()
                .order(order)
                .statusFrom(statusFrom)
                .statusTo(statusTo)
                .statusText(statusTo.getDisplayName())
                .createdBy(createdBy)
                .memo(memo)
                .build();
    }
} 
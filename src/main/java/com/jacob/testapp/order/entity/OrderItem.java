package com.jacob.testapp.order.entity;

import com.jacob.testapp.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문 아이템 엔티티
 * 주문에 포함된 개별 상품 항목을 나타냄
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantity;

    @Column
    private String productOption;
    
    /**
     * 주문 항목의 총 가격 계산
     * @return 단가 × 수량
     */
    public double getTotalPrice() {
        return price * quantity;
    }
    
    /**
     * 주문 항목을 주문에 연결
     * @param order 연결할 주문
     */
    public void setOrder(Order order) {
        this.order = order;
    }
} 
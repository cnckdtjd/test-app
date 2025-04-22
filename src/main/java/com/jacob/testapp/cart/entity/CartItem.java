package com.jacob.testapp.cart.entity;

import com.jacob.testapp.product.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 항목의 총 가격 계산
     * @return 상품 가격 * 수량
     */
    public double getTotalPrice() {
        if (product == null) {
            return 0;
        }
        return product.getPrice().doubleValue() * quantity;
    }
    
    /**
     * 수량 증가
     */
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }
    
    /**
     * 수량 감소
     * @return 수량이 0 이하가 되면 true 반환
     */
    public boolean decreaseQuantity(int amount) {
        this.quantity -= amount;
        return this.quantity <= 0;
    }
} 
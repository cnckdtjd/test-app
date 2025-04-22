package com.jacob.testapp.cart.entity;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * 장바구니에 상품 추가
     */
    public void addProduct(Product product, int quantity) {
        findCartItem(product.getId())
            .ifPresentOrElse(
                // 이미 존재하는 경우 수량 증가
                item -> item.setQuantity(item.getQuantity() + quantity),
                // 존재하지 않는 경우 새 항목 추가
                () -> {
                    CartItem cartItem = CartItem.builder()
                        .cart(this)
                        .product(product)
                        .quantity(quantity)
                        .build();
                    cartItems.add(cartItem);
                }
            );
        recalculateTotalPrice();
    }
    
    /**
     * 장바구니에서 상품 제거
     */
    public void removeProduct(Long productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
        recalculateTotalPrice();
    }
    
    /**
     * 상품 수량 업데이트
     */
    public void updateProductQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeProduct(productId);
            return;
        }
        
        findCartItem(productId).ifPresent(item -> {
            item.setQuantity(quantity);
            recalculateTotalPrice();
        });
    }
    
    /**
     * 장바구니 비우기
     */
    public void clear() {
        cartItems.clear();
        totalPrice = BigDecimal.ZERO;
    }

    /**
     * 총 가격 다시 계산
     */
    public void recalculateTotalPrice() {
        totalPrice = BigDecimal.valueOf(
            cartItems.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum()
        );
    }

    /**
     * 총 수량 계산
     */
    public int getTotalQuantity() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    /**
     * ID로 장바구니 항목 찾기
     */
    private Optional<CartItem> findCartItem(Long productId) {
        return cartItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }
} 
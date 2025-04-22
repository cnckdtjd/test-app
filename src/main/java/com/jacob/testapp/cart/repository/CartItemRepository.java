package com.jacob.testapp.cart.repository;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.entity.CartItem;
import com.jacob.testapp.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * 장바구니에 속한 모든 아이템 조회
     */
    List<CartItem> findByCart(Cart cart);
    
    /**
     * 장바구니와 상품으로 아이템 조회
     */
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    
    /**
     * 장바구니 ID와 상품 ID로 아이템 조회
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    /**
     * 장바구니 아이템 수량 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE CartItem ci SET ci.quantity = :quantity WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void updateQuantity(
        @Param("cartId") Long cartId, 
        @Param("productId") Long productId, 
        @Param("quantity") int quantity
    );
    
    /**
     * 장바구니와 상품으로 아이템 삭제
     */
    @Modifying
    @Transactional
    void deleteByCartAndProduct(Cart cart, Product product);
    
    /**
     * 장바구니 ID와 상품 ID로 아이템 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
    
    /**
     * 장바구니의 모든 아이템 삭제
     */
    @Modifying
    @Transactional
    void deleteByCart(Cart cart);
    
    /**
     * 장바구니 ID로 모든 아이템 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
} 
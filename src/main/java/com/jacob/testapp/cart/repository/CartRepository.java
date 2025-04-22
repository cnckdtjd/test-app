package com.jacob.testapp.cart.repository;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 사용자의 장바구니를 조회하고 장바구니 항목과 상품을 함께 로드
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems ci LEFT JOIN FETCH ci.product WHERE c.user = :user")
    Optional<Cart> findByUserWithItems(@Param("user") User user);
    
    /**
     * 사용자 ID로 장바구니를 조회하고 장바구니 항목과 상품을 함께 로드
     */
    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    /**
     * 사용자 ID로 장바구니만 조회 (항목 없이)
     */
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);
    
    /**
     * 비관적 락을 사용하여 장바구니 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.id = :id")
    Optional<Cart> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * 낙관적 락을 사용하여 사용자 ID로 장바구니 조회
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithOptimisticLock(@Param("userId") Long userId);
    
    /**
     * 사용자로 장바구니 조회 (기본 메서드)
     */
    default Optional<Cart> findByUser(User user) {
        return findByUserWithItems(user);
    }
} 
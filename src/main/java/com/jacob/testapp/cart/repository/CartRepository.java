package com.jacob.testapp.cart.repository;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems ci LEFT JOIN FETCH ci.product WHERE c.user = ?1")
    Optional<Cart> findByUser(User user);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.id = ?1")
    Optional<Cart> findByIdWithPessimisticLock(Long id);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Cart c WHERE c.user.id = ?1")
    Optional<Cart> findByUserIdWithOptimisticLock(Long userId);
    
    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product WHERE c.user.id = ?1")
    Optional<Cart> findByUserIdWithItems(Long userId);

    @Query("SELECT c FROM Cart c WHERE c.user.id = ?1")
    Optional<Cart> findByUserIdNoItems(Long userId);
} 
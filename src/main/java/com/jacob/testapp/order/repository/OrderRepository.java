package com.jacob.testapp.order.repository;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);
    
    Page<Order> findByUser(User user, Pageable pageable);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = ?1")
    Optional<Order> findByIdWithPessimisticLock(Long id);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o WHERE o.id = ?1")
    Optional<Order> findByIdWithOptimisticLock(Long id);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = ?1")
    Optional<Order> findByIdWithItems(Long id);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.orderNumber = ?1")
    Optional<Order> findByOrderNumberWithItems(String orderNumber);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<Order> findByOrderNumberContaining(String orderNumber, Pageable pageable);
    
    Page<Order> findByOrderNumberContainingAndStatus(String orderNumber, Order.OrderStatus status, Pageable pageable);
    
    Page<Order> findByOrderNumberContainingAndCreatedAtBetween(String orderNumber, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<Order> findByOrderNumberContainingAndStatusAndCreatedAtBetween(String orderNumber, Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<Order> findByStatusAndCreatedAtBetween(Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    long countByStatus(Order.OrderStatus status);
    
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Order> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Order> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double sumTotalAmount();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Double sumTotalAmountByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.history WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);
} 
package com.jacob.testapp.order.repository;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // 기본 조회 메서드
    List<Order> findByUser(User user);
    Page<Order> findByUser(User user, Pageable pageable);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatus(Order.OrderStatus status);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    // 상태별 주문 개수 집계
    long countByStatus(Order.OrderStatus status);
    
    // 기간별 주문 개수 집계
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    long countByStatusAndCreatedAtBetween(Order.OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    // 락 관련 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithPessimisticLock(@Param("id") Long id);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithOptimisticLock(@Param("id") Long id);
    
    // 연관 엔티티 패치 조인
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItemsOnly(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.history WHERE o.id = :id")
    Optional<Order> findByIdWithHistory(@Param("id") Long id);
    
    // 매출 집계 관련 쿼리
    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double sumTotalAmount();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Double sumTotalAmountByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 상태 필터링 (특정 상태 제외)
    Page<Order> findByUserAndStatusNot(User user, Order.OrderStatus status, Pageable pageable);
    List<Order> findByUserAndStatusNotOrderByCreatedAtDesc(User user, Order.OrderStatus status);
    Page<Order> findByStatusNot(Order.OrderStatus status, Pageable pageable);
    
    // 기간 필터링
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // 주문번호 검색
    Page<Order> findByOrderNumberContaining(String orderNumber, Pageable pageable);
    
    // 기본 정렬 메서드
    List<Order> findAllByOrderByCreatedAtDesc();
} 
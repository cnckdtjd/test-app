package com.jacob.testapp.order.repository;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.entity.OrderHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    
    /**
     * 특정 주문의 이력을 생성일시 기준 내림차순으로 조회
     */
    List<OrderHistory> findByOrderOrderByCreatedAtDesc(Order order);
    
    /**
     * 특정 기간 내의 주문 이력 페이징 조회
     */
    Page<OrderHistory> findByCreatedAtBetween(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable);
    
    /**
     * 특정 주문의 가장 최근 이력 조회
     */
    @Query(value = "SELECT * FROM order_history WHERE order_id = :orderId " +
           "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    OrderHistory findLatestHistoryByOrderId(@Param("orderId") Long orderId);
} 
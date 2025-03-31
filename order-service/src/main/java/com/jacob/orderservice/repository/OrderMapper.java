package com.jacob.orderservice.repository;

import com.jacob.orderservice.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    
    // 조회
    Order findById(Long id);
    Order findByOrderNumber(String orderNumber);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAll();
    List<Order> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    // 상태별 조회
    List<Order> findByStatus(@Param("status") Order.OrderStatus status);
    List<Order> findByStatusWithPagination(
            @Param("status") Order.OrderStatus status,
            @Param("offset") int offset,
            @Param("limit") int limit);
    
    // 사용자별 조회
    List<Order> findByUserId(@Param("userId") Long userId);
    List<Order> findByUserIdWithPagination(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);
    
    // 검색
    List<Order> findByOrderNumberContaining(
            @Param("orderNumber") String orderNumber,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    List<Order> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    List<Order> findByOrderNumberContainingAndStatus(
            @Param("orderNumber") String orderNumber,
            @Param("status") Order.OrderStatus status,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    List<Order> findByStatusAndCreatedAtBetween(
            @Param("status") Order.OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    List<Order> findByOrderNumberContainingAndCreatedAtBetween(
            @Param("orderNumber") String orderNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    List<Order> findByOrderNumberContainingAndStatusAndCreatedAtBetween(
            @Param("orderNumber") String orderNumber,
            @Param("status") Order.OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);
            
    // 카운트
    long countAll();
    long countByStatus(Order.OrderStatus status);
    long countByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    long countByStatusAndCreatedAtBetween(
            @Param("status") Order.OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // 통계
    Double sumTotalAmount();
    Double sumTotalAmountByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // 생성 및 수정
    int save(Order order);
    Long saveAndGetId(Order order);
    int update(Order order);
    int updateStatus(@Param("id") Long id, @Param("status") Order.OrderStatus status);
    
    // 삭제
    int deleteById(Long id);
    int markAsDeleted(@Param("id") Long id);
    
    // 특정 상태가 아닌 주문 조회
    List<Order> findByUserIdAndStatusNot(
            @Param("userId") Long userId,
            @Param("status") Order.OrderStatus status,
            @Param("offset") int offset,
            @Param("limit") int limit);
} 
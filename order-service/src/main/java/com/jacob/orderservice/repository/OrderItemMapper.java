package com.jacob.orderservice.repository;

import com.jacob.orderservice.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    
    // 조회
    OrderItem findById(Long id);
    List<OrderItem> findByOrderId(Long orderId);
    
    // 생성 및 수정
    int save(OrderItem orderItem);
    int update(OrderItem orderItem);
    int saveAll(@Param("orderItems") List<OrderItem> orderItems);
    
    // 삭제
    int deleteById(Long id);
    int deleteByOrderId(Long orderId);
} 
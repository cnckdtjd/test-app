package com.jacob.orderservice.repository;

import com.jacob.orderservice.model.OrderHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderHistoryMapper {
    
    // 조회
    OrderHistory findById(Long id);
    List<OrderHistory> findByOrderId(Long orderId);
    
    // 생성
    int save(OrderHistory orderHistory);
    
    // 삭제
    int deleteById(Long id);
    int deleteByOrderId(Long orderId);
} 
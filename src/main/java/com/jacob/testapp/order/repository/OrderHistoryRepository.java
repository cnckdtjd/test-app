package com.jacob.testapp.order.repository;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    List<OrderHistory> findByOrderOrderByCreatedAtDesc(Order order);
} 
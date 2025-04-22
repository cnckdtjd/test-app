package com.jacob.testapp.order.repository;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.entity.OrderItem;
import com.jacob.testapp.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 주문에 포함된 모든 상품 항목 조회
     */
    List<OrderItem> findByOrder(Order order);
    
    /**
     * 특정 상품이 포함된 모든 주문 항목 조회
     */
    List<OrderItem> findByProduct(Product product);
    
    /**
     * 가장 많이 팔린 상품 순으로 조회
     */
    @Query("SELECT oi.product.id as productId, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "GROUP BY oi.product.id " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts();
    
    /**
     * 특정 상품 목록의 판매량 조회
     */
    @Query("SELECT oi.product.id as productId, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "WHERE oi.product.id IN :productIds " +
           "GROUP BY oi.product.id")
    List<Object[]> findSalesByProductIds(@Param("productIds") List<Long> productIds);
} 
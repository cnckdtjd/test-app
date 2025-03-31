package com.jacob.orderservice.controller;

import com.jacob.orderservice.model.Order;
import com.jacob.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    
    /**
     * 주문 생성
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderService.OrderRequest request,
            @RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Order order = orderService.createOrder(request, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Failed to create order", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 주문 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            Order order = orderService.findById(id);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Failed to get order", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 주문 번호로 주문 조회
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            Order order = orderService.findByOrderNumber(orderNumber);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Failed to get order by number", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 사용자 주문 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Order> orders = orderService.findByUserId(userId, page, size);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Failed to get user orders", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 최근 주문 목록 조회
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<?> getRecentUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Order> orders = orderService.getRecentOrdersByUser(userId, limit);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Failed to get recent user orders", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 주문 상태 변경
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            @RequestHeader(value = "X-Updated-By", defaultValue = "SYSTEM") String updatedBy) {
        try {
            Order order = orderService.updateOrderStatus(id, status, updatedBy);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to update order status", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 주문 취소
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-Updated-By", defaultValue = "USER") String updatedBy) {
        try {
            Order order = orderService.updateOrderStatus(id, Order.OrderStatus.CANCELLED, updatedBy);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 주문 삭제 (논리적 삭제)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        try {
            Order order = orderService.markAsDeleted(id);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to delete order", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 주문 매출 통계
     */
    @GetMapping("/sales")
    public ResponseEntity<?> getSalesStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 전체 매출
            Double totalSales = orderService.getTotalSales();
            statistics.put("totalSales", totalSales);
            
            // 특정 기간 매출 (요청 시)
            if (startDate != null && endDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                LocalDateTime start = LocalDateTime.parse(startDate, formatter);
                LocalDateTime end = LocalDateTime.parse(endDate, formatter);
                
                Double periodSales = orderService.getTotalSalesForPeriod(start, end);
                statistics.put("periodStartDate", startDate);
                statistics.put("periodEndDate", endDate);
                statistics.put("periodSales", periodSales);
            }
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Failed to get sales statistics", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 
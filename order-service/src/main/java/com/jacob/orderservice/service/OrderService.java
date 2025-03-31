package com.jacob.orderservice.service;

import com.jacob.orderservice.client.ProductServiceClient;
import com.jacob.orderservice.client.UserServiceClient;
import com.jacob.orderservice.model.*;
import com.jacob.orderservice.repository.OrderHistoryMapper;
import com.jacob.orderservice.repository.OrderItemMapper;
import com.jacob.orderservice.repository.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderHistoryMapper orderHistoryMapper;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    private final Random random = new Random();

    /**
     * 모든 주문 목록 조회
     */
    public List<Order> findAll() {
        return orderMapper.findAll();
    }

    /**
     * 페이지네이션이 적용된 주문 목록 조회
     */
    public List<Order> findAllWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return orderMapper.findAllWithPagination(offset, size);
    }

    /**
     * 주문 ID로 주문 조회
     */
    public Order findById(Long id) {
        Order order = orderMapper.findById(id);
        if (order != null) {
            List<OrderItem> items = orderItemMapper.findByOrderId(id);
            order.setItems(items);
            List<OrderHistory> history = orderHistoryMapper.findByOrderId(id);
            order.setHistory(history);
        }
        return order;
    }

    /**
     * 주문 번호로 주문 조회
     */
    public Order findByOrderNumber(String orderNumber) {
        Order order = orderMapper.findByOrderNumber(orderNumber);
        if (order != null) {
            List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
            order.setItems(items);
            List<OrderHistory> history = orderHistoryMapper.findByOrderId(order.getId());
            order.setHistory(history);
        }
        return order;
    }

    /**
     * 사용자 ID로 주문 목록 조회
     */
    public List<Order> findByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 사용자 ID로 주문 목록 조회 (페이지네이션)
     */
    public List<Order> findByUserId(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return orderMapper.findByUserIdWithPagination(userId, offset, size);
    }

    /**
     * 주문 상태별 주문 목록 조회
     */
    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderMapper.findByStatus(status);
    }

    /**
     * 주문 상태별 주문 목록 조회 (페이지네이션)
     */
    public List<Order> findByStatus(Order.OrderStatus status, int page, int size) {
        int offset = (page - 1) * size;
        return orderMapper.findByStatusWithPagination(status, offset, size);
    }

    /**
     * 주문 생성
     */
    @Transactional
    public Order createOrder(OrderRequest orderRequest, String token) {
        // 사용자 정보 조회
        UserDto user = userServiceClient.getUserById(orderRequest.getUserId(), token).block();
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // 주문 정보 생성
        Order order = new Order();
        order.generateOrderNumber();
        order.setUserId(user.getId());
        order.setUserEmail(user.getEmail());
        order.setUserName(user.getName());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setEmail(user.getEmail());
        order.setPhoneNumber(user.getPhone());
        order.setReceiverName(orderRequest.getReceiverName() != null ? orderRequest.getReceiverName() : user.getName());
        order.setReceiverPhone(orderRequest.getReceiverPhone() != null ? orderRequest.getReceiverPhone() : user.getPhone());
        order.setReceiverZipcode(orderRequest.getReceiverZipcode());
        order.setReceiverAddress1(orderRequest.getReceiverAddress1());
        order.setReceiverAddress2(orderRequest.getReceiverAddress2());
        order.setDeliveryMessage(orderRequest.getDeliveryMessage());
        
        // 주문 금액 계산 변수 초기화
        double totalAmount = 0.0;
        
        // 현재 시간 설정
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setVersion(0L);
        
        // 주문 저장
        Long orderId = orderMapper.saveAndGetId(order);
        order.setId(orderId);
        
        // 주문 아이템 생성 및 저장
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getItems()) {
            // 상품 정보 조회
            ProductDto product = productServiceClient.getProductById(itemRequest.getProductId()).block();
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemRequest.getProductId());
            }
            
            // 재고 감소
            Boolean decreased = productServiceClient.decreaseStock(
                    product.getId(), itemRequest.getQuantity()).block();
            if (decreased == null || !decreased) {
                throw new IllegalStateException("Failed to decrease stock for product: " + product.getId());
            }
            
            // 주문 항목 생성 및 추가
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImageUrl());
            orderItem.setPrice(product.getPrice().doubleValue());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setProductOption(itemRequest.getProductOption());
            
            orderItemMapper.save(orderItem);
            orderItems.add(orderItem);
            
            // 주문 금액 계산
            totalAmount += product.getPrice().doubleValue() * itemRequest.getQuantity();
        }
        
        // 주문 금액 설정
        order.setSubtotalAmount(totalAmount);
        order.setShippingAmount(0.0);
        order.setDiscountAmount(0.0);
        order.setTotalAmount(totalAmount);
        
        // 주문 업데이트
        orderMapper.update(order);
        
        // 주문 상태 이력 저장
        OrderHistory history = new OrderHistory();
        history.setOrderId(orderId);
        history.setStatusTo(Order.OrderStatus.PENDING);
        history.setStatusText("주문이 생성되었습니다.");
        history.setCreatedBy("SYSTEM");
        history.setCreatedAt(now);
        orderHistoryMapper.save(history);
        
        // 완성된 주문 반환
        order.setItems(orderItems);
        return order;
    }

    /**
     * 주문 상태 업데이트
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String updatedBy) {
        Order order = findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        
        Order.OrderStatus oldStatus = order.getStatus();
        
        // 상태 변경
        if (oldStatus != newStatus) {
            orderMapper.updateStatus(orderId, newStatus);
            order.setStatus(newStatus);
            
            // 주문 이력 저장
            OrderHistory history = new OrderHistory();
            history.setOrderId(orderId);
            history.setStatusFrom(oldStatus);
            history.setStatusTo(newStatus);
            history.setStatusText(generateStatusChangeText(oldStatus, newStatus));
            history.setCreatedBy(updatedBy);
            history.setCreatedAt(LocalDateTime.now());
            orderHistoryMapper.save(history);
            
            // 주문 취소인 경우 재고 복원
            if (newStatus == Order.OrderStatus.CANCELLED && 
                    (oldStatus == Order.OrderStatus.PENDING || oldStatus == Order.OrderStatus.PAID)) {
                restoreStockForCancelledOrder(order);
            }
        }
        
        return findById(orderId);
    }

    /**
     * 취소된 주문의 재고 복원
     */
    private void restoreStockForCancelledOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            productServiceClient.increaseStock(item.getProductId(), item.getQuantity()).block();
        }
    }

    /**
     * 상태 변경 텍스트 생성
     */
    private String generateStatusChangeText(Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        return String.format("주문 상태가 '%s'에서 '%s'로 변경되었습니다.", 
                oldStatus.getDisplayName(), newStatus.getDisplayName());
    }

    /**
     * 주문 삭제
     */
    @Transactional
    public void delete(Long id) {
        // 연관된 주문 항목 및 이력 삭제
        orderItemMapper.deleteByOrderId(id);
        orderHistoryMapper.deleteByOrderId(id);
        
        // 주문 삭제
        orderMapper.deleteById(id);
    }

    /**
     * 주문 논리적 삭제 (상태만 DELETED로 변경)
     */
    @Transactional
    public Order markAsDeleted(Long id) {
        return updateOrderStatus(id, Order.OrderStatus.DELETED, "SYSTEM");
    }

    /**
     * 특정 기간의 주문 매출 합계 조회
     */
    public Double getTotalSalesForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return orderMapper.sumTotalAmountByCreatedAtBetween(startDate, endDate);
    }

    /**
     * 전체 주문 매출 합계 조회
     */
    public Double getTotalSales() {
        return orderMapper.sumTotalAmount();
    }

    /**
     * 특정 사용자의 최근 주문 목록 조회
     */
    public List<Order> getRecentOrdersByUser(Long userId, int limit) {
        return orderMapper.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 요청 객체
     */
    @Data
    public static class OrderRequest {
        private Long userId;
        private String paymentMethod;
        private String receiverName;
        private String receiverPhone;
        private String receiverZipcode;
        private String receiverAddress1;
        private String receiverAddress2;
        private String deliveryMessage;
        private List<OrderItemRequest> items;
        
        @Data
        public static class OrderItemRequest {
            private Long productId;
            private Integer quantity;
            private String productOption;
        }
    }
} 
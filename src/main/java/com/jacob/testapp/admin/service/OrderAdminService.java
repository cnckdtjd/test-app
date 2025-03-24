package com.jacob.testapp.admin.service;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.entity.OrderHistory;
import com.jacob.testapp.order.entity.OrderItem;
import com.jacob.testapp.order.repository.OrderHistoryRepository;
import com.jacob.testapp.order.repository.OrderRepository;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자용 주문 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ProductRepository productRepository;

    /**
     * 모든 주문 조회
     */
    @Transactional(readOnly = true)
    public Page<Order> findAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * 주문번호로 주문 검색
     */
    @Transactional(readOnly = true)
    public Page<Order> findByOrderNumber(String orderNumber, Pageable pageable) {
        return orderRepository.findByOrderNumberContaining(orderNumber, pageable);
    }

    /**
     * 상태로 주문 검색
     */
    @Transactional(readOnly = true)
    public Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    /**
     * 날짜 범위로 주문 검색
     */
    @Transactional(readOnly = true)
    public Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }

    /**
     * 조건별 주문 검색 (복합 조건)
     */
    @Transactional(readOnly = true)
    public Page<Order> searchOrders(String orderNumber, Order.OrderStatus status, 
                                   LocalDate startDate, LocalDate endDate, Pageable pageable) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        
        if (orderNumber != null && !orderNumber.isEmpty()) {
            if (status != null && startDateTime != null && endDateTime != null) {
                return orderRepository.findByOrderNumberContainingAndStatusAndCreatedAtBetween(
                    orderNumber, status, startDateTime, endDateTime, pageable);
            } else if (status != null) {
                return orderRepository.findByOrderNumberContainingAndStatus(orderNumber, status, pageable);
            } else if (startDateTime != null && endDateTime != null) {
                return orderRepository.findByOrderNumberContainingAndCreatedAtBetween(
                    orderNumber, startDateTime, endDateTime, pageable);
            } else {
                return orderRepository.findByOrderNumberContaining(orderNumber, pageable);
            }
        } else if (status != null) {
            if (startDateTime != null && endDateTime != null) {
                return orderRepository.findByStatusAndCreatedAtBetween(status, startDateTime, endDateTime, pageable);
            } else {
                return orderRepository.findByStatus(status, pageable);
            }
        } else if (startDateTime != null && endDateTime != null) {
            return orderRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable);
        } else {
            return orderRepository.findAll(pageable);
        }
    }

    /**
     * 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String memo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        
        Order.OrderStatus oldStatus = order.getStatus();
        
        // 상태가 변경되었을 때만 이력 추가 및 처리
        if (oldStatus != newStatus) {
            log.info("주문 상태 변경: {} -> {}, 주문 ID: {}", oldStatus, newStatus, orderId);
            
            // 주문 상태 변경
            order.setStatus(newStatus);
            
            // 주문 이력 추가
            OrderHistory history = new OrderHistory();
            history.setOrder(order);
            history.setStatusFrom(oldStatus);
            history.setStatusTo(newStatus);
            history.setStatusText(getStatusChangeText(oldStatus, newStatus));
            history.setMemo(memo);
            history.setCreatedBy("관리자");
            
            orderHistoryRepository.save(history);
            
            // 상태가 취소로 변경된 경우 재고 원복
            if (newStatus == Order.OrderStatus.CANCELLED) {
                restoreProductStock(order);
            }
        }
        
        return orderRepository.save(order);
    }

    /**
     * 배송 정보 업데이트
     */
    @Transactional
    public Order updateShippingInfo(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        
        // 운송장 번호가 입력되고 배송중 상태가 아니라면 배송중으로 상태 변경
        if (trackingNumber != null && !trackingNumber.isEmpty() && order.getStatus() != Order.OrderStatus.SHIPPING) {
            updateOrderStatus(orderId, Order.OrderStatus.SHIPPING, "배송 정보 등록으로 인한 자동 상태 변경");
        }
        
        return orderRepository.save(order);
    }

    /**
     * 관리자 메모 업데이트
     */
    @Transactional
    public Order updateAdminMemo(Long orderId, String adminMemo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        
        order.setAdminMemo(adminMemo);
        return orderRepository.save(order);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public Order cancelOrder(Long orderId, String cancelReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 주문은 취소할 수 없습니다.");
        }
        
        // 주문 상태 변경
        updateOrderStatus(orderId, Order.OrderStatus.CANCELLED, cancelReason);
        
        return order;
    }
    
    /**
     * 주문 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 총 주문 수
        long totalOrders = orderRepository.count();
        statistics.put("totalOrders", totalOrders);
        
        // 상태별 주문 수
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            ordersByStatus.put(status.name(), count);
        }
        statistics.put("ordersByStatus", ordersByStatus);
        
        // 오늘 주문 수
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        long todayOrders = orderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        statistics.put("todayOrders", todayOrders);
        
        // 이번 달 주문 수
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);
        long monthOrders = orderRepository.countByCreatedAtBetween(monthStart, monthEnd);
        statistics.put("monthOrders", monthOrders);
        
        // 총 매출
        Double totalSales = orderRepository.sumTotalAmount();
        statistics.put("totalSales", totalSales != null ? totalSales : 0);
        
        // 이번 달 매출
        Double monthSales = orderRepository.sumTotalAmountByCreatedAtBetween(monthStart, monthEnd);
        statistics.put("monthSales", monthSales != null ? monthSales : 0);
        
        return statistics;
    }
    
    /**
     * 주문 엑셀 내보내기를 위한 데이터 조회
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersForExport(LocalDateTime startDate, LocalDateTime endDate, Order.OrderStatus status) {
        if (status != null) {
            if (startDate != null && endDate != null) {
                return orderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                    status, startDate, endDate);
            } else {
                return orderRepository.findByStatusOrderByCreatedAtDesc(status);
            }
        } else if (startDate != null && endDate != null) {
            return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
        } else {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
    }
    
    /**
     * 상태 변경 메시지 생성
     */
    private String getStatusChangeText(Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        String oldStatusText = getStatusDisplayName(oldStatus);
        String newStatusText = getStatusDisplayName(newStatus);
        
        return oldStatusText + " → " + newStatusText;
    }
    
    /**
     * 상태 표시명 조회
     */
    private String getStatusDisplayName(Order.OrderStatus status) {
        switch (status) {
            case PENDING:
                return "결제대기";
            case PAID:
                return "결제완료";
            case SHIPPING:
                return "배송중";
            case COMPLETED:
                return "배송완료";
            case CANCELLED:
                return "주문취소";
            default:
                return status.name();
        }
    }
    
    /**
     * 주문 취소 시 상품 재고 원복
     */
    private void restoreProductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                int quantity = item.getQuantity();
                log.info("주문 취소로 재고 복구: 상품 ID={}, 수량={}", product.getId(), quantity);
                
                // 상품 재고 업데이트
                productRepository.increaseStock(product.getId(), quantity);
            }
        }
    }
} 
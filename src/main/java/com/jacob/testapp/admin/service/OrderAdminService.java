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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.jacob.testapp.admin.service.StatisticsUtil.*;

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
        
        // 명시적으로 DELETED 상태를 요청하지 않으면 제외 (기본 검색 조건)
        boolean excludeDeleted = status != Order.OrderStatus.DELETED;
        
        return findOrdersWithSpecification(orderNumber, status, startDateTime, endDateTime, excludeDeleted, pageable);
    }
    
    /**
     * 주문 검색을 위한 Specification 생성 및 조회
     */
    private Page<Order> findOrdersWithSpecification(String orderNumber, Order.OrderStatus status, 
                                                 LocalDateTime startDateTime, LocalDateTime endDateTime, 
                                                 boolean excludeDeleted, Pageable pageable) {
        Specification<Order> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 주문번호 검색 조건
            if (orderNumber != null && !orderNumber.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("orderNumber")), 
                    "%" + orderNumber.toLowerCase() + "%"
                ));
            }
            
            // 상태 검색 조건
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else if (excludeDeleted) {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), Order.OrderStatus.DELETED));
            }
            
            // 시작일 검색 조건
            if (startDateTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), startDateTime
                ));
            }
            
            // 종료일 검색 조건
            if (endDateTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), endDateTime
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return orderRepository.findAll(spec, pageable);
    }

    /**
     * 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long id) {
        // 먼저 items와 함께 주문을 조회합니다
        Order order = orderRepository.findByIdWithItemsOnly(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));
        
        // 그 후 history를 조회합니다
        orderRepository.findByIdWithHistory(id).ifPresent(orderWithHistory -> {
            // 이미 로드된 주문 객체에 history 컬렉션을 설정합니다
            order.setHistory(orderWithHistory.getHistory());
        });
        
        return order;
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, String memo) {
        Order order = findOrderOrThrow(orderId);
        
        Order.OrderStatus oldStatus = order.getStatus();
        
        // 상태가 변경되었을 때만 이력 추가 및 처리
        if (oldStatus != newStatus) {
            log.info("주문 상태 변경: {} -> {}, 주문 ID: {}", oldStatus, newStatus, orderId);
            
            // 주문 이력 생성 및 상태 변경 (Order의 changeStatus 메서드 활용)
            OrderHistory history = order.changeStatus(newStatus, "관리자", memo);
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
        Order order = findOrderOrThrow(orderId);
        
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
        Order order = findOrderOrThrow(orderId);
        
        order.setAdminMemo(adminMemo);
        return orderRepository.save(order);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public Order cancelOrder(Long orderId, String cancelReason) {
        Order order = findOrderOrThrow(orderId);
        
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
     * 주문 내역 삭제 (상태를 DELETED로 변경)
     */
    @Transactional
    public Order markOrderAsDeleted(Long orderId, String deleteReason) {
        Order order = findOrderOrThrow(orderId);
        
        if (order.getStatus() == Order.OrderStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 주문입니다.");
        }
        
        // 주문 상태 변경 및 이력 생성 (Order의 changeStatus 메서드 활용)
        OrderHistory history = order.changeStatus(Order.OrderStatus.DELETED, "관리자", deleteReason);
        orderHistoryRepository.save(history);
        
        return orderRepository.save(order);
    }

    /**
     * 주문 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        return safelyGetStatistics(this::collectOrderStatistics, "주문 통계 정보 조회 중 오류 발생");
    }
    
    /**
     * 주문 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectOrderStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 총 주문 수 (DELETED 상태 제외)
        long totalOrders = orderRepository.count() - orderRepository.countByStatus(Order.OrderStatus.DELETED);
        statistics.put("totalOrders", totalOrders);
        
        // 상태별 주문 수
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            ordersByStatus.put(status.name(), count);
        }
        statistics.put("ordersByStatus", ordersByStatus);
        
        // 오늘 주문 수 (DELETED 상태 제외)
        LocalDateTime todayStart = getTodayStart();
        LocalDateTime todayEnd = getTodayEnd();

        // 오늘 주문 수 조회 (Specification 사용)
        Specification<Order> todaySpec = getCreatedBetweenSpec(todayStart, todayEnd);
        Specification<Order> todayDeletedSpec = todaySpec.and(getStatusSpec(Order.OrderStatus.DELETED));
        
        long todayOrders = orderRepository.count(todaySpec) - orderRepository.count(todayDeletedSpec);
        statistics.put("todayOrders", todayOrders);
        
        // 이번 달 주문 수 (DELETED 상태 제외)
        LocalDateTime monthStart = getMonthStart();
        LocalDateTime monthEnd = getMonthEnd();
        
        // 이번 달 주문 수 조회 (Specification 사용)
        Specification<Order> monthSpec = getCreatedBetweenSpec(monthStart, monthEnd);
        Specification<Order> monthDeletedSpec = monthSpec.and(getStatusSpec(Order.OrderStatus.DELETED));
        
        long monthOrders = orderRepository.count(monthSpec) - orderRepository.count(monthDeletedSpec);
        statistics.put("monthOrders", monthOrders);
        
        // 총 매출 (DELETED 및 CANCELLED 상태 제외)
        List<Order> validOrders = orderRepository.findAll().stream()
            .filter(order -> order.getStatus() != Order.OrderStatus.DELETED && 
                            order.getStatus() != Order.OrderStatus.CANCELLED)
            .collect(Collectors.toList());
        
        double totalSales = validOrders.stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        statistics.put("totalSales", totalSales);
        
        // 이번 달 매출 (DELETED 및 CANCELLED 상태 제외)
        double monthSales = validOrders.stream()
            .filter(order -> order.getCreatedAt() != null && 
                           order.getCreatedAt().isAfter(monthStart) && 
                           order.getCreatedAt().isBefore(monthEnd))
            .mapToDouble(Order::getTotalAmount)
            .sum();
        statistics.put("monthSales", monthSales);
        
        // 월별 주문 통계 (DELETED 상태 제외)
        Map<String, Long> monthlyOrders = new HashMap<>();
        
        orderRepository.findAll().stream()
            .filter(order -> order.getStatus() != Order.OrderStatus.DELETED)
            .forEach(order -> {
                if (order.getCreatedAt() != null) {
                    String month = order.getCreatedAt().format(MONTH_FORMATTER);
                    monthlyOrders.put(month, monthlyOrders.getOrDefault(month, 0L) + 1);
                }
            });
        statistics.put("monthlyOrders", monthlyOrders);
        
        // 월별 매출 통계 (DELETED 및 CANCELLED 상태 제외)
        Map<String, Double> monthlySales = new HashMap<>();
        
        validOrders.forEach(order -> {
            if (order.getCreatedAt() != null) {
                String month = order.getCreatedAt().format(MONTH_FORMATTER);
                monthlySales.put(month, monthlySales.getOrDefault(month, 0.0) + order.getTotalAmount());
            }
        });
        statistics.put("monthlySales", monthlySales);
        
        return statistics;
    }
    
    /**
     * 주문 엑셀 내보내기를 위한 데이터 조회
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersForExport(LocalDateTime startDate, LocalDateTime endDate, Order.OrderStatus status) {
        // Specification을 사용하여 조건 조합
        Specification<Order> spec = Specification.where(null);
        
        if (status != null) {
            spec = spec.and(getStatusSpec(status));
        }
        
        if (startDate != null && endDate != null) {
            spec = spec.and(getCreatedBetweenSpec(startDate, endDate));
        }
        
        // 결과를 생성일 기준 내림차순 정렬
        return orderRepository.findAll(spec, 
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
    
    /**
     * 상태 변경 메시지 생성
     */
    private String getStatusChangeText(Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        String oldStatusText = oldStatus.getDisplayName();
        String newStatusText = newStatus.getDisplayName();
        
        return oldStatusText + " → " + newStatusText;
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

    /**
     * ID로 주문을 찾거나 예외를 발생시키는 헬퍼 메서드
     */
    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
    }
    
    /**
     * 상태 기준 검색을 위한 Specification 생성 헬퍼 메서드
     */
    private Specification<Order> getStatusSpec(Order.OrderStatus status) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("status"), status);
    }
    
    /**
     * 기간 기준 검색을 위한 Specification 생성 헬퍼 메서드
     */
    private Specification<Order> getCreatedBetweenSpec(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), startDate
                ));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), endDate
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
} 
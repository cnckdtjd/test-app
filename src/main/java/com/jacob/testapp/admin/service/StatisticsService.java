package com.jacob.testapp.admin.service;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.repository.OrderRepository;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jacob.testapp.admin.service.StatisticsUtil.*;

/**
 * 통계 정보를 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    
    /**
     * 사용자 관련 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        log.debug("사용자 통계 정보 조회");
        return safelyGetStatistics(this::collectUserStatistics, "사용자 통계 조회 중 오류 발생");
    }
    
    /**
     * 사용자 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<User> users = userRepository.findAll();
        stats.put("totalUsers", users.size());
        
        // 활성 계정 수
        long activeUsers = users.stream()
                .filter(user -> !user.isAccountLocked())
                .count();
        stats.put("activeUsers", activeUsers);
        
        // 잠긴 계정 수
        stats.put("lockedUsers", users.size() - activeUsers);
        
        // 최근 가입자 수 (지난 30일)
        LocalDateTime thirtyDaysAgo = getDaysAgo(30);
        long newUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        stats.put("newUsers", newUsers);
        
        // 월별 가입자 통계
        Map<String, Long> monthlyStats = new HashMap<>();
        
        users.forEach(user -> {
            if (user.getCreatedAt() != null) {
                String month = user.getCreatedAt().format(MONTH_FORMATTER);
                monthlyStats.put(month, monthlyStats.getOrDefault(month, 0L) + 1);
            }
        });
        
        stats.put("monthlyRegistrations", monthlyStats);
        
        // 상세 분석: 최근 7일간 일별 가입자 추이
        Map<String, Long> dailyStats = new HashMap<>();
        LocalDateTime sevenDaysAgo = getDaysAgo(7);
        
        users.stream()
            .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(sevenDaysAgo))
            .forEach(user -> {
                String day = user.getCreatedAt().format(DAY_FORMATTER);
                dailyStats.put(day, dailyStats.getOrDefault(day, 0L) + 1);
            });
        
        stats.put("dailyRegistrations", dailyStats);
        
        return stats;
    }
    
    /**
     * 상품 관련 통계 정보 조회
     */
    public Map<String, Object> getProductStatistics() {
        log.debug("상품 통계 정보 조회");
        return safelyGetStatistics(this::collectProductStatistics, "상품 통계 조회 중 오류 발생");
    }
    
    /**
     * 상품 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectProductStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Product> products = productRepository.findAll();
        stats.put("totalProducts", products.size());
        
        // 재고 없는 상품 수
        long outOfStock = products.stream()
                .filter(product -> product.getStock() <= 0)
                .count();
        stats.put("outOfStockProducts", outOfStock);
        
        // 카테고리별 상품 수
        Map<Product.Category, Long> categoryStats = products.stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
        stats.put("productsByCategory", categoryStats);
        
        // 가격대별 상품 수
        Map<String, Long> priceRangeStats = collectPriceRangeStatistics(products);
        stats.put("productsByPriceRange", priceRangeStats);
        
        // 활성/비활성 상품 수
        long activeProducts = products.stream()
                .filter(product -> product.getStatus() == Product.Status.ACTIVE)
                .count();
        stats.put("activeProducts", activeProducts);
        stats.put("inactiveProducts", products.size() - activeProducts);
        
        return stats;
    }
    
    /**
     * 가격대별 상품 통계 수집
     */
    private Map<String, Long> collectPriceRangeStatistics(List<Product> products) {
        Map<String, Long> priceRangeStats = new HashMap<>();
        priceRangeStats.put("0-5000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(5000)) < 0).count());
        priceRangeStats.put("5000-10000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(5000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(10000)) < 0).count());
        priceRangeStats.put("10000-15000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(10000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(15000)) < 0).count());
        priceRangeStats.put("15000-20000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(15000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(20000)) < 0).count());
        priceRangeStats.put("20000-25000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(20000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(25000)) < 0).count());
        priceRangeStats.put("25000-30000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(25000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(30000)) < 0).count());
        priceRangeStats.put("30000-40000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(30000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(40000)) < 0).count());
        priceRangeStats.put("40000-50000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(40000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(50000)) < 0).count());
        priceRangeStats.put("50000+", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(50000)) >= 0).count());
        return priceRangeStats;
    }
    
    /**
     * 주문 관련 통계 정보 조회
     */
    public Map<String, Object> getOrderStatistics() {
        log.debug("주문 통계 정보 조회");
        return safelyGetStatistics(this::collectOrderStatistics, "주문 통계 조회 중 오류 발생");
    }
    
    /**
     * 주문 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Order> orders = orderRepository.findAll();
        stats.put("totalOrders", orders.size());
        
        // 주문 상태별 통계
        Map<Order.OrderStatus, Long> statusStats = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        stats.put("ordersByStatus", statusStats);
        
        // 완료된 주문 개수
        long completedOrders = statusStats.getOrDefault(Order.OrderStatus.COMPLETED, 0L);
        stats.put("completedOrders", completedOrders);
        
        // 처리 중인 주문 개수 (PENDING, PAID, SHIPPING 상태)
        long pendingOrders = statusStats.getOrDefault(Order.OrderStatus.PENDING, 0L) +
                           statusStats.getOrDefault(Order.OrderStatus.PAID, 0L) +
                           statusStats.getOrDefault(Order.OrderStatus.SHIPPING, 0L);
        stats.put("pendingOrders", pendingOrders);
        
        // 총 매출 계산 - CANCELLED, DELETED 상태가 아닌 주문만 합산
        double totalSales = orders.stream()
                .filter(order -> order.getStatus() != Order.OrderStatus.CANCELLED && 
                               order.getStatus() != Order.OrderStatus.DELETED)
                .filter(order -> order.getTotalAmount() != null)
                .mapToDouble(Order::getTotalAmount)
                .sum();
        stats.put("totalSales", totalSales);
        
        // 월별 주문 통계
        Map<String, Long> monthlyStats = new HashMap<>();
        
        orders.forEach(order -> {
            if (order.getCreatedAt() != null) {
                String month = order.getCreatedAt().format(MONTH_FORMATTER);
                monthlyStats.put(month, monthlyStats.getOrDefault(month, 0L) + 1);
            }
        });
        
        stats.put("monthlyOrders", monthlyStats);
        
        // 월별 매출 통계
        Map<String, BigDecimal> monthlySales = new HashMap<>();
        
        orders.forEach(order -> {
            if (order.getCreatedAt() != null && order.getTotalAmount() != null) {
                String month = order.getCreatedAt().format(MONTH_FORMATTER);
                BigDecimal currentAmount = monthlySales.getOrDefault(month, BigDecimal.ZERO);
                monthlySales.put(month, currentAmount.add(BigDecimal.valueOf(order.getTotalAmount())));
            }
        });
        
        stats.put("monthlySales", monthlySales);
        
        return stats;
    }

    /**
     * 주문 상세 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDetailedOrderStatistics() {
        log.debug("주문 상세 통계 정보 조회");
        return safelyGetStatistics(this::collectDetailedOrderStatistics, "주문 상세 통계 조회 중 오류 발생");
    }
    
    /**
     * 주문 상세 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectDetailedOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Object> baseStats = collectOrderStatistics();
        stats.putAll(baseStats);
        
        List<Order> orders = orderRepository.findAll();
        
        // 최근 7일간 일별 주문 수
        Map<String, Long> dailyOrderStats = new HashMap<>();
        LocalDateTime sevenDaysAgo = getDaysAgo(7);
        
        orders.stream()
            .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(sevenDaysAgo))
            .forEach(order -> {
                String day = order.getCreatedAt().format(DAY_FORMATTER);
                dailyOrderStats.put(day, dailyOrderStats.getOrDefault(day, 0L) + 1);
            });
        
        stats.put("dailyOrders", dailyOrderStats);
        
        // 주문 통계 집계
        collectOrderAggregateStatistics(stats, orders);
        
        return stats;
    }
    
    /**
     * 주문 통계 집계 메서드
     */
    private void collectOrderAggregateStatistics(Map<String, Object> stats, List<Order> orders) {
        // 평균 주문 금액
        double avgOrderAmount = orders.stream()
            .filter(order -> order.getTotalAmount() != null)
            .mapToDouble(Order::getTotalAmount)
            .average()
            .orElse(0);
        stats.put("averageOrderAmount", avgOrderAmount);
        
        // 최대 주문 금액
        double maxOrderAmount = orders.stream()
            .filter(order -> order.getTotalAmount() != null)
            .mapToDouble(Order::getTotalAmount)
            .max()
            .orElse(0);
        stats.put("maxOrderAmount", maxOrderAmount);
        
        // 구매 횟수별 사용자 수
        Map<Long, Long> userOrderCounts = orders.stream()
            .filter(order -> order.getUser() != null)
            .collect(Collectors.groupingBy(
                order -> order.getUser().getId(),
                Collectors.counting()
            ));
        
        // 1회, 2-3회, 4-5회, 6회 이상 주문 사용자 분포
        long oneTimeUsers = userOrderCounts.values().stream().filter(count -> count == 1).count();
        long twoToThreeTimeUsers = userOrderCounts.values().stream().filter(count -> count >= 2 && count <= 3).count();
        long fourToFiveTimeUsers = userOrderCounts.values().stream().filter(count -> count >= 4 && count <= 5).count();
        long moreThanFiveTimeUsers = userOrderCounts.values().stream().filter(count -> count > 5).count();
        
        Map<String, Long> orderFrequencyStats = new HashMap<>();
        orderFrequencyStats.put("1회", oneTimeUsers);
        orderFrequencyStats.put("2-3회", twoToThreeTimeUsers);
        orderFrequencyStats.put("4-5회", fourToFiveTimeUsers);
        orderFrequencyStats.put("6회 이상", moreThanFiveTimeUsers);
        
        stats.put("orderFrequency", orderFrequencyStats);
    }
}

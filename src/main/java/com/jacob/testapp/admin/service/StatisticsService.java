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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Map<String, Object> getUserStatistics() {
        log.debug("사용자 통계 정보 조회");
        Map<String, Object> stats = new HashMap<>();
        
        try {
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
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long newUsers = users.stream()
                    .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo))
                    .count();
            stats.put("newUsers", newUsers);
            
            // 월별 가입자 통계
            Map<String, Long> monthlyStats = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            
            users.forEach(user -> {
                if (user.getCreatedAt() != null) {
                    String month = user.getCreatedAt().format(formatter);
                    monthlyStats.put(month, monthlyStats.getOrDefault(month, 0L) + 1);
                }
            });
            
            stats.put("monthlyRegistrations", monthlyStats);
            
            return stats;
        } catch (Exception e) {
            log.error("사용자 통계 조회 중 오류 발생: ", e);
            stats.put("error", "통계 정보를 로드하는 중 오류가 발생했습니다");
            return stats;
        }
    }
    
    /**
     * 상품 관련 통계 정보 조회
     */
    public Map<String, Object> getProductStatistics() {
        log.debug("상품 통계 정보 조회");
        Map<String, Object> stats = new HashMap<>();
        
        try {
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
            Map<String, Long> priceRangeStats = new HashMap<>();
            priceRangeStats.put("0-10000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(10000)) < 0).count());
            priceRangeStats.put("10000-30000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(10000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(30000)) < 0).count());
            priceRangeStats.put("30000-50000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(30000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(50000)) < 0).count());
            priceRangeStats.put("50000-100000", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(50000)) >= 0 && p.getPrice().compareTo(BigDecimal.valueOf(100000)) < 0).count());
            priceRangeStats.put("100000+", products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.valueOf(100000)) >= 0).count());
            stats.put("productsByPriceRange", priceRangeStats);
            
            // 활성/비활성 상품 수
            long activeProducts = products.stream()
                    .filter(product -> product.getStatus() == Product.Status.ACTIVE)
                    .count();
            stats.put("activeProducts", activeProducts);
            stats.put("inactiveProducts", products.size() - activeProducts);
            
            return stats;
        } catch (Exception e) {
            log.error("상품 통계 조회 중 오류 발생: ", e);
            stats.put("error", "통계 정보를 로드하는 중 오류가 발생했습니다");
            return stats;
        }
    }
    
    /**
     * 주문 관련 통계 정보 조회
     */
    public Map<String, Object> getOrderStatistics() {
        log.debug("주문 통계 정보 조회");
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<Order> orders = orderRepository.findAll();
            stats.put("totalOrders", orders.size());
            
            // 주문 상태별 통계
            Map<Order.OrderStatus, Long> statusStats = orders.stream()
                    .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
            stats.put("ordersByStatus", statusStats);
            
            // 월별 주문 통계
            Map<String, Long> monthlyStats = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            
            orders.forEach(order -> {
                if (order.getCreatedAt() != null) {
                    String month = order.getCreatedAt().format(formatter);
                    monthlyStats.put(month, monthlyStats.getOrDefault(month, 0L) + 1);
                }
            });
            
            stats.put("monthlyOrders", monthlyStats);
            
            // 월별 매출 통계
            Map<String, BigDecimal> monthlySales = new HashMap<>();
            
            orders.forEach(order -> {
                if (order.getCreatedAt() != null && order.getTotalAmount() != null) {
                    String month = order.getCreatedAt().format(formatter);
                    BigDecimal currentAmount = monthlySales.getOrDefault(month, BigDecimal.ZERO);
                    monthlySales.put(month, currentAmount.add(order.getTotalAmount()));
                }
            });
            
            stats.put("monthlySales", monthlySales);
            
            return stats;
        } catch (Exception e) {
            log.error("주문 통계 조회 중 오류 발생: ", e);
            stats.put("error", "통계 정보를 로드하는 중 오류가 발생했습니다");
            return stats;
        }
    }
}

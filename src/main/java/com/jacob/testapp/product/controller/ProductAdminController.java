package com.jacob.testapp.product.controller;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.service.ProductManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductAdminController {

    private final ProductManagementService productManagementService;

    /**
     * 관리자용 상품 검색 API - 다양한 필터링 조건 지원
     *
     * @param keyword 검색어 (상품명)
     * @param category 상품 카테고리
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 검색 조건에 맞는 상품 목록 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Product.Category category,
            @RequestParam(required = false) Product.Status status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Product> products = productManagementService.findProductsWithSpecification(
                keyword, category, status, pageable);
        
        return ResponseEntity.ok(products);
    }

    /**
     * 관리자용 상품 통계 API
     *
     * @return 상품 관련 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 재고 관련 통계
        statistics.put("lowStockCount", productManagementService.countProductsWithLowStock(10));
        
        // 상태별 상품 수
        Map<String, Long> statusCounts = new HashMap<>();
        for (Product.Status status : Product.Status.values()) {
            statusCounts.put(status.name(), productManagementService.countProductsByStatus(status));
        }
        statistics.put("statusCounts", statusCounts);
        
        // 카테고리별 상품 수
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Product.Category category : Product.Category.values()) {
            categoryCounts.put(category.name(), productManagementService.countProductsByCategory(category));
        }
        statistics.put("categoryCounts", categoryCounts);
        
        return ResponseEntity.ok(statistics);
    }
} 
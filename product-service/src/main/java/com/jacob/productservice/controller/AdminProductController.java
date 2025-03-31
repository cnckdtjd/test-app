package com.jacob.productservice.controller;

import com.jacob.productservice.model.Product;
import com.jacob.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    
    /**
     * 모든 상품 목록 조회 (상태에 상관없이)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Product> products = productService.findAllWithPagination(page, size);
        long total = productService.countProducts();
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("currentPage", page);
        response.put("totalItems", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 재고 부족 상품 수 조회
     */
    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
        long count = productService.countByStockLessThanEqual(threshold);
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("threshold", threshold);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 카테고리별 상품 수 통계
     */
    @GetMapping("/category-stats")
    public ResponseEntity<Map<String, Object>> getCategoryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (Product.Category category : Product.Category.values()) {
            long count = productService.countByCategory(category);
            stats.put(category.name(), count);
        }
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 카테고리별 재고 일괄 업데이트
     */
    @PutMapping("/category-stock")
    public ResponseEntity<?> updateStockByCategory(
            @RequestParam Product.Category category,
            @RequestParam int stock) {
        
        if (stock < 0) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "재고는 0 이상이어야 합니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        int updated = productService.updateStockByCategory(category, stock);
        
        Map<String, Object> response = new HashMap<>();
        response.put("updated", updated);
        response.put("category", category);
        response.put("stock", stock);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상품 ID 범위로 삭제 (테스트 데이터 정리용)
     */
    @DeleteMapping("/range")
    public ResponseEntity<?> deleteProductRange(
            @RequestParam Long startId,
            @RequestParam Long endId) {
        
        if (startId > endId) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "시작 ID는 종료 ID보다 작아야 합니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        productService.deleteByIdBetween(startId, endId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("startId", startId);
        response.put("endId", endId);
        response.put("message", String.format("ID 범위 %d부터 %d까지의 상품이 삭제되었습니다.", startId, endId));
        
        return ResponseEntity.ok(response);
    }
} 
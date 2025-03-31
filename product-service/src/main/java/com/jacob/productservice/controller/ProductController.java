package com.jacob.productservice.controller;

import com.jacob.productservice.model.Product;
import com.jacob.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (페이지네이션)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Product.Category category,
            @RequestParam(required = false) Product.Status status) {
        
        List<Product> products;
        long total = 0;
        
        // 상태 기본값은 ACTIVE
        Product.Status productStatus = (status != null) ? status : Product.Status.ACTIVE;
        
        if (name != null && !name.isEmpty() && category != null) {
            products = productService.searchByNameAndCategory(name, category, page, size);
            total = productService.countActiveProducts();
        } else if (name != null && !name.isEmpty()) {
            products = productService.searchProducts(name, page, size);
            total = productService.countActiveProducts();
        } else if (category != null) {
            products = productService.findByCategory(category, page, size);
            total = productService.countActiveProducts();
        } else {
            products = productService.findActiveProducts(page, size);
            total = productService.countActiveProducts();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("currentPage", page);
        response.put("totalItems", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 상품 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            Product product = productService.findById(id);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * 최근 상품 목록 조회
     */
    @GetMapping("/latest")
    public ResponseEntity<List<Product>> getLatestProducts(@RequestParam(defaultValue = "5") int limit) {
        List<Product> latestProducts = productService.findLatestProducts(limit);
        return ResponseEntity.ok(latestProducts);
    }
    
    /**
     * 카테고리별 상품 목록 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @PathVariable Product.Category category) {
        List<Product> products = productService.findByCategoryOrderByPriceAsc(category);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Product.Category>> getCategories() {
        List<Product.Category> categories = productService.findAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * 상품 생성 (관리자 전용)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product) {
        try {
            product.setId(null); // 신규 생성 시 ID는 null로 설정
            product.setStatus(Product.Status.ACTIVE); // 기본 상태는 ACTIVE
            Product savedProduct = productService.save(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 상품 수정 (관리자 전용)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        try {
            // ID가 존재하는지 확인
            productService.findById(id);
            
            // 요청 바디의 ID와 경로 변수의 ID가 일치하는지 확인
            if (!id.equals(product.getId())) {
                product.setId(id);
            }
            
            Product updatedProduct = productService.save(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }
    
    /**
     * 상품 삭제 (관리자 전용)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            // ID가 존재하는지 확인
            productService.findById(id);
            
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * 재고 감소 (다른 서비스에서 호출)
     */
    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<?> decreaseStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            boolean updated = productService.decreaseStock(id, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", updated);
            response.put("message", "재고가 성공적으로 감소되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 재고 증가 (다른 서비스에서 호출)
     */
    @PostMapping("/{id}/increase-stock")
    public ResponseEntity<?> increaseStock(@PathVariable Long id, @RequestParam int quantity) {
        try {
            boolean updated = productService.increaseStock(id, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", updated);
            response.put("message", "재고가 성공적으로 증가되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * 부하 테스트용 - 의도적으로 지연 발생
     */
    @GetMapping("/delay/{id}")
    public ResponseEntity<?> getProductWithDelay(
            @PathVariable Long id, 
            @RequestParam(defaultValue = "1000") long delayMillis) {
        Optional<Product> product = productService.findByIdWithDelay(id, delayMillis);
        
        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "상품을 찾을 수 없습니다. ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
} 
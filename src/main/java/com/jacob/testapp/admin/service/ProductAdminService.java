package com.jacob.testapp.admin.service;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자용 상품 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회 (페이징)
     */
    public Page<Product> findAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * 상품 검색 (키워드, 카테고리, 상태 필터링)
     */
    public Page<Product> searchProducts(String keyword, Product.Category category, Product.Status status, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            if (category != null) {
                if (status != null) {
                    // 키워드 + 카테고리 + 상태 검색
                    return productRepository.findByNameContainingAndCategoryAndStatus(keyword, category, status, pageable);
                } else {
                    // 키워드 + 카테고리 검색
                    return productRepository.findByNameContainingAndCategory(keyword, category, pageable);
                }
            } else if (status != null) {
                // 키워드 + 상태 검색
                return productRepository.findByNameContainingAndStatus(keyword, status, pageable);
            } else {
                // 키워드만 검색
                return productRepository.findByNameContaining(keyword, pageable);
            }
        } else if (category != null) {
            if (status != null) {
                // 카테고리 + 상태 검색
                return productRepository.findByCategoryAndStatus(category, status, pageable);
            } else {
                // 카테고리만 검색
                return productRepository.findByCategory(category, pageable);
            }
        } else if (status != null) {
            // 상태만 검색
            return productRepository.findByStatus(status, pageable);
        } else {
            // 필터링 없이 전체 조회
            return productRepository.findAll(pageable);
        }
    }

    /**
     * 상품 저장 (생성/수정)
     */
    @Transactional
    public Product saveProduct(Product product) {
        // 신규 상품인 경우
        if (product.getId() == null) {
            product.setCreatedAt(LocalDateTime.now());
        } else {
            // 기존 상품인 경우, 현재 데이터베이스에 저장된 생성일 유지
            productRepository.findById(product.getId()).ifPresent(existingProduct -> {
                if (product.getCreatedAt() == null) {
                    product.setCreatedAt(existingProduct.getCreatedAt());
                }
            });
        }
        
        product.setUpdatedAt(LocalDateTime.now());
        log.info("상품 저장: {}", product.getName());
        return productRepository.save(product);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            log.info("상품 삭제: {} (ID: {})", product.getName(), product.getId());
            productRepository.delete(product);
        });
    }

    /**
     * 상품 통계 정보 조회
     */
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 전체 상품 수
        long totalProducts = productRepository.count();
        statistics.put("totalProducts", totalProducts);
        
        // 활성 상품 수
        long activeProducts = productRepository.countByStatus(Product.Status.ACTIVE);
        statistics.put("activeProducts", activeProducts);
        
        // 비활성 상품 수
        long inactiveProducts = productRepository.countByStatus(Product.Status.INACTIVE);
        statistics.put("inactiveProducts", inactiveProducts);
        
        // 재고 없는 상품 수
        long outOfStockProducts = productRepository.countByStockLessThanEqual(0);
        statistics.put("outOfStockProducts", outOfStockProducts);
        
        // 카테고리별 상품 수
        Map<String, Long> productsByCategory = new HashMap<>();
        for (Product.Category category : Product.Category.values()) {
            long count = productRepository.countByCategory(category);
            productsByCategory.put(category.name(), count);
        }
        statistics.put("productsByCategory", productsByCategory);
        
        return statistics;
    }

    /**
     * 상품 대량 삭제 (ID 범위 기준)
     */
    @Transactional
    public int bulkDeleteProducts(Long startId, Long endId) {
        if (startId > endId) {
            throw new IllegalArgumentException("시작 ID는 종료 ID보다 작거나 같아야 합니다.");
        }
        
        log.info("상품 대량 삭제: ID {} ~ {}", startId, endId);
        productRepository.deleteByIdBetween(startId, endId);
        
        // 삭제된 레코드 수는 별도로 계산 필요
        long countBefore = productRepository.count();
        long countAfter = productRepository.count();
        return (int) (countBefore - countAfter);
    }

    /**
     * 상품 재고 일괄 업데이트
     */
    @Transactional
    public int bulkUpdateStock(Product.Category category, int stockAmount) {
        if (stockAmount < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
        
        log.info("카테고리 {} 상품 재고 일괄 업데이트: {}", category, stockAmount);
        return productRepository.updateStockByCategory(category, stockAmount);
    }
} 
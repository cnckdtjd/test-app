package com.jacob.testapp.admin.service;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jacob.testapp.admin.service.StatisticsUtil.*;

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
        return findProductsWithSpecification(keyword, category, status, pageable);
    }
    
    /**
     * 필터링된 상품 조회 (Specification 기반)
     */
    private Page<Product> findProductsWithSpecification(String keyword, Product.Category category, Product.Status status, Pageable pageable) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 키워드 검색 조건
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    "%" + keyword.toLowerCase() + "%"
                ));
            }
            
            // 카테고리 검색 조건
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            
            // 상태 검색 조건
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return productRepository.findAll(spec, pageable);
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
        return safelyGetStatistics(this::collectProductStatistics, "상품 통계 정보 조회 중 오류 발생");
    }
    
    /**
     * 상품 통계 데이터 수집 내부 메서드
     */
    private Map<String, Object> collectProductStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 전체 상품 수
        long totalProducts = productRepository.count();
        statistics.put("totalProducts", totalProducts);
        
        // 활성 상품 수 (Specification 사용)
        Specification<Product> activeSpec = getStatusSpec(Product.Status.ACTIVE);
        long activeProducts = productRepository.count(activeSpec);
        statistics.put("activeProducts", activeProducts);
        
        // 비활성 상품 수 (Specification 사용)
        Specification<Product> inactiveSpec = getStatusSpec(Product.Status.INACTIVE);
        long inactiveProducts = productRepository.count(inactiveSpec);
        statistics.put("inactiveProducts", inactiveProducts);
        
        // 재고 없는 상품 수 (Specification 사용)
        Specification<Product> outOfStockSpec = (root, query, criteriaBuilder) -> 
            criteriaBuilder.lessThanOrEqualTo(root.get("stock"), 0);
        long outOfStockProducts = productRepository.count(outOfStockSpec);
        statistics.put("outOfStockProducts", outOfStockProducts);
        
        // 카테고리별 상품 수
        Map<String, Long> productsByCategory = new HashMap<>();
        for (Product.Category category : Product.Category.values()) {
            Specification<Product> categorySpec = getCategorySpec(category);
            long count = productRepository.count(categorySpec);
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
        
        // ID 범위 조건으로 상품 조회 후 일괄 삭제
        Specification<Product> idRangeSpec = (root, query, criteriaBuilder) -> {
            Predicate greaterThanOrEqual = criteriaBuilder.greaterThanOrEqualTo(root.get("id"), startId);
            Predicate lessThanOrEqual = criteriaBuilder.lessThanOrEqualTo(root.get("id"), endId);
            return criteriaBuilder.and(greaterThanOrEqual, lessThanOrEqual);
        };
        
        List<Product> productsToDelete = productRepository.findAll(idRangeSpec);
        int deletedCount = productsToDelete.size();
        
        productRepository.deleteAll(productsToDelete);
        
        return deletedCount;
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
        
        // 카테고리로 상품 조회 후 재고 일괄 업데이트
        Specification<Product> categorySpec = getCategorySpec(category);
        List<Product> productsToUpdate = productRepository.findAll(categorySpec);
        
        for (Product product : productsToUpdate) {
            product.setStock(stockAmount);
            productRepository.save(product);
        }
        
        return productsToUpdate.size();
    }
    
    /**
     * 카테고리 기준 검색을 위한 Specification 생성 헬퍼 메서드
     */
    private Specification<Product> getCategorySpec(Product.Category category) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("category"), category);
    }
    
    /**
     * 상태 기준 검색을 위한 Specification 생성 헬퍼 메서드
     */
    private Specification<Product> getStatusSpec(Product.Status status) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("status"), status);
    }
} 
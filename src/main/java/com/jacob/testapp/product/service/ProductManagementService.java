package com.jacob.testapp.product.service;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductManagementService {

    private final ProductRepository productRepository;

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
     * 상품 검색 (키워드, 카테고리, 상태 필터링)
     */
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, Product.Category category, Product.Status status, Pageable pageable) {
        return findProductsWithSpecification(keyword, category, status, pageable);
    }

    /**
     * 상품 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 전체 상품 수
        long totalProducts = productRepository.count();
        statistics.put("totalProducts", totalProducts);
        
        // 활성 상품 수
        Specification<Product> activeSpec = createStatusSpecification(Product.Status.ACTIVE);
        long activeProducts = productRepository.count(activeSpec);
        statistics.put("activeProducts", activeProducts);
        
        // 비활성 상품 수
        Specification<Product> inactiveSpec = createStatusSpecification(Product.Status.INACTIVE);
        long inactiveProducts = productRepository.count(inactiveSpec);
        statistics.put("inactiveProducts", inactiveProducts);
        
        // 재고 없는 상품 수
        Specification<Product> outOfStockSpec = (root, query, criteriaBuilder) -> 
            criteriaBuilder.lessThanOrEqualTo(root.get("stock"), 0);
        long outOfStockProducts = productRepository.count(outOfStockSpec);
        statistics.put("outOfStockProducts", outOfStockProducts);
        
        // 카테고리별 상품 수
        Map<String, Long> productsByCategory = new HashMap<>();
        for (Product.Category category : Product.Category.values()) {
            Specification<Product> categorySpec = createCategorySpecification(category);
            long count = productRepository.count(categorySpec);
            productsByCategory.put(category.name(), count);
        }
        statistics.put("productsByCategory", productsByCategory);
        
        return statistics;
    }

    /**
     * 관리자용 상품 검색 메서드 - 다양한 검색 조건을 처리함
     * - 키워드로 상품명 검색 
     * - 카테고리별 필터링
     * - 상태별 필터링
     * 
     * @param keyword 검색어 (상품명 기준)
     * @param category 상품 카테고리
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 검색 조건에 맞는 상품 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<Product> findProductsWithSpecification(
            String keyword, 
            Product.Category category, 
            Product.Status status, 
            Pageable pageable) {
        
        Specification<Product> spec = Specification.where(null);
        
        // 키워드 검색 조건 추가
        if (StringUtils.hasText(keyword)) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), 
                    "%" + keyword.toLowerCase() + "%"));
        }
        
        // 카테고리 필터 조건 추가
        if (category != null) {
            spec = spec.and(createCategorySpecification(category));
        }
        
        // 상태 필터 조건 추가
        if (status != null) {
            spec = spec.and(createStatusSpecification(status));
        }
        
        return productRepository.findAll(spec, pageable);
    }

    /**
     * 관리자 통계용 - 재고 임계치 이하 상품 수 조회
     * 
     * @param threshold 재고 임계치
     * @return 재고가 임계치 이하인 상품 수
     */
    @Transactional(readOnly = true)
    public long countProductsWithLowStock(int threshold) {
        return productRepository.countByStockLessThanEqual(threshold);
    }

    /**
     * 관리자 통계용 - 상태별 상품 수 조회
     * 
     * @param status 상품 상태
     * @return 해당 상태의 상품 수
     */
    @Transactional(readOnly = true)
    public long countProductsByStatus(Product.Status status) {
        return productRepository.countByStatus(status);
    }

    /**
     * 관리자 통계용 - 카테고리별 상품 수 조회
     * 
     * @param category 상품 카테고리
     * @return 해당 카테고리의 상품 수
     */
    @Transactional(readOnly = true)
    public long countProductsByCategory(Product.Category category) {
        return productRepository.countByCategory(category);
    }

    /**
     * 카테고리별 필터링을 위한 Specification 생성
     * 
     * @param category 상품 카테고리
     * @return 카테고리 검색을 위한 Specification
     */
    private Specification<Product> createCategorySpecification(Product.Category category) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("category"), category);
    }

    /**
     * 상태별 필터링을 위한 Specification 생성
     * 
     * @param status 상품 상태
     * @return 상태 검색을 위한 Specification
     */
    private Specification<Product> createStatusSpecification(Product.Status status) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("status"), status);
    }
} 
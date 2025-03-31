package com.jacob.productservice.service;

import com.jacob.productservice.model.Product;
import com.jacob.productservice.repository.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 상품 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "products")
@Transactional(readOnly = true)
public class ProductService {

    private final ProductMapper productMapper;

    /**
     * 모든 상품 목록 조회
     */
    public List<Product> findAll() {
        return productMapper.findAll();
    }

    /**
     * 페이지네이션이 적용된 상품 목록 조회
     */
    public List<Product> findAllWithPagination(int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findAllWithPagination(offset, size);
    }

    /**
     * 활성화된 상품 목록 조회 (페이지네이션)
     */
    public List<Product> findActiveProducts(int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByStatusEquals(Product.Status.ACTIVE, offset, size);
    }

    /**
     * 상품명으로 활성화된 상품 검색 (페이지네이션)
     */
    public List<Product> searchProducts(String name, int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByStatusEqualsAndNameContaining(Product.Status.ACTIVE, name, offset, size);
    }

    /**
     * 카테고리로 활성화된 상품 검색 (페이지네이션)
     */
    public List<Product> findByCategory(Product.Category category, int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByStatusEqualsAndCategory(Product.Status.ACTIVE, category, offset, size);
    }

    /**
     * 상품명과 카테고리로 활성화된 상품 검색 (페이지네이션)
     */
    public List<Product> searchByNameAndCategory(String name, Product.Category category, int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByStatusEqualsAndNameContainingAndCategory(Product.Status.ACTIVE, name, category, offset, size);
    }

    /**
     * 상품 ID로 상품 조회
     */
    @Cacheable(key = "#id")
    public Product findById(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id);
        }
        return product;
    }

    /**
     * 상품 수 조회
     */
    public long countProducts() {
        return productMapper.countAll();
    }

    /**
     * 활성화된 상품 수 조회
     */
    public long countActiveProducts() {
        return productMapper.countByStatus(Product.Status.ACTIVE);
    }

    /**
     * 카테고리별 상품 조회
     */
    public List<Product> findByCategoryOrderByPriceAsc(Product.Category category) {
        return productMapper.findByCategoryOrderByPriceAsc(category);
    }

    /**
     * 상품명으로 상품 검색
     */
    public List<Product> findByName(String keyword) {
        return productMapper.findByNameContainingIgnoreCase(keyword);
    }

    /**
     * 최근 추가된 상품 조회
     */
    public List<Product> findLatestProducts(int limit) {
        return productMapper.findTopByOrderByCreatedAtDesc(limit);
    }

    /**
     * 상품 저장 (생성 및 업데이트)
     */
    @Transactional
    @CacheEvict(key = "#product.id", condition = "#product.id != null")
    public Product save(Product product) {
        LocalDateTime now = LocalDateTime.now();
        
        if (product.getId() == null) {
            // 신규 상품 생성
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
            product.setVersion(0L);
            productMapper.save(product);
        } else {
            // 기존 상품 업데이트
            Product existingProduct = findById(product.getId());
            product.setVersion(existingProduct.getVersion());
            product.setCreatedAt(existingProduct.getCreatedAt());
            product.setUpdatedAt(now);
            int updated = productMapper.update(product);
            
            if (updated == 0) {
                throw new IllegalStateException("상품 업데이트 실패: 동시성 충돌 발생");
            }
        }
        
        return product;
    }

    /**
     * 재고 감소 (낙관적 락 사용)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(key = "#productId")
    public boolean updateStock(Long productId, int quantity) {
        int affected = productMapper.updateStockById(productId, quantity);
        return affected > 0;
    }

    /**
     * 재고 감소 (낙관적 락 사용)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(key = "#productId")
    public boolean decreaseStock(Long productId, int quantity) {
        int affected = productMapper.decreaseStock(productId, quantity);
        if (affected == 0) {
            throw new IllegalStateException("재고가 부족하거나 상품이 존재하지 않습니다. 상품 ID: " + productId);
        }
        return true;
    }

    /**
     * 재고 증가 (낙관적 락 사용)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(key = "#productId")
    public boolean increaseStock(Long productId, int quantity) {
        int affected = productMapper.increaseStock(productId, quantity);
        if (affected == 0) {
            throw new IllegalStateException("상품이 존재하지 않습니다. 상품 ID: " + productId);
        }
        return true;
    }

    /**
     * 상품 삭제
     */
    @Transactional
    @CacheEvict(key = "#id")
    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    /**
     * ID 범위로 상품 삭제 (테스트용)
     */
    @Transactional
    public void deleteByIdBetween(Long startId, Long endId) {
        productMapper.deleteByIdBetween(startId, endId);
    }

    /**
     * 부하 테스트용 - 의도적으로 지연 발생
     */
    public Optional<Product> findByIdWithDelay(Long id, long delayMillis) {
        Product product = null;
        try {
            product = findById(id);
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (IllegalArgumentException e) {
            log.warn("상품을 찾을 수 없습니다: {}", id);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.ofNullable(product);
    }

    /**
     * 이름으로 상품 검색 (페이지네이션)
     */
    public List<Product> findByNameContaining(String name, int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByNameContaining(name, offset, size);
    }

    /**
     * 이름과 카테고리로 상품 검색 (페이지네이션)
     */
    public List<Product> findByNameContainingAndCategory(String name, Product.Category category, int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.findByNameContainingAndCategory(name, category, offset, size);
    }

    /**
     * 모든 카테고리 목록 가져오기
     */
    public List<Product.Category> findAllCategories() {
        return List.of(Product.Category.values());
    }
    
    /**
     * 카테고리별 상품 수 조회
     */
    public long countByCategory(Product.Category category) {
        return productMapper.countByCategory(category);
    }
    
    /**
     * 재고가 특정 수량 이하인 상품 수 조회
     */
    public long countByStockLessThanEqual(int stock) {
        return productMapper.countByStockLessThanEqual(stock);
    }
    
    /**
     * 카테고리별 재고 일괄 업데이트
     */
    @Transactional
    public int updateStockByCategory(Product.Category category, int stock) {
        return productMapper.updateStockByCategory(category, stock);
    }
} 
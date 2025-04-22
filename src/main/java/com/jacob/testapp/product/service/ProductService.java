package com.jacob.testapp.product.service;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 상품 서비스
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 상품 목록 조회
     */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * 페이징된 상품 목록 조회
     */
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * 활성 상품 목록 조회
     */
    public Page<Product> findActiveProducts(Pageable pageable) {
        return productRepository.findByStatus(Product.Status.ACTIVE, pageable);
    }

    /**
     * 이름으로 상품 검색
     */
    public Page<Product> findByNameContaining(String name, Pageable pageable) {
        return productRepository.findByNameContaining(name, pageable);
    }

    /**
     * 카테고리로 상품 검색
     */
    public Page<Product> findByCategory(Product.Category category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable);
    }

    /**
     * 이름과 카테고리로 상품 검색
     */
    public Page<Product> findByNameContainingAndCategory(String name, Product.Category category, Pageable pageable) {
        return productRepository.findByNameContainingAndCategory(name, category, pageable);
    }

    /**
     * 활성 상품만 검색 (이름 기준)
     */
    public Page<Product> searchActiveProducts(String name, Pageable pageable) {
        return productRepository.findByStatusAndNameContaining(Product.Status.ACTIVE, name, pageable);
    }

    /**
     * 활성 상품만 검색 (카테고리 기준)
     */
    public Page<Product> findActiveProductsByCategory(Product.Category category, Pageable pageable) {
        return productRepository.findByStatusAndCategory(Product.Status.ACTIVE, category, pageable);
    }

    /**
     * 활성 상품만 검색 (이름, 카테고리 기준)
     */
    public Page<Product> searchActiveProductsByNameAndCategory(String name, Product.Category category, Pageable pageable) {
        return productRepository.findByStatusAndNameContainingAndCategory(Product.Status.ACTIVE, name, category, pageable);
    }

    /**
     * ID로 상품 조회
     */
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 전체 상품 수 조회
     */
    public long countProducts() {
        return productRepository.count();
    }

    /**
     * 활성 상품 수 조회
     */
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    /**
     * 카테고리별 상품 조회 (가격 오름차순)
     */
    public List<Product> findByCategoryOrderByPrice(Product.Category category) {
        return productRepository.findByCategoryOrderByPriceAsc(category);
    }

    /**
     * 상품명으로 상품 검색 (대소문자 구분 없음)
     */
    public List<Product> findByNameIgnoreCase(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    /**
     * 최근 추가된 상품 조회
     */
    public List<Product> findLatestProducts() {
        return productRepository.findTop5ByOrderByCreatedAtDesc();
    }

    /**
     * 상품 저장
     */
    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * 재고 감소
     */
    @Transactional
    public boolean decreaseStock(Long productId, int quantity) {
        // 먼저 최적화된 쿼리로 시도
        int affected = productRepository.decreaseStock(productId, quantity);
        if (affected > 0) {
            return true;
        }
        
        // 실패하면 비관적 락을 사용하여 재시도
        return findAndDecreaseStock(productId, quantity);
    }
    
    /**
     * 비관적 락을 사용하여 재고 감소
     */
    private boolean findAndDecreaseStock(Long productId, int quantity) {
        try {
            Product product = productRepository.findByIdWithPessimisticLock(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
            
            if (product.decreaseStock(quantity)) {
                productRepository.save(product);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 재고 증가
     */
    @Transactional
    public boolean increaseStock(Long productId, int quantity) {
        try {
            Product product = findById(productId);
            product.increaseStock(quantity);
            productRepository.save(product);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    /**
     * 상품 상태 변경
     */
    @Transactional
    public Product updateStatus(Long id, Product.Status status) {
        Product product = findById(id);
        product.setStatus(status);
        return productRepository.save(product);
    }

    /**
     * 모든 카테고리 목록 가져오기
     */
    public List<Product.Category> findAllCategories() {
        return List.of(Product.Category.values());
    }

    /**
     * 부하 테스트용 - 의도적으로 지연 발생
     */
    public Optional<Product> findByIdWithDelay(Long id, long delayMillis) {
        Optional<Product> product = productRepository.findById(id);
        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return product;
    }
} 
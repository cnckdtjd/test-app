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
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 모든 상품 목록 조회
     */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Page<Product> findActiveProducts(Pageable pageable) {
        return productRepository.findByStatusEquals(Product.Status.ACTIVE, pageable);
    }

    public Page<Product> searchProducts(String name, Pageable pageable) {
        return productRepository.findByStatusEqualsAndNameContaining(Product.Status.ACTIVE, name, pageable);
    }

    public Page<Product> findByCategory(Product.Category category, Pageable pageable) {
        return productRepository.findByStatusEqualsAndCategory(Product.Status.ACTIVE, category, pageable);
    }

    public Page<Product> searchByNameAndCategory(String name, Product.Category category, Pageable pageable) {
        return productRepository.findByStatusEqualsAndNameContainingAndCategory(Product.Status.ACTIVE, name, category, pageable);
    }

    /**
     * 상품 ID로 상품 조회
     */
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 상품 수 조회
     */
    public long countProducts() {
        return productRepository.count();
    }

    /**
     * 활성화된 상품 수 조회
     */
    public long countActiveProducts() {
        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() == Product.Status.ACTIVE)
                .count();
    }

    /**
     * 카테고리별 상품 조회
     */
    public List<Product> findByCategory(Product.Category category) {
        return productRepository.findByCategoryOrderByPriceAsc(category);
    }

    /**
     * 상품명으로 상품 검색
     */
    public List<Product> findByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    /**
     * 최근 추가된 상품 5개 조회
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

    @Transactional
    public boolean updateStock(Long productId, int quantity) {
        int affected = productRepository.updateStockById(productId, quantity);
        return affected > 0;
    }

    @Transactional
    public Product decreaseStock(Long productId, int quantity) {
        Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.getStock() >= quantity) {
                product.setStock(product.getStock() - quantity);
                return productRepository.save(product);
            } else {
                throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
            }
        }
        throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId);
    }

    @Transactional
    public Product restoreStock(Long productId, int quantity) {
        Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStock(product.getStock() + quantity);
            return productRepository.save(product);
        }
        throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    // 부하 테스트용 - 의도적으로 지연 발생
    public Optional<Product> findByIdWithDelay(Long id, long delayMillis) {
        Optional<Product> product = productRepository.findById(id);
        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return product;
    }

    // 이름으로 상품 검색 (부분 일치)
    public Page<Product> findByNameContaining(String name, Pageable pageable) {
        return productRepository.findByNameContaining(name, pageable);
    }

    // 이름과 카테고리로 상품 검색 (문자열 카테고리를 Enum으로 변환)
    public Page<Product> findByNameContainingAndCategory(String name, Product.Category category, Pageable pageable) {
        return productRepository.findByNameContainingAndCategory(name, category, pageable);
    }

    // 모든 카테고리 목록 가져오기
    public List<Product.Category> findAllCategories() {
        return List.of(Product.Category.values());
    }
} 
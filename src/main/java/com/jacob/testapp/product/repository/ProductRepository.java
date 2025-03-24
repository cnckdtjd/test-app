package com.jacob.testapp.product.repository;

import com.jacob.testapp.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusEquals(Product.Status status, Pageable pageable);
    
    Page<Product> findByStatusEqualsAndNameContaining(Product.Status status, String name, Pageable pageable);
    
    Page<Product> findByStatusEqualsAndCategory(Product.Status status, Product.Category category, Pageable pageable);
    
    Page<Product> findByStatusEqualsAndNameContainingAndCategory(Product.Status status, String name, Product.Category category, Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = ?1")
    Optional<Product> findByIdWithPessimisticLock(Long id);
    
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM Product p WHERE p.id = ?1")
    Optional<Product> findByIdWithOptimisticLock(Long id);
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - ?2 WHERE p.id = ?1 AND p.stock >= ?2")
    int updateStockById(Long id, int quantity);
    
    List<Product> findTop10ByOrderByCreatedAtDesc();

    Page<Product> findByNameContaining(String name, Pageable pageable);
    Page<Product> findByCategory(Product.Category category, Pageable pageable);
    Page<Product> findByNameContainingAndCategory(String name, Product.Category category, Pageable pageable);
    
    /**
     * ID 범위에 해당하는 상품을 삭제
     * @param startId 시작 ID
     * @param endId 종료 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Product p WHERE p.id >= ?1 AND p.id <= ?2")
    void deleteByIdBetween(Long startId, Long endId);

    List<Product> findByCategoryOrderByPriceAsc(Product.Category category);
    List<Product> findByNameContainingIgnoreCase(String keyword);
    List<Product> findByNameStartingWith(String prefix);
    List<Product> findTop5ByOrderByCreatedAtDesc();

    Page<Product> findByStatusAndCategoryOrderByCreatedAtDesc(Product.Status status, Product.Category category, Pageable pageable);
    Page<Product> findByStatusOrderByCreatedAtDesc(Product.Status status, Pageable pageable);
    long countByStatus(Product.Status status);
    List<Product> findByRemarks(String remarks);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
    long countActiveProducts();
    
    // 관리자 페이지 추가 메소드
    
    /**
     * 상품명 + 카테고리 + 상태로 검색
     */
    Page<Product> findByNameContainingAndCategoryAndStatus(String name, Product.Category category, Product.Status status, Pageable pageable);
    
    /**
     * 상품명 + 상태로 검색
     */
    Page<Product> findByNameContainingAndStatus(String name, Product.Status status, Pageable pageable);
    
    /**
     * 카테고리 + 상태로 검색
     */
    Page<Product> findByCategoryAndStatus(Product.Category category, Product.Status status, Pageable pageable);
    
    /**
     * 상태로 검색
     */
    Page<Product> findByStatus(Product.Status status, Pageable pageable);
    
    /**
     * 재고가 특정 수량 이하인 상품 수 조회
     */
    long countByStockLessThanEqual(int stock);
    
    /**
     * 카테고리별 상품 수 조회
     */
    long countByCategory(Product.Category category);
    
    /**
     * 카테고리별 재고 일괄 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = ?2 WHERE p.category = ?1")
    int updateStockByCategory(Product.Category category, int stock);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
} 
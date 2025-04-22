package com.jacob.testapp.product.repository;

import com.jacob.testapp.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // ========== 기본 조회 메서드 ==========
    
    /**
     * 상태별 상품 조회
     */
    Page<Product> findByStatus(Product.Status status, Pageable pageable);
    
    /**
     * 카테고리별 상품 조회 
     */
    Page<Product> findByCategory(Product.Category category, Pageable pageable);
    
    /**
     * 상품명으로 부분 일치 검색
     */
    Page<Product> findByNameContaining(String name, Pageable pageable);
    
    /**
     * 상품명과 카테고리로 검색
     */
    Page<Product> findByNameContainingAndCategory(String name, Product.Category category, Pageable pageable);
    
    /**
     * 상태와 상품명으로 검색
     */
    Page<Product> findByStatusAndNameContaining(Product.Status status, String name, Pageable pageable);
    
    /**
     * 상태와 카테고리로 검색
     */
    Page<Product> findByStatusAndCategory(Product.Status status, Product.Category category, Pageable pageable);
    
    /**
     * 상태, 상품명, 카테고리로 검색
     */
    Page<Product> findByStatusAndNameContainingAndCategory(Product.Status status, String name, Product.Category category, Pageable pageable);

    // ========== 재고 관리 메서드 ==========
    
    /**
     * 낙관적 락을 사용한 상품 조회
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithOptimisticLock(@Param("id") Long id);
    
    /**
     * 비관적 락을 사용한 상품 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * 재고 감소
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    
    /**
     * 재고 증가
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    
    /**
     * 카테고리별 재고 일괄 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = :stock WHERE p.category = :category")
    int updateStockByCategory(@Param("category") Product.Category category, @Param("stock") int stock);
    
    /**
     * 재고가 특정 수량 이하인 상품 수 조회
     */
    long countByStockLessThanEqual(int stock);

    // ========== 통계 및 집계 메서드 ==========
    
    /**
     * 상태별 상품 수 조회
     */
    long countByStatus(Product.Status status);
    
    /**
     * 활성 상품 수 조회
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
    long countActiveProducts();
    
    /**
     * 카테고리별 상품 수 조회
     */
    long countByCategory(Product.Category category);

    // ========== 특수 목적 메서드 ==========
    
    /**
     * 최근 등록된 상품 10개 조회
     */
    List<Product> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 최근 등록된 상품 5개 조회
     */
    List<Product> findTop5ByOrderByCreatedAtDesc();
    
    /**
     * 카테고리별 가격 오름차순 정렬 조회
     */
    List<Product> findByCategoryOrderByPriceAsc(Product.Category category);
    
    /**
     * 상품명으로 대소문자 구분 없이 검색
     */
    List<Product> findByNameContainingIgnoreCase(String keyword);
    
    /**
     * 상품명 접두어로 검색
     */
    List<Product> findByNameStartingWith(String prefix);
    
    /**
     * 비고로 상품 검색
     */
    List<Product> findByRemarks(String remarks);
    
    /**
     * ID 범위 삭제 (테스트용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Product p WHERE p.id >= :startId AND p.id <= :endId")
    void deleteByIdBetween(@Param("startId") Long startId, @Param("endId") Long endId);
} 
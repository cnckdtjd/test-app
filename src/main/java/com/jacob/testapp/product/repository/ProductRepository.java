package com.jacob.testapp.product.repository;

import com.jacob.testapp.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);
    
    Page<Product> findByActiveTrueAndNameContaining(String name, Pageable pageable);
    
    Page<Product> findByActiveTrueAndCategory(Product.Category category, Pageable pageable);
    
    Page<Product> findByActiveTrueAndNameContainingAndCategory(String name, Product.Category category, Pageable pageable);
    
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
} 
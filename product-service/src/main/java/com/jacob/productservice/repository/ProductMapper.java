package com.jacob.productservice.repository;

import com.jacob.productservice.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    
    // 조회
    Product findById(Long id);
    List<Product> findAll();
    List<Product> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    List<Product> findByStatusEquals(@Param("status") Product.Status status, @Param("offset") int offset, @Param("limit") int limit);
    List<Product> findByStatusEqualsAndNameContaining(
            @Param("status") Product.Status status, 
            @Param("name") String name, 
            @Param("offset") int offset, 
            @Param("limit") int limit);
    List<Product> findByStatusEqualsAndCategory(
            @Param("status") Product.Status status, 
            @Param("category") Product.Category category, 
            @Param("offset") int offset, 
            @Param("limit") int limit);
    List<Product> findByStatusEqualsAndNameContainingAndCategory(
            @Param("status") Product.Status status, 
            @Param("name") String name, 
            @Param("category") Product.Category category, 
            @Param("offset") int offset, 
            @Param("limit") int limit);
            
    // 페이지네이션 관련 카운트
    long countAll();
    long countByStatus(Product.Status status);
    long countByStatusAndNameContaining(Product.Status status, String name);
    long countByStatusAndCategory(Product.Status status, Product.Category category);
    long countByStatusAndNameContainingAndCategory(Product.Status status, String name, Product.Category category);
    
    // 다양한 검색 기능
    List<Product> findByNameContaining(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);
    List<Product> findByNameContainingAndCategory(
            @Param("name") String name, 
            @Param("category") Product.Category category, 
            @Param("offset") int offset, 
            @Param("limit") int limit);
    List<Product> findByCategoryOrderByPriceAsc(Product.Category category);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByNameStartingWith(String prefix);
    List<Product> findTopByOrderByCreatedAtDesc(@Param("limit") int limit);
    
    // 생성 및 수정
    int save(Product product);
    int update(Product product);
    
    // 재고 관련
    int updateStockById(@Param("id") Long id, @Param("quantity") int quantity);
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
    
    // 삭제
    int deleteById(Long id);
    int deleteByIdBetween(@Param("startId") Long startId, @Param("endId") Long endId);
    
    // 관리자 기능
    List<Product> findByNameContainingAndCategoryAndStatus(
            @Param("name") String name, 
            @Param("category") Product.Category category, 
            @Param("status") Product.Status status, 
            @Param("offset") int offset, 
            @Param("limit") int limit);
    long countByStockLessThanEqual(int stock);
    long countByCategory(Product.Category category);
    int updateStockByCategory(@Param("category") Product.Category category, @Param("stock") int stock);
} 
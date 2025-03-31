package com.jacob.userservice.repository;

import com.jacob.userservice.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    // 조회
    User findById(Long id);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
    List<User> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    // 생성 및 수정
    int save(User user);
    int update(User user);
    
    // 로그인 관련
    void updateLoginAttempts(@Param("username") String username, @Param("attempts") int attempts);
    void updateAccountLocked(@Param("username") String username, @Param("locked") boolean locked);
    
    // 삭제
    int deleteById(Long id);
    int deleteByIdBetween(@Param("startId") Long startId, @Param("endId") Long endId);
    
    // 카운트
    long countAll();
    long countByStatus(User.Status status);
    
    // 존재 여부 확인
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
} 
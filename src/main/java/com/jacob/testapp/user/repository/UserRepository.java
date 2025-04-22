package com.jacob.testapp.user.repository;

import com.jacob.testapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 특정 상태의 사용자 수 조회
     */
    long countByStatus(User.Status status);
    
    /**
     * 사용자명 접두사로 사용자 목록 조회
     */
    List<User> findByUsernameStartingWith(String prefix);
    
    /**
     * 비고로 사용자 목록 조회
     */
    List<User> findByRemarks(String remarks);
    
    /**
     * 로그인 시도 횟수 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.loginAttempts = :attempts WHERE u.username = :username")
    void updateLoginAttempts(@Param("username") String username, @Param("attempts") int attempts);
    
    /**
     * 계정 잠금 상태 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLocked = :locked, u.status = CASE WHEN :locked = true THEN 'LOCKED' ELSE 'ACTIVE' END, u.lockTime = CASE WHEN :locked = true THEN CURRENT_TIMESTAMP ELSE NULL END WHERE u.username = :username")
    void updateAccountLockStatus(@Param("username") String username, @Param("locked") boolean locked);
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.username = :username")
    void updateLastLoginAt(@Param("username") String username, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    /**
     * ID 범위에 해당하는 사용자를 삭제 (테스트용)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id >= :startId AND u.id <= :endId AND u.role = com.jacob.testapp.user.entity.User.Role.ROLE_USER")
    void deleteByIdBetween(@Param("startId") Long startId, @Param("endId") Long endId);
} 
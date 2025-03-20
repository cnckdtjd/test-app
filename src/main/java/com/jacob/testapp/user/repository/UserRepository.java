package com.jacob.testapp.user.repository;

import com.jacob.testapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = ?2 WHERE u.username = ?1")
    void updateLoginAttempts(String username, int attempts);
    
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = ?2 WHERE u.username = ?1")
    void updateAccountLocked(String username, boolean locked);
    
    /**
     * ID 범위에 해당하는 사용자를 삭제
     * @param startId 시작 ID
     * @param endId 종료 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id >= ?1 AND u.id <= ?2 AND u.role = com.jacob.testapp.user.entity.User.Role.ROLE_USER")
    void deleteByIdBetween(Long startId, Long endId);

    List<User> findByUsernameStartingWith(String prefix);
} 
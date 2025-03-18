package com.jacob.testapp.user.repository;

import com.jacob.testapp.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
} 
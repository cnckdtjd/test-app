package com.jacob.adminservice.mapper;

import com.jacob.adminservice.model.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AdminUserMapper {
    List<AdminUser> findAll();
    AdminUser findById(Long id);
    AdminUser findByUsername(String username);
    void insert(AdminUser adminUser);
    void update(AdminUser adminUser);
    void delete(Long id);
} 
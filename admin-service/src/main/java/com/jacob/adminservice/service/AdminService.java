package com.jacob.adminservice.service;

import com.jacob.adminservice.client.CartServiceClient;
import com.jacob.adminservice.client.OrderServiceClient;
import com.jacob.adminservice.client.ProductServiceClient;
import com.jacob.adminservice.client.UserServiceClient;
import com.jacob.adminservice.mapper.AdminUserMapper;
import com.jacob.adminservice.model.AdminUser;
import com.jacob.adminservice.model.Dashboard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final CartServiceClient cartServiceClient;
    
    public List<AdminUser> getAllAdminUsers() {
        return adminUserMapper.findAll();
    }
    
    public AdminUser getAdminUserById(Long id) {
        return adminUserMapper.findById(id);
    }
    
    public AdminUser getAdminUserByUsername(String username) {
        return adminUserMapper.findByUsername(username);
    }
    
    @Transactional
    public void createAdminUser(AdminUser adminUser) {
        adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword()));
        adminUserMapper.insert(adminUser);
    }
    
    @Transactional
    public void updateAdminUser(AdminUser adminUser) {
        adminUserMapper.update(adminUser);
    }
    
    @Transactional
    public void deleteAdminUser(Long id) {
        adminUserMapper.delete(id);
    }
    
    public Dashboard getDashboardData() {
        Dashboard dashboard = new Dashboard();
        
        // 각 서비스에서 통계 데이터 수집
        dashboard.setTotalUsers(userServiceClient.getTotalUsers());
        dashboard.setActiveUsers(userServiceClient.getActiveUsers());
        
        dashboard.setTotalProducts(productServiceClient.getTotalProducts());
        dashboard.setLowStockProducts(productServiceClient.getLowStockProducts());
        
        dashboard.setTotalOrders(orderServiceClient.getTotalOrders());
        dashboard.setTotalRevenue(orderServiceClient.getTotalRevenue());
        dashboard.setNewOrdersToday(orderServiceClient.getNewOrdersToday());
        
        return dashboard;
    }
} 
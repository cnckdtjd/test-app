package com.jacob.adminservice.controller;

import com.jacob.adminservice.model.AdminUser;
import com.jacob.adminservice.model.Dashboard;
import com.jacob.adminservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    @GetMapping("/dashboard")
    public ResponseEntity<Dashboard> getDashboard() {
        Dashboard dashboard = adminService.getDashboardData();
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<AdminUser>> getAllAdminUsers() {
        List<AdminUser> adminUsers = adminService.getAllAdminUsers();
        return ResponseEntity.ok(adminUsers);
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUser> getAdminUserById(@PathVariable Long id) {
        AdminUser adminUser = adminService.getAdminUserById(id);
        if (adminUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(adminUser);
    }
    
    @PostMapping("/users")
    public ResponseEntity<?> createAdminUser(@RequestBody AdminUser adminUser) {
        adminService.createAdminUser(adminUser);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateAdminUser(@PathVariable Long id, @RequestBody AdminUser adminUser) {
        adminUser.setId(id);
        adminService.updateAdminUser(adminUser);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteAdminUser(@PathVariable Long id) {
        adminService.deleteAdminUser(id);
        return ResponseEntity.ok().build();
    }
} 
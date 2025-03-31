package com.jacob.adminservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminViewController {

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }
    
    @GetMapping("/users")
    public String usersPage() {
        return "users";
    }
    
    @GetMapping("/products")
    public String productsPage() {
        return "products";
    }
    
    @GetMapping("/products/new")
    public String newProductPage() {
        return "product-form";
    }
    
    @GetMapping("/products/{productId}/edit")
    public String editProductPage(@PathVariable Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "product-form";
    }
    
    @GetMapping("/orders")
    public String ordersPage() {
        return "orders";
    }
    
    @GetMapping("/orders/{orderId}")
    public String orderDetailPage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "order-detail";
    }
    
    @GetMapping("/statistics")
    public String statisticsPage() {
        return "statistics";
    }
    
    @GetMapping("/test-data")
    public String testDataPage() {
        return "test-data";
    }
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
} 
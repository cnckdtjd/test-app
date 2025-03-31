package com.jacob.orderservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderViewController {

    @GetMapping("/orders")
    public String orderListPage() {
        return "order-list";
    }
    
    @GetMapping("/orders/{orderId}")
    public String orderDetailPage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "order-detail";
    }
    
    @GetMapping("/orders/{orderId}/complete")
    public String orderCompletePage(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "order-complete";
    }
    
    @GetMapping("/checkout")
    public String checkoutPage() {
        return "checkout";
    }
    
    @GetMapping("/checkout/direct")
    public String directCheckoutPage() {
        return "checkout";
    }
} 
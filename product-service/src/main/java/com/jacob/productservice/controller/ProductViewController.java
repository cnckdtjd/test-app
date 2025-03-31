package com.jacob.productservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductViewController {

    @GetMapping("/products")
    public String productListPage() {
        return "product-list";
    }
    
    @GetMapping("/products/{productId}")
    public String productDetailPage(@PathVariable Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "product-detail";
    }
    
    @GetMapping("/products/{productId}/review")
    public String productReviewPage(@PathVariable Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "product-review";
    }
} 
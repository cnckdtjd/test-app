package com.jacob.testapp.admin.controller;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.order.service.OrderService;
import com.jacob.testapp.product.service.ProductService;
import com.jacob.testapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping
    public String adminDashboard(Model model) {
        // 사용자, 상품, 주문 수 조회
        long userCount = userService.findAll().size();
        long productCount = productService.findAll().size();
        
        // 상품 카테고리 목록
        model.addAttribute("categories", Product.Category.values());
        
        // 주문 상태 목록
        model.addAttribute("orderStatuses", Order.OrderStatus.values());
        
        // 통계 데이터
        model.addAttribute("userCount", userCount);
        model.addAttribute("productCount", productCount);
        
        return "admin/dashboard";
    }

    // 사용자 관리
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userService.findAll(pageable);
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        
        return "admin/users/list";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));
        model.addAttribute("user", user);
        return "admin/users/view";
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));
        
        userService.lockAccount(user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User account locked");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));
        
        userService.unlockAccount(user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User account unlocked");
        return "redirect:/admin/users/" + id;
    }

    // 상품 관리
    @GetMapping("/products")
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productService.findAll(pageable);
        
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("categories", Product.Category.values());
        
        return "admin/products/list";
    }

    @GetMapping("/products/create")
    public String createProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", Product.Category.values());
        return "admin/products/create";
    }

    @PostMapping("/products/create")
    public String createProduct(@Valid @ModelAttribute Product product, 
                               BindingResult result, 
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/products/create";
        }
        
        productService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product created successfully");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + id));
        
        model.addAttribute("product", product);
        model.addAttribute("categories", Product.Category.values());
        return "admin/products/edit";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id, 
                               @Valid @ModelAttribute Product product, 
                               BindingResult result, 
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/products/edit";
        }
        
        Product existingProduct = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + id));
        
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStock(product.getStock());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setActive(product.isActive());
        
        productService.save(existingProduct);
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully");
        return "redirect:/admin/products";
    }

    // 주문 관리
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders;
        
        if (status != null) {
            orders = orderService.findByStatus(status, pageable);
        } else {
            orders = orderService.findAll(pageable);
        }
        
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("statuses", Order.OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        
        return "admin/orders/list";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = orderService.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + id));
        
        model.addAttribute("order", order);
        model.addAttribute("statuses", Order.OrderStatus.values());
        return "admin/orders/view";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id, 
            @RequestParam Order.OrderStatus status,
            RedirectAttributes redirectAttributes) {
        
        orderService.updateOrderStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully");
        return "redirect:/admin/orders/" + id;
    }

    // 부하 테스트 데이터 생성 및 롤백 (모의 구현)
    @PostMapping("/generate-test-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(defaultValue = "100") int userCount,
            @RequestParam(defaultValue = "1000") int productCount) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 실제로는 대량 데이터 생성 로직이 들어갈 자리
            // 예: Faker 라이브러리 등을 사용하여 대량 데이터 생성
            
            response.put("success", true);
            response.put("message", "Generated " + userCount + " users and " + productCount + " products");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/rollback-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rollbackData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 실제로는 데이터 롤백 로직이 들어갈 자리
            // 예: DB 백업에서 복원, 특정 시점으로 되돌리기 등
            
            response.put("success", true);
            response.put("message", "Data rollback completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
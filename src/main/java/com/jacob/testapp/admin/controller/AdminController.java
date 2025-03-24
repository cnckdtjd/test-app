package com.jacob.testapp.admin.controller;

import com.jacob.testapp.admin.service.StatisticsService;
import com.jacob.testapp.admin.service.SystemMonitorService;
import com.jacob.testapp.admin.service.TestDataService;
import com.jacob.testapp.admin.service.UserExportService;
import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.order.service.OrderService;
import com.jacob.testapp.product.service.ProductService;
import com.jacob.testapp.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final SystemMonitorService systemMonitorService;
    private final TestDataService testDataService;
    private final StatisticsService statisticsService;
    private final UserExportService userExportService;

    /**
     * 모든 관리자 컨트롤러 메소드에 requestURI 모델 속성을 자동으로 추가합니다.
     * Thymeleaf 3.1 이상에서는 #request 객체에 직접 접근할 수 없기 때문에 필요합니다.
     */
    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request, Model model) {
        model.addAttribute("requestURI", request.getRequestURI());
    }

    /**
     * 관리자 대시보드
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("관리자 대시보드 페이지 요청");
        
        try {
            // 사용자 통계
            long userCount = userService.countUsers();
            model.addAttribute("userCount", userCount);
            
            // 상품 통계
            long productCount = productService.countProducts();
            model.addAttribute("productCount", productCount);
            
            // 주문 통계 (임시 더미 데이터, 실제로는 OrderService에서 가져와야 함)
            model.addAttribute("orderCount", 0);
            model.addAttribute("totalSales", "0");
            
            // 시스템 자원 사용량 정보
            Map<String, Object> systemResources = systemMonitorService.getSystemResources();
            model.addAttribute("systemResources", systemResources);
            
            return "admin/dashboard";
        } catch (Exception e) {
            log.error("대시보드 데이터 로딩 중 오류 발생", e);
            model.addAttribute("errorMessage", "대시보드 데이터를 불러오는 중 오류가 발생했습니다.");
            return "admin/error";
        }
    }

    /**
     * 루트 경로 요청을 대시보드로 리다이렉트
     */
    @GetMapping("")
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
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
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "admin/users/view";
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        
        userService.lockAccount(user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "사용자 계정이 잠금 처리되었습니다.");

        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        
        userService.unlockAccount(user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "사용자 계정 잠금이 해제되었습니다.");

        return "redirect:/admin/users/" + id;
    }
    
    /**
     * 사용자 목록 CSV 내보내기
     */
    @GetMapping("/users/export")
    public void exportUsersToCsv(HttpServletResponse response) throws Exception {
        log.debug("사용자 목록 CSV 내보내기 요청");
        
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv");
        
        byte[] csvData = userExportService.exportUsersToCsv();
        response.getOutputStream().write(csvData);
        response.getOutputStream().flush();
    }
    
    /**
     * 사용자 인증 정보 CSV 내보내기
     */
    @GetMapping("/users/export-credentials")
    public void exportUserCredentialsToCsv(HttpServletResponse response) throws Exception {
        log.debug("사용자 인증 정보 CSV 내보내기 요청");
        
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_credentials.csv");
        
        byte[] csvData = userExportService.exportUserCredentialsToCsv();
        response.getOutputStream().write(csvData);
        response.getOutputStream().flush();
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
        redirectAttributes.addFlashAttribute("successMessage", "상품이 성공적으로 생성되었습니다.");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        
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
        
        Product existingProduct = productService.findById(id);
        
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStock(product.getStock());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStatus(product.getStatus());
        
        productService.save(existingProduct);
        redirectAttributes.addFlashAttribute("successMessage", "상품이 성공적으로 업데이트되었습니다.");
        
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
        redirectAttributes.addFlashAttribute("successMessage", "주문 상태가 성공적으로 업데이트되었습니다.");
        return "redirect:/admin/orders/" + id;
    }
    
    /**
     * 상세 통계 화면
     */
    @GetMapping("/details")
    public String details(Model model) {
        log.info("관리자 상세 통계 페이지 요청");
        
        try {
            // 사용자 통계
            Map<String, Object> userStats = new HashMap<>();
            userStats.put("totalUsers", userService.countUsers());
            userStats.put("activeUsers", userService.countUsersByStatus(com.jacob.testapp.user.entity.User.Status.ACTIVE));
            userStats.put("lockedUsers", userService.countUsersByStatus(com.jacob.testapp.user.entity.User.Status.LOCKED));
            userStats.put("newUsers", 0); // 최근 30일 신규 가입자 (실제 구현 필요)
            userStats.put("monthlyRegistrations", "{}"); // 월별 가입자 데이터 (실제 구현 필요)
            model.addAttribute("userStats", userStats);
            
            // 상품 통계
            Map<String, Object> productStats = new HashMap<>();
            productStats.put("totalProducts", productService.countProducts());
            productStats.put("activeProducts", productService.countActiveProducts());
            productStats.put("inactiveProducts", 0); // 비활성 상품 (실제 구현 필요)
            productStats.put("outOfStockProducts", 0); // 재고 없는 상품 (실제 구현 필요)
            productStats.put("productsByCategory", "{}"); // 카테고리별 상품 수 (실제 구현 필요)
            productStats.put("productsByPriceRange", "{}"); // 가격대별 상품 수 (실제 구현 필요)
            model.addAttribute("productStats", productStats);
            
            // 주문 통계 (임시 더미 데이터, 실제로는 OrderService에서 가져와야 함)
            Map<String, Object> orderStats = new HashMap<>();
            orderStats.put("totalOrders", 0);
            orderStats.put("ordersByStatus", "{}");
            orderStats.put("totalSales", "0");
            orderStats.put("monthlySales", "{}");
            model.addAttribute("orderStats", orderStats);
            
            return "admin/details";
        } catch (Exception e) {
            log.error("상세 통계 데이터 로딩 중 오류 발생", e);
            model.addAttribute("errorMessage", "상세 통계 데이터를 불러오는 중 오류가 발생했습니다.");
            return "admin/error";
        }
    }

    /**
     * 테스트 데이터 생성 API
     */
    @PostMapping("/test-data/generate")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateTestData(
            @RequestParam(defaultValue = "10") int userCount, 
            @RequestParam(defaultValue = "50") int productCount) {
        
        log.info("테스트 데이터 생성 요청: 사용자 {}명, 상품 {}개", userCount, productCount);
        
        if (userCount > 1000) {
            userCount = 1000; // 사용자 수 제한
        }
        
        if (productCount > 5000) {
            productCount = 5000; // 상품 수 제한
        }
        
        try {
            String result = testDataService.generateTestData(userCount, productCount);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("테스트 데이터 생성 중 오류 발생", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "테스트 데이터 생성 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 테스트 데이터 롤백 API
     */
    @PostMapping("/test-data/rollback")
    @ResponseBody
    public ResponseEntity<Map<String, String>> rollbackTestData() {
        log.info("테스트 데이터 롤백 요청");
        
        try {
            String result = testDataService.rollbackTestData();
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("테스트 데이터 롤백 중 오류 발생", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "테스트 데이터 롤백 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 
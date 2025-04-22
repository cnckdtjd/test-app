package com.jacob.testapp.admin.controller;

import com.jacob.testapp.admin.service.OrderAdminService;
import com.jacob.testapp.admin.service.StatisticsService;
import com.jacob.testapp.admin.service.SystemMonitorService;
import com.jacob.testapp.admin.service.TestDataService;
import com.jacob.testapp.admin.service.UserExportService;
import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.order.service.OrderService;
import com.jacob.testapp.product.service.ProductManagementService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.NumberFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final OrderAdminService orderAdminService;
    private final SystemMonitorService systemMonitorService;
    private final TestDataService testDataService;
    private final StatisticsService statisticsService;
    private final UserExportService userExportService;
    private final ProductManagementService productManagementService;

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
            // 대시보드 통계 데이터를 얻어옴
            Map<String, Object> stats = getDashboardStatistics();
            model.addAllAttributes(stats);
            
            return "admin/dashboard";
        } catch (Exception e) {
            log.error("대시보드 데이터 로딩 중 오류 발생", e);
            model.addAttribute("errorMessage", "대시보드 데이터를 불러오는 중 오류가 발생했습니다.");
            return "admin/error";
        }
    }

    /**
     * 대시보드 통계 데이터 API - AJAX 요청용
     */
    @GetMapping("/dashboard-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.info("대시보드 통계 데이터 API 요청");
        
        try {
            Map<String, Object> stats = getDashboardStatistics();
            
            // API 응답용 추가 포맷팅 - 총 매출 통화 형식으로 변환
            if (stats.containsKey("totalSales")) {
                Double totalSales = (Double) stats.get("totalSales");
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.KOREA);
                stats.put("totalSales", formatter.format(totalSales));
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("대시보드 통계 데이터 API 요청 처리 중 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 대시보드 통계 데이터를 얻는 내부 메서드
     */
    private Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 사용자 통계
        long userCount = userService.countUsers();
        stats.put("userCount", userCount);
        
        // 상품 통계
        long productCount = productService.countProducts();
        stats.put("productCount", productCount);
        
        // 주문 통계 (OrderAdminService에서 가져오기)
        Map<String, Object> orderStats = orderAdminService.getOrderStatistics();
        long orderCount = (long) orderStats.getOrDefault("totalOrders", 0L);
        Double totalSales = (Double) orderStats.getOrDefault("totalSales", 0.0);
        
        stats.put("orderCount", orderCount);
        stats.put("totalSales", totalSales);
        
        // 시스템 자원 사용량 정보
        Map<String, Object> systemResources = systemMonitorService.getSystemResources();
        stats.put("systemResources", systemResources);
        
        return stats;
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
        return updateUserAccountStatus(id, true, "사용자 계정이 잠금 처리되었습니다.", redirectAttributes);
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return updateUserAccountStatus(id, false, "사용자 계정 잠금이 해제되었습니다.", redirectAttributes);
    }
    
    /**
     * 사용자 계정 잠금 상태를 업데이트하는 내부 메서드
     */
    private String updateUserAccountStatus(Long id, boolean lockAccount, String successMessage, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        
        if (lockAccount) {
            userService.lockAccount(user.getUsername());
        } else {
            userService.unlockAccount(user.getUsername());
        }
        
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * 사용자 현금 잔액 업데이트
     */
    @PostMapping("/users/{id}/update-balance")
    public String updateUserBalance(
            @PathVariable Long id, 
            @RequestParam Long cashBalance,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);
            user.setCashBalance(cashBalance);
            userService.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "현금 잔액이 " + cashBalance + "원으로 변경되었습니다.");
        } catch (Exception e) {
            log.error("사용자 잔액 업데이트 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "현금 잔액 업데이트 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * 사용자 현금 잔액 충전
     */
    @PostMapping("/users/{id}/add-balance")
    public String addUserBalance(
            @PathVariable Long id, 
            @RequestParam Long amount,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);
            Long newBalance = user.getCashBalance() + amount;
            updateUserCashBalance(user, newBalance, amount + "원이 충전되었습니다. 현재 잔액: " + newBalance + "원", redirectAttributes);
        } catch (Exception e) {
            log.error("사용자 잔액 충전 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "현금 잔액 충전 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * 사용자 현금 잔액 차감
     */
    @PostMapping("/users/{id}/subtract-balance")
    public String subtractUserBalance(
            @PathVariable Long id, 
            @RequestParam Long amount,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);
            if (user.getCashBalance() < amount) {
                redirectAttributes.addFlashAttribute("errorMessage", "차감하려는 금액(" + amount + "원)이 현재 잔액(" + user.getCashBalance() + "원)보다 큽니다.");
            } else {
                Long newBalance = user.getCashBalance() - amount;
                updateUserCashBalance(user, newBalance, amount + "원이 차감되었습니다. 현재 잔액: " + newBalance + "원", redirectAttributes);
            }
        } catch (Exception e) {
            log.error("사용자 잔액 차감 중 오류 발생: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "현금 잔액 차감 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * 사용자 현금 잔액을 업데이트하는 내부 메서드
     */
    private void updateUserCashBalance(User user, Long newBalance, String successMessage, RedirectAttributes redirectAttributes) {
        user.setCashBalance(newBalance);
        userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
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
        log.info("사용자 인증 정보 CSV 내보내기 요청");
        
        // 응답 헤더 설정 - CSV 파일임을 명확히 함
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_credentials.csv");
        
        try {
            byte[] csvData = userExportService.exportUserCredentialsToCsv();
            log.info("CSV 데이터 크기: {} 바이트", csvData.length);
            
            // 데이터가 비어있는지 확인
            if (csvData.length <= 3) { // UTF-8 BOM이 3바이트
                log.warn("생성된 CSV 데이터가 비어 있습니다!");
            }
            
            response.setContentLength(csvData.length);
            response.getOutputStream().write(csvData);
            response.getOutputStream().flush();
            log.info("사용자 인증정보 CSV 다운로드 완료");
        } catch (Exception e) {
            log.error("사용자 인증정보 CSV 내보내기 중 오류 발생: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("인증 정보 내보내기 중 오류가 발생했습니다: " + e.getMessage());
            response.getWriter().flush();
        }
    }

    // 상품 관리 - 개선된 버전
    @GetMapping("/products")
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            Model model) {
        
        log.info("상품 목록 페이지 요청 - 페이지: {}, 키워드: {}, 카테고리: {}, 상태: {}", page, keyword, category, status);
        
        // 정렬 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        
        // 카테고리, 상태 파라미터 처리
        Product.Category categoryEnum = null;
        Product.Status statusEnum = null;
        
        if (category != null && !category.isEmpty()) {
            try {
                categoryEnum = Product.Category.valueOf(category);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 카테고리 값: {}", category);
            }
        }
        
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = Product.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 상태 값: {}", status);
            }
        }
        
        // 검색 조건에 따라 상품 조회
        Page<Product> products = productManagementService.searchProducts(keyword, categoryEnum, statusEnum, pageable);
        
        model.addAttribute("products", products);
        model.addAttribute("categories", Product.Category.values());
        
        return "admin/products/list";
    }

    @GetMapping("/products/create")
    public String createProductForm(Model model) {
        log.info("상품 등록 폼 요청");
        model.addAttribute("product", new Product());
        model.addAttribute("categories", Product.Category.values());
        return "admin/products/create";
    }

    @PostMapping("/products/create")
    public String createProduct(@Valid @ModelAttribute Product product, 
                               BindingResult result, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        log.info("상품 등록 요청: {}", product.getName());
        
        if (result.hasErrors()) {
            log.warn("상품 등록 폼 유효성 검사 실패: {}", result.getAllErrors());
            model.addAttribute("categories", Product.Category.values());
            return "admin/products/create";
        }
        
        return processProductSave(product, "상품이 성공적으로 등록되었습니다.", "상품 등록 중 오류가 발생했습니다", 
                "admin/products/create", model, redirectAttributes);
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        log.info("상품 수정 폼 요청: ID = {}", id);
        
        try {
            Product product = productService.findById(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", Product.Category.values());
            return "admin/products/edit";
        } catch (Exception e) {
            log.error("상품 정보 조회 중 오류 발생: ID = {}", id, e);
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id, 
                               @Valid @ModelAttribute Product product, 
                               BindingResult result, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        log.info("상품 수정 요청: ID = {}, 이름 = {}", id, product.getName());
        
        if (result.hasErrors()) {
            log.warn("상품 수정 폼 유효성 검사 실패: {}", result.getAllErrors());
            model.addAttribute("categories", Product.Category.values());
            return "admin/products/edit";
        }
        
        return processProductSave(product, "상품이 성공적으로 수정되었습니다.", "상품 수정 중 오류가 발생했습니다", 
                "admin/products/edit", model, redirectAttributes);
    }
    
    /**
     * 상품 저장 처리를 위한 공통 메서드
     */
    private String processProductSave(Product product, String successMessage, String errorMessage, 
                                     String errorViewName, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product savedProduct = productManagementService.saveProduct(product);
            log.info("상품 저장 완료: ID = {}", savedProduct.getId());
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("상품 저장 중 오류 발생", e);
            model.addAttribute("errorMessage", errorMessage + ": " + e.getMessage());
            model.addAttribute("categories", Product.Category.values());
            return errorViewName;
        }
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("상품 삭제 요청: ID = {}", id);
        
        try {
            productManagementService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "상품이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            log.error("상품 삭제 중 오류 발생: ID = {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "상품 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @GetMapping("/products/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        log.info("상품 통계 정보 API 요청");
        return getStatisticsResponse(productManagementService::getProductStatistics, "상품 통계 정보 조회 중 오류 발생");
    }

    /**
     * 상품 재고 업데이트 API
     */
    @PostMapping("/products/{id}/stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProductStock(
            @PathVariable Long id,
            @RequestParam int stock) {
        log.info("상품 재고 업데이트 요청: ID = {}, 재고 = {}", id, stock);

        Map<String, Object> response = new HashMap<>();

        try {
            if (stock < 0) {
                response.put("success", false);
                response.put("message", "재고는 0 이상이어야 합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Product product = productService.findById(id);
            product.setStock(stock);
            productManagementService.saveProduct(product);

            response.put("success", true);
            response.put("message", "재고가 성공적으로 업데이트되었습니다.");
            response.put("newStock", stock);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("상품 재고 업데이트 중 오류 발생: ID = {}", id, e);
            response.put("success", false);
            response.put("message", "재고 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 주문 관리 - 개선된 버전
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        log.info("주문 목록 페이지 요청 - 페이지: {}, 주문번호: {}, 상태: {}, 시작일: {}, 종료일: {}", 
                page, orderNumber, status, startDate, endDate);
        
        // 정렬 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 검색 조건에 따라 주문 조회
        Page<Order> orders = orderAdminService.searchOrders(orderNumber, status, startDate, endDate, pageable);
        
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", Order.OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        
        return "admin/orders/list";
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        log.info("주문 상세 페이지 요청 - 주문 ID: {}", id);
        
        try {
            Order order = orderAdminService.getOrderDetails(id);
            model.addAttribute("order", order);
            model.addAttribute("statuses", Order.OrderStatus.values());
            return "admin/orders/view";
        } catch (Exception e) {
            log.error("주문 정보 조회 중 오류 발생: ID = {}", id, e);
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            RedirectAttributes redirectAttributes) {
        return processOrderUpdate(id, 
            () -> orderAdminService.updateOrderStatus(id, status, "관리자에 의한 상태 변경"),
            "주문 상태가 성공적으로 업데이트되었습니다.",
            "주문 상태 업데이트 중 오류가 발생했습니다",
            redirectAttributes);
    }

    @PostMapping("/orders/{id}/shipping")
    public String updateShippingInfo(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam String carrier,
            RedirectAttributes redirectAttributes) {
        return processOrderUpdate(id, 
            () -> orderAdminService.updateShippingInfo(id, trackingNumber, carrier),
            "배송 정보가 성공적으로 업데이트되었습니다.",
            "배송 정보 업데이트 중 오류가 발생했습니다",
            redirectAttributes);
    }

    @PostMapping("/orders/{id}/memo")
    public String updateOrderMemo(
            @PathVariable Long id,
            @RequestParam String adminMemo,
            RedirectAttributes redirectAttributes) {
        return processOrderUpdate(id, 
            () -> orderAdminService.updateAdminMemo(id, adminMemo),
            "주문 메모가 성공적으로 업데이트되었습니다.",
            "주문 메모 업데이트 중 오류가 발생했습니다",
            redirectAttributes);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            @RequestParam String cancelReason,
            RedirectAttributes redirectAttributes) {
        return processOrderUpdate(id, 
            () -> orderAdminService.cancelOrder(id, cancelReason),
            "주문이 성공적으로 취소되었습니다.",
            "주문 취소 중 오류가 발생했습니다",
            redirectAttributes);
    }
    
    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(
            @PathVariable Long id,
            @RequestParam String deleteReason,
            RedirectAttributes redirectAttributes) {
        log.info("주문 이력 삭제 요청 - 주문 ID: {}, 삭제 사유: {}", id, deleteReason);
        
        try {
            orderAdminService.markOrderAsDeleted(id, deleteReason);
            redirectAttributes.addFlashAttribute("successMessage", "주문 이력이 성공적으로 삭제되었습니다.");
            return "redirect:/admin/orders"; // 목록 페이지로 리다이렉트
        } catch (Exception e) {
            log.error("주문 이력 삭제 중 오류 발생: ID = {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "주문 이력 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/orders/" + id;
        }
    }
    
    /**
     * 주문 업데이트 작업을 처리하는 공통 메서드
     * @param id 주문 ID
     * @param updateAction 업데이트 작업을 실행할 함수
     * @param successMessage 성공 메시지
     * @param errorPrefix 에러 메시지 접두사
     * @param redirectAttributes 리다이렉트 속성
     * @return 리다이렉트 URL
     */
    private String processOrderUpdate(
            Long id, 
            Supplier<Order> updateAction,
            String successMessage,
            String errorPrefix,
            RedirectAttributes redirectAttributes) {
        
        try {
            updateAction.get();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (Exception e) {
            log.error("{}:  ID = {}", errorPrefix, id, e);
            redirectAttributes.addFlashAttribute("errorMessage", errorPrefix + ": " + e.getMessage());
        }
        
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/orders/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        log.info("주문 통계 정보 API 요청");
        return getStatisticsResponse(orderAdminService::getOrderStatistics, "주문 통계 정보 조회 중 오류 발생");
    }
    
    @GetMapping("/orders/export")
    public void exportOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Order.OrderStatus status,
            HttpServletResponse response) throws Exception {
        
        log.info("주문 내보내기 요청 - 시작일: {}, 종료일: {}, 상태: {}", startDate, endDate, status);
        
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx");
        
        // 주문 내보내기 구현은 실제 서비스 클래스에서 처리 (현재는 미구현)
        // orderExportService.exportOrdersToExcel(startDate, endDate, status, response.getOutputStream());
        
        response.getOutputStream().flush();
    }

    @GetMapping("/orders/invoice/{id}")
    public String viewOrderInvoice(@PathVariable Long id, Model model) {
        log.info("주문서 인쇄 페이지 요청 - 주문 ID: {}", id);
        
        try {
            Order order = orderAdminService.getOrderDetails(id);
            model.addAttribute("order", order);
            return "admin/orders/invoice";
        } catch (Exception e) {
            log.error("주문 정보 조회 중 오류 발생: ID = {}", id, e);
            return "redirect:/admin/orders";
        }
    }

    /**
     * 상세 통계 화면
     */
    @GetMapping("/details")
    public String details(Model model) {
        log.info("관리자 상세 통계 페이지 요청");
        
        try {
            // 사용자 통계
            Map<String, Object> userStats = statisticsService.getUserStatistics();
            model.addAttribute("userStats", userStats);
            
            // 상품 통계
            Map<String, Object> productStats = statisticsService.getProductStatistics();
            model.addAttribute("productStats", productStats);
            
            // 주문 통계
            Map<String, Object> orderStats = statisticsService.getOrderStatistics();
            
            // 주문 상태별 통계 추가 처리
            if (orderStats.containsKey("ordersByStatus")) {
                Map<String, Long> ordersByStatus = (Map<String, Long>) orderStats.get("ordersByStatus");
                long completedOrders = ordersByStatus.getOrDefault("COMPLETED", 0L);
                long pendingOrders = ordersByStatus.getOrDefault("PENDING", 0L) + 
                                    ordersByStatus.getOrDefault("PAID", 0L) + 
                                    ordersByStatus.getOrDefault("SHIPPING", 0L);
                
                orderStats.put("completedOrders", completedOrders);
                orderStats.put("pendingOrders", pendingOrders);
            }
            
            // 매출 포맷팅
            if (orderStats.containsKey("totalSales")) {
                Double totalSales = (Double) orderStats.get("totalSales");
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.KOREA);
                orderStats.put("totalSales", formatter.format(totalSales));
            }
            
            // Chart.js에서 사용할 JSON 문자열로 변환
            try {
                if (orderStats.containsKey("monthlyOrders")) {
                    orderStats.put("monthlyOrders", new ObjectMapper().writeValueAsString(orderStats.get("monthlyOrders")));
                }
                
                if (orderStats.containsKey("monthlySales")) {
                    orderStats.put("monthlySales", new ObjectMapper().writeValueAsString(orderStats.get("monthlySales")));
                }
                
                if (userStats.containsKey("monthlyRegistrations")) {
                    userStats.put("monthlyRegistrations", new ObjectMapper().writeValueAsString(userStats.get("monthlyRegistrations")));
                }
            } catch (Exception e) {
                log.warn("통계 데이터 JSON 변환 중 오류", e);
            }
            
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
            log.info("롤백 결과: {}", result);
            
            // 롤백 결과 메시지 파싱하여 삭제된 데이터 수 확인
            int deletedUsers = 0;
            int deletedProducts = 0;
            
            if (result.contains("사용자")) {
                try {
                    String userPart = result.split("사용자 ")[1].split("명")[0];
                    deletedUsers = Integer.parseInt(userPart.trim());
                } catch (Exception e) {
                    log.warn("롤백 결과에서 사용자 수 파싱 실패", e);
                }
            }
            
            if (result.contains("상품")) {
                try {
                    String productPart = result.split("상품 ")[1].split("개")[0];
                    deletedProducts = Integer.parseInt(productPart.trim());
                } catch (Exception e) {
                    log.warn("롤백 결과에서 상품 수 파싱 실패", e);
                }
            }
            
            log.info("롤백된 데이터: 사용자 {}명, 상품 {}개", deletedUsers, deletedProducts);
            
            if (deletedUsers == 0 && deletedProducts == 0) {
                log.warn("롤백 결과는 성공이지만 삭제된 데이터가 없습니다!");
            }
            
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

    /**
     * 통계 데이터 API 응답을 처리하는 공통 메서드
     * @param statisticsProvider 통계 데이터를 제공하는 함수형 인터페이스
     * @param errorMessage 오류 발생 시 로깅할 메시지
     * @return 통계 데이터 또는 오류 응답
     */
    private ResponseEntity<Map<String, Object>> getStatisticsResponse(
            Supplier<Map<String, Object>> statisticsProvider, 
            String errorMessage) {
        try {
            Map<String, Object> statistics = statisticsProvider.get();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 
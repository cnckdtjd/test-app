package com.jacob.testapp.order.controller;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.service.CartService;
import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.service.OrderService;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final CartService cartService;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model) {
        
        User user = getCurrentUser(principal);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByUser(user, pageable);
        
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        
        return "order/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Principal principal, Model model) {
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            Order order = getOrderAndValidateOwnership(id, user);
            
            model.addAttribute("order", order);
            return "order/detail";
        }, "redirect:/orders");
    }

    @GetMapping("/checkout")
    public String checkout(Model model, Principal principal) {
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            
            // 사용자의 장바구니 조회
            Cart cart = cartService.getCart(user);
            
            if (cart.getCartItems().isEmpty()) {
                return "redirect:/cart";
            }
            
            model.addAttribute("cart", cart);
            model.addAttribute("user", user);
            model.addAttribute("totalAmount", cart.getTotalPrice());
            
            return "order/checkout";
        }, "redirect:/cart");
    }
    
    @PostMapping("/create-and-pay")
    public String createAndPayOrder(Principal principal, RedirectAttributes redirectAttributes) {
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            
            // 주문 생성 (기본 배송지 설정)
            String shippingAddress = (user.getAddress() != null && !user.getAddress().isEmpty()) 
                ? user.getAddress() : "테스트 주문 (배송 없음)";
            
            // 주문 생성
            Order order = orderService.createOrder(user, shippingAddress, "현금결제");
            
            // 현금 결제 처리
            orderService.processPaymentWithCash(order.getId(), user);
            
            // 성공 메시지 설정
            setOrderCompleteAttributes(redirectAttributes, order, user);
            
            return "redirect:/orders/complete";
        }, redirectAttributes, "redirect:/cart");
    }
    
    @GetMapping("/complete")
    public String orderComplete(Model model) {
        // 결제 완료 페이지는 RedirectAttributes에서 전달된 정보를 사용합니다
        return "order/complete";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            Order order = getOrderAndValidateOwnership(id, user);
            
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "주문이 취소되었습니다");
            
            return "redirect:/orders/" + id;
        }, redirectAttributes, "redirect:/orders/" + id);
    }

    // 결제 처리 (모의 구현)
    @PostMapping("/{id}/pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Long id, Principal principal) {
        try {
            User user = getCurrentUser(principal);
            Order order = getOrderAndValidateOwnership(id, user);
            
            // 모의 결제 처리 - 실제로는 외부 결제 API 연동
            String transactionId = UUID.randomUUID().toString();
            orderService.processPayment(id, transactionId);
            
            return ResponseEntity.ok(createSuccessResponse("Payment processed successfully", Map.of(
                "transactionId", transactionId
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/orders")
    public String listUserOrders(
            Model model, 
            Principal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            
            // 삭제된 주문을 제외하고 조회
            Page<Order> orders = orderService.findByUserExcludeDeleted(user, pageable);
            model.addAttribute("orders", orders);
            
            return "order/list";
        }, "redirect:/login");
    }

    // 관리자용 주문 취소 (AJAX 응답)
    @PostMapping("/admin/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelAdminOrder(@PathVariable("id") Long id) {
        try {
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
            
            validateCancellableOrder(order);
            
            // 현금 결제였으면 환불 처리
            processCashRefundIfNeeded(order);
            
            // 주문 취소 처리
            orderService.cancelOrder(id);
            
            return ResponseEntity.ok(createSuccessResponse("주문이 취소되었습니다", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("주문 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        return handleOrderOperation(() -> {
            User user = getCurrentUser(principal);
            Order order = getOrderAndValidateOwnership(id, user);
            
            validateDeletableOrder(order);
            
            orderService.markOrderAsDeleted(id);
            redirectAttributes.addFlashAttribute("successMessage", "주문이 삭제되었습니다");
            
            return "redirect:/orders";
        }, redirectAttributes, "redirect:/orders");
    }
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 현재 로그인한 사용자 조회
     */
    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다");
        }
        
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }
    
    /**
     * 주문 소유자 확인
     */
    private void validateOrderOwnership(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근이 거부되었습니다");
        }
    }
    
    /**
     * 주문 조회 및 소유자 확인
     */
    private Order getOrderAndValidateOwnership(Long orderId, User user) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
        
        validateOrderOwnership(order, user);
        
        return order;
    }
    
    /**
     * 취소 가능한 주문 상태인지 확인
     */
    private void validateCancellableOrder(Order order) {
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PAID) {
            throw new IllegalArgumentException("이미 처리된 주문은 취소할 수 없습니다");
        }
    }
    
    /**
     * 현금 결제였을 경우 환불 처리
     */
    private void processCashRefundIfNeeded(Order order) {
        if ("현금결제".equals(order.getPaymentMethod()) && order.getUser() != null) {
            User user = order.getUser();
            user.setCashBalance(user.getCashBalance() + Math.round(order.getTotalAmount()));
        }
    }
    
    /**
     * 삭제 가능한 주문 상태인지 확인
     */
    private void validateDeletableOrder(Order order) {
        if (order.getStatus() == Order.OrderStatus.SHIPPING || order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("이 상태의 주문은 삭제할 수 없습니다: " + order.getStatus());
        }
    }
    
    /**
     * 주문 완료 페이지에 필요한 속성 설정
     */
    private void setOrderCompleteAttributes(RedirectAttributes redirectAttributes, Order order, User user) {
        redirectAttributes.addFlashAttribute("orderId", order.getId());
        redirectAttributes.addFlashAttribute("orderNumber", order.getOrderNumber());
        redirectAttributes.addFlashAttribute("totalAmount", order.getTotalAmount());
        redirectAttributes.addFlashAttribute("remainingBalance", user.getCashBalance());
        redirectAttributes.addFlashAttribute("orderDate", order.getCreatedAt());
    }
    
    /**
     * 주문 작업 처리 및 예외 처리
     */
    private String handleOrderOperation(Supplier<String> operation, RedirectAttributes redirectAttributes, String defaultRedirect) {
        try {
            return operation.get();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return defaultRedirect;
        }
    }
    
    /**
     * 주문 작업 처리 및 예외 처리 (리다이렉트 속성 없음)
     */
    private String handleOrderOperation(Supplier<String> operation, String defaultRedirect) {
        try {
            return operation.get();
        } catch (Exception e) {
            return defaultRedirect;
        }
    }
    
    /**
     * 성공 응답 생성
     */
    private Map<String, Object> createSuccessResponse(String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        
        if (additionalData != null) {
            response.putAll(additionalData);
        }
        
        return response;
    }
    
    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        return response;
    }
} 
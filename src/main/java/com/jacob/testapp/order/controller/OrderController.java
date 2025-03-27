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
        
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByUser(user, pageable);
        
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        
        return "order/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        Order order = orderService.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
        
        // 주문이 현재 사용자의 것인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근이 거부되었습니다");
        }
        
        model.addAttribute("order", order);
        return "order/detail";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // 사용자의 장바구니 조회
        Cart cart = cartService.getCart(user);
        
        if (cart.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }
        
        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("totalAmount", cart.getTotalPrice());
        
        return "order/checkout";
    }
    
    @PostMapping("/create-and-pay")
    public String createAndPayOrder(
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            // 주문 생성 (기본 배송지 설정)
            String shippingAddress = (user.getAddress() != null && !user.getAddress().isEmpty()) 
                ? user.getAddress() : "테스트 주문 (배송 없음)";
            
            // 주문 생성
            Order order = orderService.createOrder(user, shippingAddress, "현금결제");
            
            // 현금 결제 처리
            orderService.processPaymentWithCash(order.getId(), user);
            
            // 성공 메시지 설정
            redirectAttributes.addFlashAttribute("orderId", order.getId());
            redirectAttributes.addFlashAttribute("orderNumber", order.getOrderNumber());
            redirectAttributes.addFlashAttribute("totalAmount", order.getTotalAmount());
            redirectAttributes.addFlashAttribute("remainingBalance", user.getCashBalance());
            redirectAttributes.addFlashAttribute("orderDate", order.getCreatedAt());
            
            return "redirect:/orders/complete";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }
    
    @GetMapping("/complete")
    public String orderComplete(Model model) {
        // 결제 완료 페이지는 RedirectAttributes에서 전달된 정보를 사용합니다
        return "order/complete";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
            
            // 주문이 현재 사용자의 것인지 확인
            if (!order.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("접근이 거부되었습니다");
            }
            
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "주문이 취소되었습니다");
            return "redirect:/orders/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/orders/" + id;
        }
    }

    // 결제 처리 (모의 구현)
    @PostMapping("/{id}/pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            
            // 주문이 현재 사용자의 것인지 확인
            if (!order.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied");
            }
            
            // 모의 결제 처리 - 실제로는 외부 결제 API 연동
            String transactionId = UUID.randomUUID().toString();
            orderService.processPayment(id, transactionId);
            
            response.put("success", true);
            response.put("message", "Payment processed successfully");
            response.put("transactionId", transactionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 사용자 주문 목록 조회
    @GetMapping("/orders")
    public String listUserOrders(Model model, Principal principal,
                                @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        // 삭제된 주문을 제외하고 조회
        Page<Order> orders = orderService.findByUserExcludeDeleted(user, pageable);
        model.addAttribute("orders", orders);
        
        return "order/list";
    }

    // 관리자용 주문 취소 (AJAX 응답)
    @PostMapping("/admin/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelAdminOrder(@PathVariable("id") Long id) {
        try {
            Order order = orderService.findById(id).orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
            
            // 주문 상태 확인 (취소 가능한 상태인지)
            if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PAID) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미 처리된 주문은 취소할 수 없습니다"));
            }
            
            // 현금 결제였으면 환불 처리
            if ("현금결제".equals(order.getPaymentMethod()) && order.getUser() != null) {
                User user = order.getUser();
                user.setCashBalance(user.getCashBalance() + Math.round(order.getTotalAmount()));
            }
            
            // 주문 취소 처리
            orderService.cancelOrder(id);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "주문이 취소되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "주문 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user == null) {
                return "redirect:/login";
            }
            
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
            
            // 주문이 현재 사용자의 것인지 확인
            if (!order.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "접근이 거부되었습니다");
                return "redirect:/orders";
            }
            
            // 주문 상태를 DELETED로 변경 (이력은 유지되지만 사용자 화면에서는 숨김)
            orderService.markOrderAsDeleted(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "주문 내역이 삭제되었습니다");
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "주문 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/orders";
        }
    }
} 
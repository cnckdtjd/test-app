package com.jacob.testapp.order.controller;

import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.service.OrderService;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // 주문이 현재 사용자의 것인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        
        model.addAttribute("order", order);
        return "order/detail";
    }

    @PostMapping("/create")
    public String createOrder(
            @RequestParam String shippingAddress,
            @RequestParam String paymentMethod,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            Order order = orderService.createOrder(user, shippingAddress, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Order created successfully");
            return "redirect:/orders/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            
            // 주문이 현재 사용자의 것인지 확인
            if (!order.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Access denied");
            }
            
            orderService.cancelOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully");
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
} 
package com.jacob.testapp.cart.controller;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.service.CartService;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.function.Consumer;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @Autowired
    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    // 장바구니 페이지 보기
    @GetMapping
    public String viewCart(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = getUserFromPrincipal(principal);
        Cart cart = cartService.getCart(user);

        model.addAttribute("cart", cart);
        model.addAttribute("cartItemCount", cart.getCartItems().size());
        model.addAttribute("totalPrice", cart.getTotalPrice());
        model.addAttribute("user", user);

        return "cart/view";
    }

    // 장바구니에 상품 추가
    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.addProductToCart(user, productId, quantity),
            "상품이 장바구니에 추가되었습니다", "redirect:/products");
    }
    
    // 장바구니에서 상품 제거
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam Long productId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.removeProductFromCart(user, productId),
            "상품이 장바구니에서 제거되었습니다", "redirect:/cart");
    }
    
    // 장바구니 상품 수량 업데이트
    @PostMapping("/update")
    public String updateQuantity(
            @RequestParam Long productId,
            @RequestParam int quantity,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.updateProductQuantity(user, productId, quantity),
            "장바구니가 업데이트되었습니다", "redirect:/cart");
    }
    
    // 장바구니 비우기
    @PostMapping("/clear")
    public String clearCart(
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.clearCart(user),
            "장바구니가 비워졌습니다", "redirect:/cart");
    }
    
    // 중복 코드 제거를 위한 헬퍼 메서드
    private User getUserFromPrincipal(Principal principal) {
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }
    
    // 카트 작업 실행 및 예외 처리를 위한 공통 메서드
    private String executeCartOperation(
            Principal principal, 
            RedirectAttributes redirectAttributes,
            Consumer<User> operation,
            String successMessage,
            String redirectUrl) {
            
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            User user = getUserFromPrincipal(principal);
            operation.accept(user);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return redirectUrl;
    }
} 
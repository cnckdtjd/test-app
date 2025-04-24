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
        model.addAttribute("cartItemCount", cart.getTotalQuantity());
        model.addAttribute("cartItemTypes", cart.getCartItems().size());
        model.addAttribute("totalPrice", cart.getTotalPrice());
        model.addAttribute("user", user);
        model.addAttribute("cashBalance", user.getCashBalance());

        return "cart/view";
    }

    // 장바구니에 상품 추가
    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "/products") String returnUrl,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        // returnUrl이 절대 경로가 아니면 redirect: 접두사 추가
        String redirectTo = returnUrl.startsWith("/") ? "redirect:" + returnUrl : returnUrl;
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.addProductToCart(user, productId, quantity),
            "상품이 장바구니에 추가되었습니다", redirectTo, returnUrl);
    }
    
    // 장바구니에서 상품 제거
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam Long productId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.removeProductFromCart(user, productId),
            "상품이 장바구니에서 제거되었습니다", "redirect:/cart", "/cart");
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
            "장바구니가 업데이트되었습니다", "redirect:/cart", "/cart");
    }
    
    // 장바구니 비우기
    @PostMapping("/clear")
    public String clearCart(
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        return executeCartOperation(principal, redirectAttributes, user -> 
            cartService.clearCart(user),
            "장바구니가 비워졌습니다", "redirect:/cart", "/cart");
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
            String redirectUrl,
            String fallbackUrl) {
            
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            User user = getUserFromPrincipal(principal);
            // 작업 실행 전에 사용자 정보 설정
            redirectAttributes.addFlashAttribute("user", user);
            
            // 작업 실행
            operation.accept(user);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return redirectUrl;
        } catch (IllegalStateException e) {
            // 현금 부족 등의 상태 관련 오류
            String errorMessage = e.getMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            
            // 현금 부족 오류인 경우 사용자 잔액 정보도 함께 전달
            if (errorMessage.contains("현금 잔액이 부족합니다")) {
                try {
                    User user = getUserFromPrincipal(principal);
                    redirectAttributes.addFlashAttribute("userBalance", user.getCashBalance());
                    redirectAttributes.addFlashAttribute("balanceError", true);
                    
                    // 부족한 금액 계산 및 추가
                    if (errorMessage.contains("부족 금액:")) {
                        String shortfallStr = errorMessage.substring(errorMessage.indexOf("부족 금액:") + 6).trim();
                        shortfallStr = shortfallStr.replace("원", "").replace(",", "").trim();
                        try {
                            double shortfall = Double.parseDouble(shortfallStr);
                            redirectAttributes.addFlashAttribute("shortAmount", shortfall);
                        } catch (NumberFormatException nfe) {
                            // 금액 파싱 오류는 무시
                        }
                    }
                } catch (Exception ex) {
                    // 사용자 정보 조회 실패 시 무시
                }
            }
            
            return fallbackUrl.startsWith("redirect:") ? fallbackUrl : "redirect:" + fallbackUrl;
        } catch (IllegalArgumentException e) {
            // 재고 부족 등의 인자 관련 오류
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return fallbackUrl.startsWith("redirect:") ? fallbackUrl : "redirect:" + fallbackUrl;
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            redirectAttributes.addFlashAttribute("errorMessage", "처리 중 오류가 발생했습니다: " + e.getMessage());
            return fallbackUrl.startsWith("redirect:") ? fallbackUrl : "redirect:" + fallbackUrl;
        }
    }
    
    // 기존 메서드와의 호환성을 위한 오버로딩 메서드
    private String executeCartOperation(
            Principal principal, 
            RedirectAttributes redirectAttributes,
            Consumer<User> operation,
            String successMessage,
            String redirectUrl) {
        return executeCartOperation(principal, redirectAttributes, operation, successMessage, redirectUrl, redirectUrl);
    }
} 
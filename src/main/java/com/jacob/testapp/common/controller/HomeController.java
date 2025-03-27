package com.jacob.testapp.common.controller;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.cart.service.CartService;
import com.jacob.testapp.product.service.ProductService;
import com.jacob.testapp.user.service.UserService;
import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final UserService userService;
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;

    @Autowired
    public HomeController(UserService userService, ProductService productService, CartService cartService, OrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        // 인증된 사용자인 경우
        if (authentication != null && authentication.isAuthenticated()) {
            // 관리자인 경우 관리자 페이지로 리다이렉트
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }
            // 일반 사용자인 경우 홈페이지로 리다이렉트
            return "redirect:/home";
        }
        // 인증되지 않은 사용자는 로그인 페이지로 리다이렉트
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String homePage(Model model, Principal principal, Authentication authentication) {
        // 인증되지 않은 사용자는 로그인 페이지로 리다이렉트
        if (principal == null) {
            return "redirect:/login";
        }
        
        // 관리자인 경우 관리자 페이지로 리다이렉트
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        }
        
        // 최신 상품 목록 가져오기
        List<Product> latestProducts = productService.findLatestProducts();
        model.addAttribute("latestProducts", latestProducts);
        
        // 로그인한 사용자 정보 가져오기
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        model.addAttribute("user", user);
        
        // 장바구니 정보 가져오기
        try {
            // 장바구니가 있는지 확인 (아이템이 없어도 장바구니는 존재할 수 있음)
            Cart cart = cartService.getOrCreateCart(user);

            // 장바구니 아이템 조회 시도
            try {
                Optional<Cart> cartWithItems = cartService.findByUserWithItems(user.getId());
                if (cartWithItems.isPresent() && !cartWithItems.get().getCartItems().isEmpty()) {
                    model.addAttribute("cartItemCount", cartWithItems.get().getCartItems().size());
                    model.addAttribute("cartTotalPrice", cartWithItems.get().getTotalPrice());
                } else {
                    // 장바구니는 있지만 아이템이 없는 경우
                    model.addAttribute("cartItemCount", 0);
                    model.addAttribute("cartTotalPrice", 0);
                }
            } catch (Exception e) {
                // 장바구니 아이템 조회 실패 시 기본값 설정
                model.addAttribute("cartItemCount", 0);
                model.addAttribute("cartTotalPrice", 0);
            }
        } catch (Exception e) {
            // 장바구니 자체를 찾을 수 없는 경우 기본값 설정
            model.addAttribute("cartItemCount", 0);
            model.addAttribute("cartTotalPrice", 0);
        }
        
        // 최근 주문 내역 조회 (최대 3개)
        List<Order> recentOrders = orderService.getRecentOrdersByUserExcludeDeleted(user, 3);
        model.addAttribute("recentOrders", recentOrders);
        
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "user/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        // 회원가입 폼에 필요한 빈 User 객체 생성
        model.addAttribute("user", new User());
        // 리다이렉트 대신 직접 뷰 이름 반환
        return "user/register";
    }
} 
package com.jacob.testapp.product.controller;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.service.CartService;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.service.ProductService;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;

    @Autowired
    public ProductController(ProductService productService, UserService userService, CartService cartService) {
        this.productService = productService;
        this.userService = userService;
        this.cartService = cartService;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Product.Category category,
            Model model,
            Principal principal) {
        
        // 페이징 객체 생성 (상품명으로 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        // 상품 검색 (이름 또는 카테고리 기준)
        Page<Product> products;
        if (name != null && !name.isEmpty() && category != null) {
            products = productService.findByNameContainingAndCategory(name, category, pageable);
        } else if (name != null && !name.isEmpty()) {
            products = productService.findByNameContaining(name, pageable);
        } else if (category != null) {
            products = productService.findByCategory(category, pageable);
        } else {
            products = productService.findAll(pageable);
        }
        
        // 카테고리 목록 가져오기
        List<Product.Category> categories = List.of(Product.Category.values());
        
        // 장바구니 정보 가져오기 (로그인 상태일 경우)
        if (principal != null) {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            Optional<Cart> cart = cartService.findByUser(user);
            if (cart.isPresent()) {
                model.addAttribute("cartItemCount", cart.get().getCartItems().size());
            } else {
                model.addAttribute("cartItemCount", 0);
            }
        }
        
        // 모델에 데이터 추가
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("searchName", name);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categories);
        
        return "product/list";
    }
    
    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model, Principal principal) {
        // 상품 정보 가져오기
        Optional<Product> product = Optional.ofNullable(productService.findById(id));
        if (product.isEmpty()) {
            return "redirect:/products";
        }
        
        // 장바구니 정보 가져오기 (로그인 상태일 경우)
        if (principal != null) {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            Optional<Cart> cart = cartService.findByUser(user);
            if (cart.isPresent()) {
                model.addAttribute("cartItemCount", cart.get().getCartItems().size());
            } else {
                model.addAttribute("cartItemCount", 0);
            }
        }
        
        // 관련 상품 가져오기 (같은 카테고리)
        List<Product> relatedProducts = productService.findByCategory(product.get().getCategory(), PageRequest.of(0, 4))
                .stream()
                .filter(p -> !p.getId().equals(id))
                .collect(Collectors.toList());
        
        model.addAttribute("product", product.get());
        model.addAttribute("relatedProducts", relatedProducts);
        
        return "product/detail";
    }

    // 부하 테스트용 - 의도적으로 지연 발생
    @GetMapping("/delay/{id}")
    @ResponseBody
    public Product productDetailWithDelay(@PathVariable Long id, @RequestParam(defaultValue = "1000") long delayMillis) {
        return productService.findByIdWithDelay(id, delayMillis)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + id));
    }
} 
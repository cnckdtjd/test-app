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
        Page<Product> products = findProducts(name, category, pageable);
        
        // 카테고리 목록 가져오기
        List<Product.Category> categories = List.of(Product.Category.values());
        
        // 장바구니 정보 추가
        addCartInfoToModel(model, principal);
        
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
        Product product = productService.findById(id);
        if (product == null) {
            return "redirect:/products";
        }
        
        // 장바구니 정보 추가
        addCartInfoToModel(model, principal);
        
        // 관련 상품 가져오기 (같은 카테고리, 현재 상품 제외)
        List<Product> relatedProducts = findRelatedProducts(product, id);
        
        model.addAttribute("product", product);
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
    
    /**
     * 주어진 조건에 맞는 상품 목록을 검색합니다.
     */
    private Page<Product> findProducts(String name, Product.Category category, Pageable pageable) {
        if (name != null && !name.isEmpty() && category != null) {
            return productService.findByNameContainingAndCategory(name, category, pageable);
        } else if (name != null && !name.isEmpty()) {
            return productService.findByNameContaining(name, pageable);
        } else if (category != null) {
            return productService.findByCategory(category, pageable);
        } else {
            return productService.findAll(pageable);
        }
    }
    
    /**
     * 관련 상품 목록을 조회합니다.
     */
    private List<Product> findRelatedProducts(Product product, Long currentProductId) {
        return productService.findByCategory(product.getCategory(), PageRequest.of(0, 4))
                .stream()
                .filter(p -> !p.getId().equals(currentProductId))
                .collect(Collectors.toList());
    }
    
    /**
     * 장바구니 정보를 모델에 추가합니다.
     */
    private void addCartInfoToModel(Model model, Principal principal) {
        if (principal == null) {
            model.addAttribute("cartItemCount", 0);
            return;
        }
        
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            
            cartService.findByUserWithItems(user.getId())
                    .ifPresentOrElse(
                            cart -> {
                                model.addAttribute("cartItemCount", cart.getTotalQuantity());
                                
                                // 현금 잔액 검증
                                if (user.getCashBalance() != null && 
                                    cart.getTotalPrice().compareTo(java.math.BigDecimal.valueOf(user.getCashBalance())) > 0) {
                                    model.addAttribute("balanceWarning", true);
                                }
                            },
                            () -> model.addAttribute("cartItemCount", 0)
                    );
        } catch (Exception e) {
            model.addAttribute("cartItemCount", 0);
        }
    }
} 
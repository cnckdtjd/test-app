package com.jacob.cartservice.controller;

import com.jacob.cartservice.model.Cart;
import com.jacob.cartservice.model.CartItem;
import com.jacob.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        Cart cart = cartService.getCartByUserId(userId);
        List<CartItem> items = cartService.getCartItems(cart.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("cart", cart);
        response.put("items", items);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addItemToCart(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        
        cartService.addItemToCart(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> updateCartItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        
        cartService.updateCartItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        
        cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }
} 
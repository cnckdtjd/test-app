package com.jacob.testapp.cart.service;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.entity.CartItem;
import com.jacob.testapp.cart.repository.CartItemRepository;
import com.jacob.testapp.cart.repository.CartRepository;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, 
                      ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
    
    // 사용자의 장바구니 찾기
    public Optional<Cart> findByUser(User user) {
        return cartRepository.findByUser(user);
    }
    
    // 사용자 ID로 장바구니와 아이템들을 함께 조회
    public Optional<Cart> findByUserWithItems(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return findByUser(user);
        }
        return Optional.empty();
    }
    
    // 장바구니 생성 또는 가져오기
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .user(user)
                            .totalPrice(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(cart);
                });
    }
    
    // 장바구니에 상품 추가
    @Transactional
    public void addProductToCart(User user, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        Cart cart = getOrCreateCart(user);
        
        // 이미 장바구니에 있는 상품인지 확인
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (existingItem.isPresent()) {
            // 기존 상품 수량 업데이트
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
            cartItemRepository.save(item);
        } else {
            // 새 상품 추가
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
            cart.addItem(newItem);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        }
    }

    @Transactional
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        if (quantity <= 0) {
            removeItemFromCart(cartId, productId);
            return;
        }

        // 상품 재고 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }

        cartItemRepository.updateQuantity(cartId, productId, quantity);

        // 장바구니 총액 재계산
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }

    @Transactional
    public void removeItemFromCart(Long cartId, Long productId) {
        cartItemRepository.deleteByCartIdAndProductId(cartId, productId);

        // 장바구니 총액 재계산
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long cartId) {
        cartItemRepository.deleteAllByCartId(cartId);

        // 장바구니 총액 초기화
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        cart.clear();
        cartRepository.save(cart);
    }

    // 장바구니 조회
    public Cart getCart(User user) {
        return getOrCreateCart(user);
    }

    // 장바구니에서 상품 제거
    @Transactional
    public void removeProductFromCart(User user, Long productId) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        Optional<CartItem> item = cartItemRepository.findByCartAndProduct(cart, product);
        if (item.isPresent()) {
            cartItemRepository.delete(item.get());
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        }
    }
    
    // 장바구니 상품 수량 업데이트
    @Transactional
    public void updateProductQuantity(User user, Long productId, int quantity) {
        if (quantity <= 0) {
            // 수량이 0 이하면 상품 제거
            removeProductFromCart(user, productId);
            return;
        }
        
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
            cart.addItem(newItem);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        }
    }
    
    // 장바구니 비우기
    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
        cart.clear();
        cartRepository.save(cart);
    }
} 
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
import java.util.function.Consumer;

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
    
    /**
     * 사용자의 장바구니를 조회합니다.
     */
    public Optional<Cart> findByUser(User user) {
        return cartRepository.findByUserWithItems(user);
    }
    
    /**
     * 사용자 ID로 장바구니와 아이템들을 함께 조회
     */
    public Optional<Cart> findByUserWithItems(Long userId) {
        return cartRepository.findByUserIdWithItems(userId);
    }
    
    /**
     * 장바구니 가져오기 (없으면 생성)
     */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .user(user)
                            .totalPrice(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(cart);
                });
    }
    
    /**
     * 장바구니 조회
     */
    public Cart getCart(User user) {
        return getOrCreateCart(user);
    }
    
    /**
     * 장바구니에 상품 추가
     */
    @Transactional
    public void addProductToCart(User user, Long productId, int quantity) {
        validateQuantity(quantity);
        
        Product product = findProductAndValidateStock(productId, quantity);
        Cart cart = getOrCreateCart(user);
        
        updateCartItem(cart, product, existingItem -> 
            existingItem.setQuantity(existingItem.getQuantity() + quantity));
    }
    
    /**
     * 장바구니 상품 수량 업데이트
     */
    @Transactional
    public void updateProductQuantity(User user, Long productId, int quantity) {
        if (quantity <= 0) {
            removeProductFromCart(user, productId);
            return;
        }
        
        Product product = findProductAndValidateStock(productId, quantity);
        Cart cart = getOrCreateCart(user);
        
        updateCartItem(cart, product, existingItem -> existingItem.setQuantity(quantity));
    }
    
    /**
     * 장바구니에서 상품 제거
     */
    @Transactional
    public void removeProductFromCart(User user, Long productId) {
        Cart cart = getOrCreateCart(user);
        Product product = findProduct(productId);
        
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            cartItemRepository.delete(item);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        });
    }
    
    /**
     * 장바구니 비우기
     */
    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCart(cart);
        cart.clear();
        cartRepository.save(cart);
    }
    
    /**
     * 장바구니 ID로 상품 수량 업데이트
     */
    @Transactional
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        if (quantity <= 0) {
            removeItemFromCart(cartId, productId);
            return;
        }

        Product product = findProductAndValidateStock(productId, quantity);
        Cart cart = findCart(cartId);
        
        cartItemRepository.updateQuantity(cartId, productId, quantity);
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }

    /**
     * 장바구니 ID로 상품 제거
     */
    @Transactional
    public void removeItemFromCart(Long cartId, Long productId) {
        Cart cart = findCart(cartId);
        
        cartItemRepository.deleteByCartIdAndProductId(cartId, productId);
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }

    /**
     * 장바구니 ID로 장바구니 비우기
     */
    @Transactional
    public void clearCart(Long cartId) {
        Cart cart = findCart(cartId);
        
        cartItemRepository.deleteAllByCartId(cartId);
        cart.clear();
        cartRepository.save(cart);
    }
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 수량 유효성 검사
     */
    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
    }
    
    /**
     * 상품 ID로 상품 찾기
     */
    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
    }
    
    /**
     * 상품 ID로 상품 찾고 재고 확인
     */
    private Product findProductAndValidateStock(Long productId, int quantity) {
        Product product = findProduct(productId);
        
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        return product;
    }
    
    /**
     * 장바구니 ID로 장바구니 찾기
     */
    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다: " + cartId));
    }
    
    /**
     * 장바구니 아이템 업데이트 공통 로직
     */
    private void updateCartItem(Cart cart, Product product, Consumer<CartItem> itemUpdater) {
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (existingItemOpt.isPresent()) {
            // 기존 상품 업데이트
            CartItem item = existingItemOpt.get();
            itemUpdater.accept(item);
            cartItemRepository.save(item);
        } else {
            // 새 상품 추가
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(1) // 기본값, itemUpdater에서 변경됨
                    .build();
            itemUpdater.accept(newItem);
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }
        
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }
} 
package com.jacob.testapp.cart.service;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.entity.CartItem;
import com.jacob.testapp.cart.repository.CartItemRepository;
import com.jacob.testapp.cart.repository.CartRepository;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
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
     * 낙관적 락 발생 시 재시도 로직 포함
     */
    @Transactional
    public void addProductToCart(User user, Long productId, int quantity) {
        int maxRetryCount = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount < maxRetryCount) {
            try {
                doAddProductToCart(user, productId, quantity);
                success = true;
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetryCount) {
                    logger.error("장바구니 상품 추가 중 최대 재시도 횟수 초과: productId={}, userId={}", productId, user.getId());
                    throw new RuntimeException("장바구니 상품 추가 중 오류가 발생했습니다. 다시 시도해주세요.");
                }
                logger.warn("낙관적 락 발생, 재시도 {}/{}회: productId={}, userId={}", 
                        retryCount, maxRetryCount, productId, user.getId());
                // 짧은 대기 후 재시도
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("장바구니 작업이 중단되었습니다.");
                }
            }
        }
    }
    
    /**
     * 실제 장바구니 상품 추가 로직
     * 낙관적 락 예외 발생 가능
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void doAddProductToCart(User user, Long productId, int quantity) {
        validateQuantity(quantity);
        
        Product product = findProductAndValidateStock(productId, quantity);
        
        // 사용자 현금 잔액 검증
        validateUserBalance(user, product, quantity);
        
        Cart cart = getOrCreateCart(user);
        
        updateCartItem(cart, product, existingItem -> {
            // 기존 상품이면 수량만 증가, 새 상품이면 지정된 수량으로 설정
            if (existingItem.getId() != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
            } else {
                existingItem.setQuantity(quantity);
            }
        });
    }
    
    /**
     * 장바구니 상품 수량 업데이트
     */
    @Transactional
    public void updateProductQuantity(User user, Long productId, int quantity) {
        try {
            if (quantity <= 0) {
                removeProductFromCart(user, productId);
                return;
            }
            
            Product product = findProductAndValidateStock(productId, quantity);
            
            // 기존 수량 확인 후 증가하는 경우에만 현금 잔액 검증
            Optional<CartItem> existingItemOpt = getExistingCartItem(user, productId);
            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                if (quantity > existingItem.getQuantity()) {
                    // 수량이 증가하는 경우만 추가 비용에 대해 잔액 검증
                    int additionalQuantity = quantity - existingItem.getQuantity();
                    validateUserBalance(user, product, additionalQuantity);
                }
            } else {
                // 새로 추가하는 경우 전체 수량에 대해 잔액 검증
                validateUserBalance(user, product, quantity);
            }
            
            Cart cart = getOrCreateCart(user);
            
            updateCartItem(cart, product, existingItem -> existingItem.setQuantity(quantity));
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 예외 발생 시 재시도
            updateProductQuantity(user, productId, quantity);
        }
    }
    
    /**
     * 장바구니에서 상품 제거
     */
    @Transactional
    public void removeProductFromCart(User user, Long productId) {
        try {
            Cart cart = cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다"));
            
            // 영속 상태의 장바구니 아이템 직접 삭제
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
            
            // 삭제 후 장바구니 다시 로드하여 상태 갱신
            cart = cartRepository.findById(cart.getId()).orElse(cart);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 예외 발생 시 재시도
            removeProductFromCart(user, productId);
        }
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
        try {
            if (quantity <= 0) {
                removeItemFromCart(cartId, productId);
                return;
            }

            Cart cart = findCart(cartId);
            Product product = findProductAndValidateStock(productId, quantity);
            
            // 사용자의 현금 잔액 검증 (cart에서 user를 가져옴)
            User user = cart.getUser();
            
            // 기존 수량 확인 후 증가하는 경우에만 현금 잔액 검증
            Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cartId, productId);
            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                if (quantity > existingItem.getQuantity()) {
                    // 수량이 증가하는 경우만 추가 비용에 대해 잔액 검증
                    int additionalQuantity = quantity - existingItem.getQuantity();
                    validateUserBalance(user, product, additionalQuantity);
                }
            } else {
                // 새로 추가하는 경우 전체 수량에 대해 잔액 검증
                validateUserBalance(user, product, quantity);
            }
            
            cartItemRepository.updateQuantity(cartId, productId, quantity);
            cart.recalculateTotalPrice();
            cartRepository.save(cart);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 낙관적 락 예외 발생 시 재시도
            updateItemQuantity(cartId, productId, quantity);
        }
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
     * 사용자의 장바구니 총액을 계산합니다.
     */
    private BigDecimal calculateCartTotalForUser(User user) {
        Optional<Cart> cartOpt = findByUserWithItems(user.getId());
        if (cartOpt.isEmpty() || cartOpt.get().getCartItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return cartOpt.get().getTotalPrice();
    }

    /**
     * 사용자의 현금 잔액이 충분한지 검증합니다.
     * 사용자의 현금 잔액이 총 카트 금액보다 적으면 예외를 발생시킵니다.
     * @param user 검증할 사용자 정보
     * @param product 추가하려는 상품
     * @param quantity 추가하려는 수량
     * @throws IllegalStateException 사용자의 현금 잔액이 부족한 경우
     */
    private void validateUserBalance(User user, Product product, int quantity) {
        // 기존 장바구니 합계 계산
        BigDecimal cartTotal = calculateCartTotalForUser(user);
        
        // 새로 추가하려는 상품 금액 계산
        BigDecimal productTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        // 총 필요 금액 계산 (기존 장바구니 + 새 상품)
        BigDecimal totalNeeded = cartTotal.add(productTotal);
        
        // 사용자 잔액 계산
        BigDecimal userBalance = BigDecimal.valueOf(user.getCashBalance());
        
        logger.info("사용자 현금 검증: 사용자={}, 현재잔액={}, 기존장바구니={}, 추가상품={}, 총필요금액={}",
                user.getUsername(), userBalance, cartTotal, productTotal, totalNeeded);
        
        // 잔액이 부족한 경우 예외 발생
        if (userBalance.compareTo(totalNeeded) < 0) {
            BigDecimal shortfall = totalNeeded.subtract(userBalance);
            String message = String.format("현금 잔액이 부족합니다. 부족 금액: %s원", 
                    shortfall.setScale(0, RoundingMode.CEILING));
            logger.warn(message);
            throw new IllegalStateException(message);
        }
    }
    
    /**
     * 특정 상품을 제외한 장바구니 총액 계산
     */
    private BigDecimal getCartTotalExcludingProduct(User user, Long productId) {
        Optional<Cart> cartOpt = findByUserWithItems(user.getId());
        if (cartOpt.isEmpty() || cartOpt.get().getCartItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(
            cartOpt.get().getCartItems().stream()
                .filter(item -> !item.getProduct().getId().equals(productId))
                .mapToDouble(CartItem::getTotalPrice)
                .sum()
        );
    }
    
    /**
     * 현재 장바구니에 있는 특정 상품 항목 조회
     */
    private Optional<CartItem> getExistingCartItem(User user, Long productId) {
        Optional<Cart> cartOpt = findByUserWithItems(user.getId());
        if (cartOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Cart cart = cartOpt.get();
        Product product = findProduct(productId);
        
        return cartItemRepository.findByCartAndProduct(cart, product);
    }
    
    /**
     * 장바구니 ID로 장바구니 찾기
     */
    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다: " + cartId));
    }
    
    /**
     * 장바구니 항목 업데이트
     */
    private void updateCartItem(Cart cart, Product product, Consumer<CartItem> itemUpdater) {
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (existingItemOpt.isPresent()) {
            // 기존 상품 업데이트
            CartItem item = existingItemOpt.get();
            itemUpdater.accept(item);
            cartItemRepository.save(item);
        } else {
            // 새 상품 추가 (빌더에서 초기 수량은 지정하지 않고 itemUpdater에서 설정)
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .build();
            itemUpdater.accept(newItem);
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }
        
        cart.recalculateTotalPrice();
        cartRepository.save(cart);
    }
} 
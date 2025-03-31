package com.jacob.cartservice.service;

import com.jacob.cartservice.client.ProductServiceClient;
import com.jacob.cartservice.mapper.CartItemMapper;
import com.jacob.cartservice.mapper.CartMapper;
import com.jacob.cartservice.model.Cart;
import com.jacob.cartservice.model.CartItem;
import com.jacob.cartservice.model.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductServiceClient productServiceClient;
    
    @Cacheable(value = "cartCache", key = "#userId")
    public Cart getCartByUserId(Long userId) {
        Cart cart = cartMapper.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cartMapper.insert(cart);
        }
        return cart;
    }
    
    @Cacheable(value = "cartItemsCache", key = "#cartId")
    public List<CartItem> getCartItems(Long cartId) {
        return cartItemMapper.findAllByCartId(cartId);
    }
    
    @Transactional
    @CacheEvict(value = {"cartCache", "cartItemsCache"}, allEntries = true)
    public void addItemToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        
        ProductDto product = productServiceClient.getProduct(productId);
        if (product == null) {
            throw new NoSuchElementException("상품을 찾을 수 없습니다.");
        }
        
        CartItem existingItem = cartItemMapper.findByCartIdAndProductId(cart.getId(), productId);
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemMapper.update(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCartId(cart.getId());
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setPrice(product.getPrice());
            newItem.setProductName(product.getName());
            cartItemMapper.insert(newItem);
        }
        
        cartMapper.update(cart);
    }
    
    @Transactional
    @CacheEvict(value = {"cartCache", "cartItemsCache"}, allEntries = true)
    public void updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getCartByUserId(userId);
        CartItem item = cartItemMapper.findByCartIdAndProductId(cart.getId(), productId);
        
        if (item == null) {
            throw new NoSuchElementException("장바구니에 해당 상품이 없습니다.");
        }
        
        if (quantity <= 0) {
            cartItemMapper.delete(item.getId());
        } else {
            item.setQuantity(quantity);
            cartItemMapper.update(item);
        }
        
        cartMapper.update(cart);
    }
    
    @Transactional
    @CacheEvict(value = {"cartCache", "cartItemsCache"}, allEntries = true)
    public void removeItemFromCart(Long userId, Long productId) {
        Cart cart = getCartByUserId(userId);
        CartItem item = cartItemMapper.findByCartIdAndProductId(cart.getId(), productId);
        
        if (item == null) {
            throw new NoSuchElementException("장바구니에 해당 상품이 없습니다.");
        }
        
        cartItemMapper.delete(item.getId());
        cartMapper.update(cart);
    }
    
    @Transactional
    @CacheEvict(value = {"cartCache", "cartItemsCache"}, allEntries = true)
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cartItemMapper.deleteAllByCartId(cart.getId());
        cartMapper.update(cart);
    }
} 
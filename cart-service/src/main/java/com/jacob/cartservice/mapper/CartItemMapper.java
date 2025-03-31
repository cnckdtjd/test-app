package com.jacob.cartservice.mapper;

import com.jacob.cartservice.model.CartItem;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CartItemMapper {
    List<CartItem> findAllByCartId(Long cartId);
    CartItem findByCartIdAndProductId(Long cartId, Long productId);
    void insert(CartItem cartItem);
    void update(CartItem cartItem);
    void delete(Long id);
    void deleteAllByCartId(Long cartId);
} 
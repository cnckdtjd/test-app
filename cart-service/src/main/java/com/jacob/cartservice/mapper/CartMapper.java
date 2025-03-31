package com.jacob.cartservice.mapper;

import com.jacob.cartservice.model.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper {
    Cart findByUserId(Long userId);
    void insert(Cart cart);
    void update(Cart cart);
    void delete(Long id);
} 
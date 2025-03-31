package com.jacob.cartservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Getter @Setter @ToString
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
} 
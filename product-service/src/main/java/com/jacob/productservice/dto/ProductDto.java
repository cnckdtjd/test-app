package com.jacob.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    
    @NotBlank(message = "상품명은 필수입니다")
    private String name;
    
    private String description;
    
    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0보다 커야 합니다")
    private BigDecimal price;
    
    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0보다 커야 합니다")
    private Integer stock;
    
    private String category;
    
    // 이미지 URL 필드 추가
    private String imageUrl;
    
    // 낙관적 락을 위한 버전
    private Long version;
} 
package com.jacob.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private Category category;
    private Status status;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public enum Category {
        프로그래밍, 데이터베이스, 네트워크, 운영체제, 인공지능, 모바일, UI_UX, DevOps, IoT, IT비즈니스, 
        네트워크_보안, 인공지능_데이터과학, 시스템_운영, 모바일_개발, IoT_혁신기술
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
} 
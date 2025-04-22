package com.jacob.testapp.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 200)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(length = 255)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 카테고리
     */
    public enum Category {
        프로그래밍("Programming"), 
        데이터베이스("Database"), 
        네트워크("Network"), 
        운영체제("Operating System"), 
        인공지능("Artificial Intelligence"), 
        모바일("Mobile"), 
        UI_UX("UI/UX"), 
        DevOps("DevOps"), 
        IoT("IoT"), 
        IT비즈니스("IT Business"), 
        네트워크_보안("Network Security"), 
        인공지능_데이터과학("AI and Data Science"), 
        시스템_운영("System Operations"), 
        모바일_개발("Mobile Development"), 
        IoT_혁신기술("IoT Innovation");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 상품 상태
     */
    public enum Status {
        ACTIVE("판매중"), 
        INACTIVE("판매중지");
        
        private final String displayName;
        
        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isActive() {
            return this == ACTIVE;
        }
    }
    
    /**
     * 재고 확인
     * @param quantity 확인할 수량
     * @return 주문 가능 여부
     */
    public boolean hasStock(int quantity) {
        return this.stock >= quantity;
    }
    
    /**
     * 재고 감소
     * @param quantity 감소시킬 수량
     * @return 재고 감소 성공 여부
     */
    public boolean decreaseStock(int quantity) {
        if (!hasStock(quantity)) {
            return false;
        }
        this.stock -= quantity;
        return true;
    }
    
    /**
     * 재고 증가
     * @param quantity 증가시킬 수량
     */
    public void increaseStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다");
        }
        this.stock += quantity;
    }
    
    /**
     * 상품이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status.isActive();
    }
} 
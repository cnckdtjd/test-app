package com.jacob.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class ProductServiceClient {
    
    @Value("${external-services.product-service.url}")
    private String productServiceUrl;
    
    private final WebClient.Builder webClientBuilder;
    
    public ProductServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public long getTotalProducts() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(productServiceUrl + "/admin/stats/total-products")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("총 상품 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    public long getLowStockProducts() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(productServiceUrl + "/admin/stats/low-stock")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("재고 부족 상품 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
} 
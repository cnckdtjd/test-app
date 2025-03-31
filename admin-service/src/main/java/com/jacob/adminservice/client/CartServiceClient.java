package com.jacob.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class CartServiceClient {
    
    @Value("${external-services.cart-service.url}")
    private String cartServiceUrl;
    
    private final WebClient.Builder webClientBuilder;
    
    public CartServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public long getTotalActiveCarts() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(cartServiceUrl + "/admin/stats/total-active-carts")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("활성 장바구니 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    public double getAverageCartValue() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(cartServiceUrl + "/admin/stats/average-cart-value")
                .retrieve()
                .bodyToMono(Double.class)
                .block();
        } catch (Exception e) {
            log.error("평균 장바구니 가치 조회 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
    }
} 
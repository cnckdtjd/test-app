package com.jacob.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class OrderServiceClient {
    
    @Value("${external-services.order-service.url}")
    private String orderServiceUrl;
    
    private final WebClient.Builder webClientBuilder;
    
    public OrderServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public long getTotalOrders() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(orderServiceUrl + "/admin/stats/total-orders")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("총 주문 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    public long getTotalRevenue() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(orderServiceUrl + "/admin/stats/total-revenue")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("총 매출 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    public long getNewOrdersToday() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(orderServiceUrl + "/admin/stats/new-orders-today")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("오늘의 신규 주문 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
} 
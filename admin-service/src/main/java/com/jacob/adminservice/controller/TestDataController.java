package com.jacob.adminservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RestController
@RequestMapping("/api/admin/test-data")
public class TestDataController {
    
    private final WebClient.Builder webClientBuilder;
    
    public TestDataController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateTestData() {
        // 각 서비스로 테스트 데이터 생성 요청을 보냄
        try {
            // 사용자 서비스에 테스트 데이터 생성 요청
            webClientBuilder.build()
                .post()
                .uri("${external-services.user-service.url}/admin/test-data/generate")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 상품 서비스에 테스트 데이터 생성 요청
            webClientBuilder.build()
                .post()
                .uri("${external-services.product-service.url}/admin/test-data/generate")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 주문 서비스에 테스트 데이터 생성 요청
            webClientBuilder.build()
                .post()
                .uri("${external-services.order-service.url}/admin/test-data/generate")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 장바구니 서비스에 테스트 데이터 생성 요청
            webClientBuilder.build()
                .post()
                .uri("${external-services.cart-service.url}/admin/test-data/generate")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("테스트 데이터 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("테스트 데이터 생성 실패");
        }
    }
    
    @PostMapping("/reset")
    public ResponseEntity<?> resetTestData() {
        // 각 서비스의 테스트 데이터 초기화 요청
        try {
            // 사용자 서비스 데이터 초기화
            webClientBuilder.build()
                .post()
                .uri("${external-services.user-service.url}/admin/test-data/reset")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 상품 서비스 데이터 초기화
            webClientBuilder.build()
                .post()
                .uri("${external-services.product-service.url}/admin/test-data/reset")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 주문 서비스 데이터 초기화
            webClientBuilder.build()
                .post()
                .uri("${external-services.order-service.url}/admin/test-data/reset")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            // 장바구니 서비스 데이터 초기화
            webClientBuilder.build()
                .post()
                .uri("${external-services.cart-service.url}/admin/test-data/reset")
                .retrieve()
                .toBodilessEntity()
                .block();
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("테스트 데이터 초기화 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("테스트 데이터 초기화 실패");
        }
    }
} 
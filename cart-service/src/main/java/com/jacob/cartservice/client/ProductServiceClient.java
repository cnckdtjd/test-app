package com.jacob.cartservice.client;

import com.jacob.cartservice.model.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceClient {
    
    @Value("${external-services.product-service.url}")
    private String productServiceUrl;
    
    private final WebClient.Builder webClientBuilder;
    
    public ProductDto getProduct(Long productId) {
        try {
            return webClientBuilder.build()
                .get()
                .uri(productServiceUrl + "/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("상품 조회 중 오류 발생 (ID: {}): {}", productId, e.getMessage());
            return null;
        }
    }
} 
package com.jacob.orderservice.client;

import com.jacob.orderservice.model.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(@Value("${external-services.product-service.url}") String productServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    /**
     * 상품 정보 조회
     */
    public Mono<ProductDto> getProductById(Long productId) {
        return webClient.get()
                .uri("/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .doOnError(e -> log.error("Product service error: {}", e.getMessage()));
    }

    /**
     * 상품 재고 감소
     */
    public Mono<Boolean> decreaseStock(Long productId, Integer quantity) {
        return webClient.post()
                .uri("/products/{id}/decrease-stock?quantity={quantity}", productId, quantity)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("success"))
                .doOnError(e -> {
                    log.error("Product service error: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * 상품 재고 증가
     */
    public Mono<Boolean> increaseStock(Long productId, Integer quantity) {
        return webClient.post()
                .uri("/products/{id}/increase-stock?quantity={quantity}", productId, quantity)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("success"))
                .doOnError(e -> {
                    log.error("Product service error: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
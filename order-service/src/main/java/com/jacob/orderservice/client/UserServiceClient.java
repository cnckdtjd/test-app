package com.jacob.orderservice.client;

import com.jacob.orderservice.model.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(@Value("${external-services.user-service.url}") String userServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    /**
     * 사용자 정보 조회
     */
    public Mono<UserDto> getUserById(Long userId, String token) {
        return webClient.get()
                .uri("/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(UserDto.class)
                .doOnError(e -> log.error("User service error: {}", e.getMessage()));
    }

    /**
     * 사용자 캐시 잔액 차감
     */
    public Mono<Boolean> deductCashBalance(Long userId, Double amount, String token) {
        return webClient.post()
                .uri("/users/{id}/deduct-balance", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("success"))
                .doOnError(e -> log.error("User service error: {}", e.getMessage()));
    }

    /**
     * 사용자 캐시 잔액 환불
     */
    public Mono<Boolean> refundCashBalance(Long userId, Double amount, String token) {
        return webClient.post()
                .uri("/users/{id}/refund-balance", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("success"))
                .doOnError(e -> log.error("User service error: {}", e.getMessage()));
    }
} 
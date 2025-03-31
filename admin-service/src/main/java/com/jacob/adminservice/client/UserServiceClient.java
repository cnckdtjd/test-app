package com.jacob.adminservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class UserServiceClient {
    
    @Value("${external-services.user-service.url}")
    private String userServiceUrl;
    
    private final WebClient.Builder webClientBuilder;
    
    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public long getTotalUsers() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/admin/stats/total-users")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("총 사용자 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
    
    public long getActiveUsers() {
        try {
            return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/admin/stats/active-users")
                .retrieve()
                .bodyToMono(Long.class)
                .block();
        } catch (Exception e) {
            log.error("활성 사용자 수 조회 중 오류 발생: {}", e.getMessage());
            return 0;
        }
    }
} 
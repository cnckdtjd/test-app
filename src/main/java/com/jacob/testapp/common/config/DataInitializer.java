package com.jacob.testapp.common.config;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Autowired
    public DataInitializer(UserRepository userRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 관리자 계정 생성
        createAdminIfNotExists();
        
        // 테스트 사용자 계정 생성
        createTestUserIfNotExists();
        
        // 샘플 상품 생성
        if (productRepository.count() == 0) {
            createSampleProducts();
        }
    }

    private void createAdminIfNotExists() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .name("관리자")
                    .email("admin@example.com")
                    .role(User.Role.ROLE_ADMIN)
                    .status(User.Status.ACTIVE)
                    .enabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .loginAttempts(0)
                    .accountLocked(false)
                    .build();
            userRepository.save(admin);
            System.out.println("관리자 계정이 생성되었습니다: admin/admin");
        }
    }

    private void createTestUserIfNotExists() {
        if (!userRepository.existsByUsername("user")) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user"))
                    .name("테스트 사용자")
                    .email("user@example.com")
                    .role(User.Role.ROLE_USER)
                    .status(User.Status.ACTIVE)
                    .enabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .loginAttempts(0)
                    .accountLocked(false)
                    .build();
            userRepository.save(user);
            System.out.println("테스트 사용자 계정이 생성되었습니다: user/user");
        }
    }

    private void createSampleProducts() {
        System.out.println("샘플 상품 100개를 생성합니다...");
        
        List<String> courseNames = Arrays.asList(
            "스프링 부트 완전 정복", "자바 마스터 클래스", "파이썬 기초부터 고급까지",
            "리액트 실전 프로젝트", "데이터 분석 입문", "알고리즘 문제 해결 전략",
            "클라우드 컴퓨팅 기초", "도커와 쿠버네티스", "머신러닝 기초",
            "웹 개발 완전 가이드", "모바일 앱 개발", "데이터베이스 설계와 구현"
        );
        
        List<String> descriptions = Arrays.asList(
            "이 강의는 초보자부터 전문가까지 모두를 위한 완벽한 가이드입니다.",
            "실무에서 바로 적용할 수 있는 실용적인 기술을 배울 수 있습니다.",
            "기초 개념부터 고급 주제까지 체계적으로 학습할 수 있습니다.",
            "다양한 실습 예제를 통해 실전 경험을 쌓을 수 있습니다.",
            "최신 트렌드와 기술을 반영한 현업 중심의 커리큘럼입니다.",
            "전문가의 노하우와 팁을 통해 효율적인 학습이 가능합니다."
        );
        
        List<String> imageUrls = Arrays.asList(
            "https://via.placeholder.com/300x200?text=Java",
            "https://via.placeholder.com/300x200?text=Spring",
            "https://via.placeholder.com/300x200?text=Python",
            "https://via.placeholder.com/300x200?text=React",
            "https://via.placeholder.com/300x200?text=Database",
            "https://via.placeholder.com/300x200?text=Cloud",
            "https://via.placeholder.com/300x200?text=Mobile",
            "https://via.placeholder.com/300x200?text=Algorithm"
        );
        
        Product.Category[] categories = Product.Category.values();
        
        for (int i = 1; i <= 100; i++) {
            String name = courseNames.get(random.nextInt(courseNames.size())) + " " + i;
            String description = descriptions.get(random.nextInt(descriptions.size()));
            BigDecimal price = new BigDecimal(10000 + (random.nextInt(20) * 5000));
            int stock = 10 + random.nextInt(100);
            String imageUrl = imageUrls.get(random.nextInt(imageUrls.size()));
            Product.Category category = categories[random.nextInt(categories.length)];
            
            Product product = Product.builder()
                    .name(name)
                    .description(description)
                    .price(price)
                    .stock(stock)
                    .imageUrl(imageUrl)
                    .category(category)
                    .status(Product.Status.ACTIVE)
                    .build();
            
            productRepository.save(product);
        }
        
        System.out.println("샘플 상품 생성이 완료되었습니다.");
    }
} 
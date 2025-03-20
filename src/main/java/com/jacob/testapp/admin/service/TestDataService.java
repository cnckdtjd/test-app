package com.jacob.testapp.admin.service;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트 데이터 생성 및 롤백을 위한 서비스
 */
@Service
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    // 카테고리 목록
    private final String[] PRODUCT_PREFIXES = {
        "프리미엄", "클래식", "슈퍼", "울트라", "뉴", "베스트", "스페셜", "디럭스", "스마트", "퓨어"
    };

    // 상품명 접미사
    private final String[] PRODUCT_SUFFIXES = {
        "플러스", "프로", "맥스", "미니", "라이트", "Z", "X", "S", "알파", "DX"
    };

    // 상품 기본 이름
    private final String[] PRODUCT_BASES = {
        "스마트폰", "노트북", "티셔츠", "청바지", "운동화", "과자", "음료", "소설", "만화책", "테이블", 
        "의자", "냄비", "프라이팬", "립스틱", "파운데이션", "축구공", "농구공", "인형", "레고", "연필"
    };

    public TestDataService(
            UserRepository userRepository, 
            ProductRepository productRepository, 
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 테스트 데이터 생성
     * @param userCount 생성할 사용자 수
     * @param productCount 생성할 상품 수
     * @return 생성된 데이터 요약
     */
    @Transactional
    public String generateTestData(int userCount, int productCount) {
        logger.info("테스트 데이터 생성 시작: 사용자 {}명, 상품 {}개", userCount, productCount);
        
        AtomicInteger userSuccessCount = new AtomicInteger(0);
        AtomicInteger productSuccessCount = new AtomicInteger(0);
        AtomicInteger userFailCount = new AtomicInteger(0);
        AtomicInteger productFailCount = new AtomicInteger(0);
        
        try {
            // 사용자 생성
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= userCount; i++) {
                try {
                    String username = "testuser" + i;
                    String email = "testuser" + i + "@example.com";
                    String password = passwordEncoder.encode("password" + i);
                    
                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .password(password)
                            .name("테스트 사용자 " + i)
                            .phone("010-1234-" + String.format("%04d", i))
                            .role(User.Role.ROLE_USER)
                            .status(User.Status.ACTIVE)
                            .loginAttempts(0)
                            .enabled(true)
                            .accountLocked(false)
                            .lastLoginAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                            .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                            .build();
                    
                    users.add(user);
                    userSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("사용자 생성 중 오류 발생 (인덱스: {}): {}", i, e.getMessage());
                    userFailCount.incrementAndGet();
                }
                
                // 메모리 관리를 위해 일정 크기마다 저장
                if (users.size() >= 100) {
                    userRepository.saveAll(users);
                    users.clear();
                }
            }
            
            // 남은 사용자 저장
            if (!users.isEmpty()) {
                userRepository.saveAll(users);
            }
            
            // 상품 생성
            List<Product> products = new ArrayList<>();
            for (int i = 1; i <= productCount; i++) {
                try {
                    String name = generateProductName();
                    Product.Category category = getRandomCategory();
                    BigDecimal price = new BigDecimal(1000 + random.nextInt(100000)).setScale(0);
                    int stock = random.nextInt(1000);
                    
                    Product product = Product.builder()
                            .name(name)
                            .description(name + "의 상세 설명입니다. 이 제품은 높은 품질과 합리적인 가격을 자랑합니다.")
                            .price(price)
                            .stock(stock)
                            .category(category)
                            .status(random.nextDouble() < 0.9 ? Product.Status.ACTIVE : Product.Status.INACTIVE)
                            .version(0L) // 낙관적 락을 위한 버전 필드 추가
                            .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                            .build();
                    
                    products.add(product);
                    productSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("상품 생성 중 오류 발생 (인덱스: {}): {}", i, e.getMessage());
                    productFailCount.incrementAndGet();
                }
                
                // 메모리 관리를 위해 일정 크기마다 저장
                if (products.size() >= 100) {
                    productRepository.saveAll(products);
                    products.clear();
                }
            }
            
            // 남은 상품 저장
            if (!products.isEmpty()) {
                productRepository.saveAll(products);
            }
            
            logger.info("테스트 데이터 생성 완료: 사용자 {}명(실패: {}명), 상품 {}개(실패: {}개)", 
                    userSuccessCount.get(), userFailCount.get(), 
                    productSuccessCount.get(), productFailCount.get());
            
            return String.format("테스트 데이터 생성 완료: 사용자 %d명(실패: %d명), 상품 %d개(실패: %d개)",
                    userSuccessCount.get(), userFailCount.get(), 
                    productSuccessCount.get(), productFailCount.get());
        } catch (Exception e) {
            logger.error("테스트 데이터 생성 중 예외 발생: {}", e.getMessage(), e);
            return "테스트 데이터 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 테스트 데이터 롤백
     * @return 삭제된 데이터 요약
     */
    @Transactional
    public String rollbackTestData() {
        logger.info("테스트 데이터 롤백 시작");
        
        int deletedUsers = 0;
        int deletedProducts = 0;
        
        try {
            // 테스트 사용자 삭제 (testuser로 시작하는 사용자)
            List<User> testUsers = userRepository.findByUsernameStartingWith("testuser");
            deletedUsers = testUsers.size();
            userRepository.deleteAll(testUsers);
            
            // 테스트 상품 삭제 (특정 접두어가 있는 상품)
            List<Product> testProducts = new ArrayList<>();
            for (String prefix : PRODUCT_PREFIXES) {
                testProducts.addAll(productRepository.findByNameStartingWith(prefix));
            }
            deletedProducts = testProducts.size();
            productRepository.deleteAll(testProducts);
            
            logger.info("테스트 데이터 롤백 완료: 사용자 {}명, 상품 {}개 삭제", deletedUsers, deletedProducts);
            
            return String.format("테스트 데이터 롤백 완료: 사용자 %d명, 상품 %d개 삭제", deletedUsers, deletedProducts);
        } catch (Exception e) {
            logger.error("테스트 데이터 롤백 중 예외 발생: {}", e.getMessage(), e);
            return "테스트 데이터 롤백 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    /**
     * 랜덤 상품명 생성
     * @return 생성된 상품명
     */
    private String generateProductName() {
        String prefix = PRODUCT_PREFIXES[random.nextInt(PRODUCT_PREFIXES.length)];
        String base = PRODUCT_BASES[random.nextInt(PRODUCT_BASES.length)];
        String suffix = random.nextDouble() < 0.7 ? PRODUCT_SUFFIXES[random.nextInt(PRODUCT_SUFFIXES.length)] : "";
        
        return prefix + " " + base + (suffix.isEmpty() ? "" : " " + suffix);
    }
    
    /**
     * 랜덤 카테고리 반환
     * @return 랜덤 카테고리
     */
    private Product.Category getRandomCategory() {
        Product.Category[] categories = Product.Category.values();
        return categories[random.nextInt(categories.length)];
    }
} 
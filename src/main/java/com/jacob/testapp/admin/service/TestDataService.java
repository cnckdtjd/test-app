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

    // 테스트 데이터 식별용 상수
    private static final String TEST_USER_FLAG = "TEST_USER";

    // 카테고리(접두어) 목록 – 책의 특성이나 난이도, 강조점을 나타냄
    private final String[] PRODUCT_PREFIXES = {
        "최신", "실전", "추천", "베스트", "핵심", "입문", "전문가용", "실무", "고급", "기초"
    };

    // 상품명 접미사 – 책의 종류나 버전, 시리즈를 표현하는 용도로 사용
    private final String[] PRODUCT_SUFFIXES = {
        "편", "교재", "핸드북", "가이드", "시리즈", "개정판", "에디션", "마스터", "리뷰", "노트"
    };

    // 상품 기본 이름 – IT 및 기타 책 관련 주제
    private final String[] PRODUCT_BASES = {
        "IT책", "프로그래밍", "자바", "파이썬", "웹개발", "데이터베이스", "네트워크", "보안", "클라우드", "인공지능",
        "머신러닝", "알고리즘", "소프트웨어 공학", "시스템 관리", "데브옵스", "빅데이터", "IoT", "블록체인", "모바일 앱", "UI/UX 디자인",
        "오픈소스", "리눅스", "윈도우", "애플리케이션", "모바일 개발", "프론트엔드", "백엔드", "풀스택", "디지털 트랜스포메이션", "가상화",
        "컨테이너", "Kubernetes", "Docker", "인프라 자동화", "스케일러빌리티", "클라우드 네이티브", "네트워크 보안", "사이버 보안", "정보보호", "코딩",
        "개발 방법론", "소프트웨어 테스트", "버전 관리", "깃", "CI/CD", "애자일", "스크럼", "데이터 분석", "데이터 시각화", "R 프로그래밍",
        "Matlab", "C++", "C#", "자바스크립트", "React", "Angular", "Vue.js", "Node.js", "Express", "SQL",
        "NoSQL", "MongoDB", "Redis", "MySQL", "PostgreSQL", "MariaDB", "Oracle", "ERP", "CRM", "SaaS",
        "PaaS", "IaaS", "테크 스타트업", "디지털 마케팅", "SEO", "웹 호스팅", "도메인", "API", "머신비전", "자율 주행",
        "로보틱스", "AR/VR", "메타버스", "핀테크", "헬스케어 IT", "전자상거래", "소프트웨어 아키텍처", "시스템 설계", "프로그래밍 언어", "컴파일러",
        "디버깅", "데이터 구조", "운영체제", "컴퓨터 구조", "정보 이론", "모바일 보안", "클라우드 보안", "네트워크 프로그래밍", "스마트 IoT", "웨어러블"
    };

    // 영문 이름 배열 (First Names) - 100개
    private final String[] FIRST_NAMES = {
            "Jacob", "Michael", "Emma", "Olivia", "Noah", "Liam", "Ava", "Sophia", "William", "James",
            "Benjamin", "Elijah", "Mia", "Charlotte", "Amelia", "Evelyn", "Harper", "Abigail", "Emily", "Madison",
            "Alexander", "Daniel", "Matthew", "Aiden", "Henry", "Sebastian", "Jack", "Samuel", "Owen", "Gabriel",
            "Logan", "Zoe", "Lily", "Ella", "Chloe", "Grace", "Victoria", "Scarlett", "Aria", "Penelope",
            "Hannah", "Leah", "Stella", "Nora", "Addison", "Lillian", "Lucy", "Paisley", "Savannah", "Brooklyn",
            "Bella", "Claire", "Skylar", "Sadie", "Genesis", "Aubrey", "Alice", "Sarah", "Allison", "Hailey",
            "Anna", "Riley", "Peyton", "Mackenzie", "Nevaeh", "Sofia", "Camila", "Maya", "Eliana", "Isabella",
            "Eleanor", "Ellie", "Caroline", "Nova", "Violet", "Ruby", "Kennedy", "Lila", "Vera", "Ivy",
            "Adeline", "Gabriella", "Luna", "Cora", "Delilah", "Lola", "Elise", "Eva", "Willow", "Isla",
            "Naomi", "Jade", "Sienna", "Emilia", "Vivian", "Adalyn", "Quinn", "Piper", "Aurora", "Serenity"
    };

    // 영문 성 배열 (Last Names) - 100개
    private final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts",
            "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards", "Collins", "Reyes",
            "Stewart", "Morris", "Morales", "Murphy", "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper",
            "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson",
            "Watson", "Brooks", "Chavez", "Wood", "James", "Bennett", "Gray", "Mendoza", "Ruiz", "Hughes",
            "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers", "Long", "Ross", "Foster", "Jimenez"
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

        List<User> users;
        try {
            // 사용자 생성
            users = new ArrayList<>();
            for (int i = 1; i <= userCount; i++) {
                try {
                    // 랜덤하게 이름과 성 선택
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    // 사용자명 구성: 예를 들어, "JacobSmith1234"
                    String username = firstName + lastName + String.format("%04d", i);
                    // 이메일은 모두 소문자로 구성
                    String email = username.toLowerCase() + "@example.com";
                    String password = passwordEncoder.encode("password" + i);

                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .password(password)
                            .name(firstName + " " + lastName) // 실제 이름은 띄어쓰기로 구분
                            .phone("010-1234-" + String.format("%04d", i))
                            .role(User.Role.ROLE_USER)
                            .status(User.Status.ACTIVE)
                            .loginAttempts(0)
                            .enabled(true)
                            .accountLocked(false)
                            .remarks(TEST_USER_FLAG) // 테스트 사용자 식별용 플래그
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
                            .remarks(TEST_USER_FLAG) // 테스트 상품 식별용 플래그
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
            // 테스트 사용자 삭제 (remarks 필드로 식별)
            List<User> testUsers = userRepository.findByRemarks(TEST_USER_FLAG);
            deletedUsers = testUsers.size();
            userRepository.deleteAll(testUsers);
            
            // 테스트 상품 삭제 (remarks 필드로 식별)
            List<Product> testProducts = productRepository.findByRemarks(TEST_USER_FLAG);
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
package com.jacob.testapp.admin.service;

import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.repository.ProductRepository;
import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 테스트 데이터 생성 및 롤백을 위한 서비스
 */
@Service
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final Random random = new Random();
    
    // EntityManager 주입
    @PersistenceContext
    private EntityManager entityManager;
    
    // 성능 최적화를 위한 설정값
    // 스레드 풀 크기: CPU 코어 수 * 2 (I/O 작업이 많으므로)
    private static final int THREAD_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    
    // 데이터 배치 처리 크기 (메모리와 성능 균형 최적화)
    private static final int BATCH_SIZE = 1000;

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
            PasswordEncoder passwordEncoder,
            CartRepository cartRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
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
        long startTime = System.currentTimeMillis();

        AtomicInteger userSuccessCount = new AtomicInteger(0);
        AtomicInteger productSuccessCount = new AtomicInteger(0);
        AtomicInteger userFailCount = new AtomicInteger(0);
        AtomicInteger productFailCount = new AtomicInteger(0);

        try {
            // 커스텀 스레드 풀 생성 (CPU 바운드 작업과 I/O 바운드 작업에 최적화)
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            
            // 병렬로 사용자 및 상품 데이터 생성 실행
            CompletableFuture<Void> usersFuture = generateUsersAsync(userCount, userSuccessCount, userFailCount, executorService);
            CompletableFuture<Void> productsFuture = generateProductsAsync(productCount, productSuccessCount, productFailCount, executorService);
            
            // 모든 작업 완료 대기
            CompletableFuture.allOf(usersFuture, productsFuture).join();
            
            // 스레드 풀 종료
            executorService.shutdown();

            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            // 초당 처리량 계산
            double userTps = userSuccessCount.get() * 1000.0 / durationMs;
            double productTps = productSuccessCount.get() * 1000.0 / durationMs;
            
            logger.info("테스트 데이터 생성 완료: 사용자 {}명(실패: {}명), 상품 {}개(실패: {}개), 소요 시간: {}ms, TPS: 사용자 {}/s, 상품 {}/s",
                    userSuccessCount.get(), userFailCount.get(),
                    productSuccessCount.get(), productFailCount.get(),
                    durationMs, String.format("%.2f", userTps), String.format("%.2f", productTps));

            return String.format("테스트 데이터 생성 완료: 사용자 %d명(실패: %d명), 상품 %d개(실패: %d명), 소요 시간: %dms, TPS: 사용자 %.2f/s, 상품 %.2f/s",
                    userSuccessCount.get(), userFailCount.get(),
                    productSuccessCount.get(), productFailCount.get(),
                    durationMs, userTps, productTps);
        } catch (Exception e) {
            logger.error("테스트 데이터 생성 중 예외 발생: {}", e.getMessage(), e);
            return "테스트 데이터 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    /**
     * 사용자 데이터를 비동기적으로 생성
     */
    private CompletableFuture<Void> generateUsersAsync(int userCount, AtomicInteger successCount, AtomicInteger failCount, ExecutorService executorService) {
        return CompletableFuture.runAsync(() -> {
            // ForkJoinPool 사용하여 더 효율적인 병렬 처리 구현
            ForkJoinPool customPool = new ForkJoinPool(THREAD_POOL_SIZE);
            
            try {
                customPool.submit(() -> {
                    // 사용자 생성 및 배치 처리를 위한 스트림 분할
                    int batchCount = (userCount + BATCH_SIZE - 1) / BATCH_SIZE; // 올림 나눗셈
                    
                    IntStream.range(0, batchCount).parallel().forEach(batchIndex -> {
                        int startIdx = batchIndex * BATCH_SIZE + 1;
                        int endIdx = Math.min((batchIndex + 1) * BATCH_SIZE, userCount);
                        
                        List<User> userBatch = new ArrayList<>();
                        
                        for (int i = startIdx; i <= endIdx; i++) {
                            try {
                                // ThreadLocalRandom 사용으로 스레드 간 경합 감소
                                ThreadLocalRandom localRandom = ThreadLocalRandom.current();
                                
                                // 랜덤하게 이름과 성 선택
                                String firstName = FIRST_NAMES[localRandom.nextInt(FIRST_NAMES.length)];
                                String lastName = LAST_NAMES[localRandom.nextInt(LAST_NAMES.length)];
                                // 사용자명 구성: 예를 들어, "JacobSmith1234"
                                String username = firstName + lastName + String.format("%04d", i);
                                // 이메일은 모두 소문자로 구성
                                String email = username.toLowerCase() + "@example.com";
                                
                                // *** UserService.register 메소드의 중복 체크 로직 통합 ***
                                // 사용자명 중복 체크
                                if (userRepository.existsByUsername(username)) {
                                    logger.warn("중복된 사용자명 발견: {}", username);
                                    // 접미사에 랜덤 숫자 추가하여 고유하게 만듦
                                    username = username + "_" + localRandom.nextInt(1000);
                                }
                                
                                // 이메일 중복 체크
                                if (userRepository.existsByEmail(email)) {
                                    logger.warn("중복된 이메일 발견: {}", email);
                                    // 사용자명에 랜덤 접미사 추가
                                    email = username.toLowerCase() + "_" + localRandom.nextInt(1000) + "@example.com";
                                }
                                
                                // *** UserService.register와 일치하도록 패스워드 암호화 로직 수정 ***
                                // 테스트 사용자는 항상 password123으로 로그인할 수 있도록 고정 비밀번호 사용
                                String password = passwordEncoder.encode("test");

                                User user = User.builder()
                                        .username(username)
                                        .email(email)
                                        .password(password)
                                        .name(firstName + " " + lastName) // 실제 이름은 띄어쓰기로 구분
                                        .phone("010-1234-" + String.format("%04d", i))
                                        .role(User.Role.ROLE_USER) // UserService.register와 일치
                                        .status(User.Status.ACTIVE) // UserService.register와 일치
                                        .loginAttempts(0) // UserService.register와 일치
                                        .enabled(true) // UserService.register와 일치
                                        .accountLocked(false) // UserService.register와 일치
                                        .remarks(TEST_USER_FLAG) // 테스트 사용자 식별용 플래그
                                        .lastLoginAt(LocalDateTime.now())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                userRepository.save(user);
                                
                                // 사용자의 장바구니 생성
                                Cart cart = Cart.builder()
                                        .user(user)
                                        .totalPrice(BigDecimal.ZERO)
                                        .build();
                                cartRepository.save(cart);
                                
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                logger.error("사용자 생성 중 오류 발생 (인덱스: {}): {}", i, e.getMessage());
                                failCount.incrementAndGet();
                            }
                        }
                    });
                }).get(); // 작업 완료 대기
            } catch (Exception e) {
                logger.error("사용자 생성 작업 중 오류 발생: {}", e.getMessage(), e);
            } finally {
                customPool.shutdown();
            }
        }, executorService);
    }
    
    /**
     * 사용자 데이터 배치 저장 (트랜잭션 처리)
     */
    @Transactional
    public void saveUserBatch(List<User> users) {
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }
    }
    
    /**
     * 상품 데이터를 비동기적으로 생성
     */
    private CompletableFuture<Void> generateProductsAsync(int productCount, AtomicInteger successCount, AtomicInteger failCount, ExecutorService executorService) {
        return CompletableFuture.runAsync(() -> {
            // ForkJoinPool 사용하여 더 효율적인 병렬 처리 구현
            ForkJoinPool customPool = new ForkJoinPool(THREAD_POOL_SIZE);
            
            try {
                customPool.submit(() -> {
                    // 상품 생성 및 배치 처리를 위한 스트림 분할
                    int batchCount = (productCount + BATCH_SIZE - 1) / BATCH_SIZE; // 올림 나눗셈
                    
                    IntStream.range(0, batchCount).parallel().forEach(batchIndex -> {
                        int startIdx = batchIndex * BATCH_SIZE + 1;
                        int endIdx = Math.min((batchIndex + 1) * BATCH_SIZE, productCount);
                        
                        List<Product> productBatch = new ArrayList<>();
                        
                        for (int i = startIdx; i <= endIdx; i++) {
                            try {
                                // ThreadLocalRandom 사용으로 스레드 간 경합 감소
                                ThreadLocalRandom localRandom = ThreadLocalRandom.current();
                                
                                String name = generateProductName(localRandom);
                                Product.Category category = getRandomCategory(localRandom);
                                BigDecimal price = new BigDecimal(1000 + localRandom.nextInt(100000)).setScale(0);
                                int stock = localRandom.nextInt(1000);

                                Product product = Product.builder()
                                        .name(name)
                                        .description(name + "의 상세 설명입니다. 이 제품은 높은 품질과 합리적인 가격을 자랑합니다.")
                                        .price(price)
                                        .stock(stock)
                                        .category(category)
                                        .status(localRandom.nextDouble() < 0.9 ? Product.Status.ACTIVE : Product.Status.INACTIVE)
                                        .remarks(TEST_USER_FLAG) // 테스트 상품 식별용 플래그
                                        .version(0L) // 낙관적 락을 위한 버전 필드 추가
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                productBatch.add(product);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                logger.error("상품 생성 중 오류 발생 (인덱스: {}): {}", i, e.getMessage());
                                failCount.incrementAndGet();
                            }
                        }
                        
                        // 트랜잭션 내에서 배치 저장
                        if (!productBatch.isEmpty()) {
                            saveProductBatch(productBatch);
                        }
                    });
                }).get(); // 작업 완료 대기
            } catch (Exception e) {
                logger.error("상품 생성 작업 중 오류 발생: {}", e.getMessage(), e);
            } finally {
                customPool.shutdown();
            }
        }, executorService);
    }
    
    /**
     * 상품 데이터 배치 저장 (트랜잭션 처리)
     */
    @Transactional
    public void saveProductBatch(List<Product> products) {
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }
    }

    /**
     * 테스트 데이터 롤백
     * @return 삭제된 데이터 요약
     */
    @Transactional
    public String rollbackTestData() {
        logger.info("테스트 데이터 롤백 시작");
        long startTime = System.currentTimeMillis();
        
        int deletedUsers = 0;
        int deletedProducts = 0;
        int deletedCartItems = 0;
        int deletedOrderItems = 0;
        
        try {
            // 1. 먼저 테스트 상품과 연관된 장바구니 아이템을 삭제 (외래 키 제약 조건 해결)
            logger.info("장바구니 아이템 삭제 시작 - 테스트 상품 관련");
            
            // JPQL을 사용하여 테스트 상품 ID와 연결된 장바구니 아이템을 삭제
            String cartItemsDeleteQuery = 
                "DELETE FROM CartItem ci WHERE ci.product.id IN " +
                "(SELECT p.id FROM Product p WHERE p.remarks = :testFlag)";
            
            // 직접 EntityManager를 사용하여 벌크 삭제 수행
            int deletedItems = entityManager.createQuery(cartItemsDeleteQuery)
                .setParameter("testFlag", TEST_USER_FLAG)
                .executeUpdate();
                
            logger.info("테스트 상품 관련 장바구니 아이템 {}개 삭제 완료", deletedItems);
            deletedCartItems = deletedItems;
            
            // 2. 테스트 상품과 연관된 주문 아이템을 삭제 (외래 키 제약 조건 해결)
            logger.info("주문 아이템 삭제 시작 - 테스트 상품 관련");
            
            String orderItemsDeleteQuery = 
                "DELETE FROM OrderItem oi WHERE oi.product.id IN " +
                "(SELECT p.id FROM Product p WHERE p.remarks = :testFlag)";
                
            deletedItems = entityManager.createQuery(orderItemsDeleteQuery)
                .setParameter("testFlag", TEST_USER_FLAG)
                .executeUpdate();
                
            logger.info("테스트 상품 관련 주문 아이템 {}개 삭제 완료", deletedItems);
            deletedOrderItems = deletedItems;
            
            // 3. 테스트 사용자와 연관된 장바구니를 삭제
            logger.info("장바구니 삭제 시작 - 테스트 사용자 관련");
            
            String cartsDeleteQuery = 
                "DELETE FROM Cart c WHERE c.user.id IN " +
                "(SELECT u.id FROM User u WHERE u.remarks = :testFlag)";
                
            deletedItems = entityManager.createQuery(cartsDeleteQuery)
                .setParameter("testFlag", TEST_USER_FLAG)
                .executeUpdate();
                
            logger.info("테스트 사용자 관련 장바구니 {}개 삭제 완료", deletedItems);
            
            // 4. 테스트 사용자와 연관된 주문을 삭제
            logger.info("주문 삭제 시작 - 테스트 사용자 관련");
            
            String ordersDeleteQuery = 
                "DELETE FROM Order o WHERE o.user.id IN " +
                "(SELECT u.id FROM User u WHERE u.remarks = :testFlag)";
                
            deletedItems = entityManager.createQuery(ordersDeleteQuery)
                .setParameter("testFlag", TEST_USER_FLAG)
                .executeUpdate();
                
            logger.info("테스트 사용자 관련 주문 {}개 삭제 완료", deletedItems);
            
            // 5. 이제 테스트 상품 삭제 (자식 레코드가 모두 삭제된 상태)
            List<Product> testProducts = productRepository.findByRemarks(TEST_USER_FLAG);
            logger.info("롤백할 테스트 상품 발견: {}개", testProducts.size());
            
            if (testProducts.isEmpty()) {
                logger.warn("롤백할 테스트 상품이 없습니다. remarks={}", TEST_USER_FLAG);
            } else {
                // 상품 ID 목록 로깅 (문제 해결용)
                if (logger.isDebugEnabled()) {
                    List<Long> productIds = testProducts.stream().map(Product::getId).collect(Collectors.toList());
                    logger.debug("롤백할 상품 ID 목록: {}", productIds);
                }
                
                // JPQL로 직접 삭제 (저장소 사용 대신)
                String productsDeleteQuery = 
                    "DELETE FROM Product p WHERE p.remarks = :testFlag";
                    
                deletedProducts = entityManager.createQuery(productsDeleteQuery)
                    .setParameter("testFlag", TEST_USER_FLAG)
                    .executeUpdate();
                    
                logger.info("테스트 상품 {}개 삭제 완료", deletedProducts);
            }
            
            // 6. 마지막으로 테스트 사용자 삭제
            List<User> testUsers = userRepository.findByRemarks(TEST_USER_FLAG);
            logger.info("롤백할 테스트 사용자 발견: {}명", testUsers.size());
            
            if (testUsers.isEmpty()) {
                logger.warn("롤백할 테스트 사용자가 없습니다. remarks={}", TEST_USER_FLAG);
            } else {
                // 사용자 ID 목록 로깅 (문제 해결용)
                if (logger.isDebugEnabled()) {
                    List<Long> userIds = testUsers.stream().map(User::getId).collect(Collectors.toList());
                    logger.debug("롤백할 사용자 ID 목록: {}", userIds);
                }
                
                // JPQL로 직접 삭제 (저장소 사용 대신)
                String usersDeleteQuery = 
                    "DELETE FROM User u WHERE u.remarks = :testFlag";
                    
                deletedUsers = entityManager.createQuery(usersDeleteQuery)
                    .setParameter("testFlag", TEST_USER_FLAG)
                    .executeUpdate();
                    
                logger.info("테스트 사용자 {}명 삭제 완료", deletedUsers);
            }
            
            // 명시적 flush 없음 - JPQL delete 쿼리는 자동으로 flush 됨
            
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            // 초당 처리량 계산
            double deleteTps = (deletedUsers + deletedProducts + deletedCartItems + deletedOrderItems) * 1000.0 / Math.max(1, durationMs);
            
            logger.info("테스트 데이터 롤백 완료: 사용자 {}명, 상품 {}개, 장바구니 아이템 {}개, 주문 아이템 {}개 삭제, 소요 시간: {}ms, TPS: {}/s", 
                    deletedUsers, deletedProducts, deletedCartItems, deletedOrderItems, durationMs, String.format("%.2f", deleteTps));
            
            return String.format("테스트 데이터 롤백 완료: 사용자 %d명, 상품 %d개 삭제, 소요 시간: %dms, TPS: %.2f/s", 
                    deletedUsers, deletedProducts, durationMs, deleteTps);
        } catch (Exception e) {
            logger.error("테스트 데이터 롤백 중 예외 발생: {}", e.getMessage(), e);
            return "테스트 데이터 롤백 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    /**
     * 랜덤 상품명 생성 (ThreadLocalRandom 사용)
     * @return 생성된 상품명
     */
    private String generateProductName(ThreadLocalRandom random) {
        String prefix = PRODUCT_PREFIXES[random.nextInt(PRODUCT_PREFIXES.length)];
        String base = PRODUCT_BASES[random.nextInt(PRODUCT_BASES.length)];
        String suffix = random.nextDouble() < 0.7 ? PRODUCT_SUFFIXES[random.nextInt(PRODUCT_SUFFIXES.length)] : "";
        
        return prefix + " " + base + (suffix.isEmpty() ? "" : " " + suffix);
    }
    
    /**
     * 랜덤 카테고리 반환 (ThreadLocalRandom 사용)
     * @return 랜덤 카테고리
     */
    private Product.Category getRandomCategory(ThreadLocalRandom random) {
        Product.Category[] categories = Product.Category.values();
        return categories[random.nextInt(categories.length)];
    }
} 
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
import java.util.concurrent.ThreadLocalRandom;

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
            // 랜덤 주소 생성
            String randomAddress = generateRandomAddress();
            String randomPhone = generateRandomPhoneNumber();
            
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user"))
                    .name("테스트 사용자")
                    .email("user@example.com")
                    .phone(randomPhone)
                    .address(randomAddress)
                    .role(User.Role.ROLE_USER)
                    .status(User.Status.ACTIVE)
                    .enabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .loginAttempts(0)
                    .accountLocked(false)
                    .cashBalance(generateRandomCashBalance()) // 50,000원 ~ 200,000원 (100원 단위)
                    .build();
            userRepository.save(user);
            System.out.println("테스트 사용자 계정이 생성되었습니다: user/user");
        }
    }

    /**
     * 50,000원 ~ 200,000원 사이의 랜덤 현금 잔액 생성 (100원 단위로 절삭)
     */
    private Long generateRandomCashBalance() {
        // 50,000원에서 200,000원 사이의 랜덤 값 생성
        int randomAmount = 50000 + random.nextInt(150001);
        // 100원 단위로 절삭
        return (long) (randomAmount / 100) * 100;
    }
    
    /**
     * 랜덤 한국 주소 생성
     */
    private String generateRandomAddress() {
        String[] cities = {"서울특별시", "부산광역시", "인천광역시", "대구광역시", "대전광역시", "광주광역시", "울산광역시", "세종특별자치시"};
        String[] districts = {"중구", "동구", "서구", "남구", "북구", "강남구", "강서구", "강동구", "송파구", "마포구", "성동구", "성북구", "영등포구"};
        String[] details = {"대로", "로", "길", "거리", "아파트", "빌딩", "오피스텔", "주택"};
        
        String randomCity = cities[random.nextInt(cities.length)];
        String randomDistrict = districts[random.nextInt(districts.length)];
        String randomDetail = details[random.nextInt(details.length)];
        int randomNumber = random.nextInt(100) + 1;
        
        return randomCity + " " + randomDistrict + " " + randomNumber + randomDetail + " " + 
               (random.nextInt(100) + 1) + "동 " + (random.nextInt(1000) + 1) + "호";
    }
    
    /**
     * 랜덤 전화번호 생성 (010-XXXX-XXXX 형식)
     */
    private String generateRandomPhoneNumber() {
        return String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
    }

    private void createSampleProducts() {
        System.out.println("샘플 상품 100개를 생성합니다...");
        
        List<String> courseNames = Arrays.asList(
                "자바 프로그래밍 기초부터 활용까지", "스프링 부트 개발 실무", "파이썬 데이터 사이언스",
                "리액트 웹 애플리케이션 구축", "SQL 데이터베이스 마스터", "알고리즘과 자료구조 완벽 정리",
                "AWS 클라우드 기초 활용", "도커 & 쿠버네티스 실습", "텐서플로우로 배우는 머신러닝",
                "모던 자바스크립트 웹 개발", "안드로이드 & iOS 앱 개발", "NoSQL 데이터베이스 입문",
                "리눅스 시스템 관리 및 운영", "네트워크 보안 입문", "깃(Git)과 깃허브(GitHub) 활용법",
                "프론트엔드 UI/UX 디자인 기초", "빅데이터 분석 기술 입문", "DevOps CI/CD 파이프라인",
                "사이버 보안 실무 가이드", "마이크로서비스 아키텍처 설계"
        );

        String[] DESCRIPTIONS = {
                "기초부터 차근차근 배우며 탄탄한 기본기를 다질 수 있습니다.",
                "실제 프로젝트에서 활용할 수 있는 실전 스킬을 습득합니다.",
                "이론과 실습을 균형 있게 구성하여 확실한 이해를 돕습니다.",
                "다양한 실습 예제로 손쉽게 기술을 익힐 수 있습니다.",
                "최신 트렌드를 반영한 실무 중심의 강의로 구성되어 있습니다.",
                "경험 많은 전문가가 제공하는 깊이 있는 노하우를 전달합니다.",
                "초보자도 쉽게 따라 할 수 있는 상세한 설명을 제공합니다.",
                "최신 기술과 지식을 빠르게 습득할 수 있도록 구성되었습니다.",
                "실제 현업에서 자주 겪는 문제들을 해결하는 방법을 알려줍니다.",
                "자세한 예시와 풍부한 자료로 이해도를 높입니다.",
                "성공적인 취업과 이직을 위한 필수 기술을 다룹니다.",
                "다양한 난이도의 예제를 통해 점진적으로 실력을 높일 수 있습니다.",
                "핵심 원리를 중심으로 실력을 단기간에 향상시킵니다.",
                "복잡한 이론도 쉽게 이해할 수 있도록 구성했습니다.",
                "현업 전문가들의 생생한 경험과 사례를 담고 있습니다.",
                "학습 효율을 극대화할 수 있는 최적의 커리큘럼을 제공합니다.",
                "기본 개념부터 응용 기술까지 폭넓게 다룹니다.",
                "반복 학습과 실습을 통해 확실한 실력을 키울 수 있습니다.",
                "빠르고 명확한 설명으로 시간을 절약할 수 있습니다.",
                "이해가 어려운 부분도 명쾌하게 해결할 수 있도록 안내합니다.",
                "장기적으로 경쟁력을 유지할 수 있는 기술을 제공합니다.",
                "실제 업무에 바로 적용할 수 있는 강의를 제공합니다.",
                "학습과 실습이 잘 결합된 커리큘럼으로 완벽한 준비를 도와줍니다.",
                "IT 산업에서 필요한 최신 기술을 학습합니다.",
                "짧은 시간 안에 효율적으로 기술을 익힐 수 있는 커리큘럼입니다.",
                "학습한 내용을 실제 환경에서 즉시 적용할 수 있도록 합니다.",
                "자기 주도적인 학습을 할 수 있도록 가이드를 제공합니다.",
                "어려운 주제도 쉽게 설명하여 학습의 부담을 줄여줍니다.",
                "기술적인 깊이를 더하고 실용성을 높인 강의입니다.",
                "강의 후 실무에서 바로 활용할 수 있는 실전 경험을 제공합니다.",
                "다양한 실습을 통해 실력을 확실히 쌓을 수 있는 과정입니다.",
                "실제 기업에서 사용하는 기술을 바탕으로 구성된 커리큘럼입니다.",
                "기술적 원리와 실제 사용 예시를 함께 다룹니다.",
                "초보자부터 고급자까지 적합한 강의를 제공합니다.",
                "효율적인 학습을 위한 전략적인 구성으로 진행됩니다.",
                "각 단계별로 세심한 피드백을 제공하여 학습 효과를 극대화합니다.",
                "실시간 질의응답으로 궁금증을 즉시 해결할 수 있습니다.",
                "학습 후 자신감을 얻고, 실력을 증명할 수 있습니다.",
                "최고의 강사진과 함께하는 차별화된 교육 프로그램입니다.",
                "현장 경험이 풍부한 강사가 직접 전수하는 노하우입니다.",
                "실제 업무에 필요한 기술만을 선별하여 제공합니다.",
                "학습의 깊이를 더할 수 있는 심화 과정이 포함됩니다.",
                "업계 트렌드를 반영한 최신 기술을 배울 수 있습니다.",
                "목표 달성을 위한 체계적인 커리큘럼이 마련되어 있습니다.",
                "구체적인 프로젝트 예시를 통해 실무에 접목할 수 있습니다."
        };
        
        List<String> imageUrls = Arrays.asList(
                "/img/Java.png",
                "/img/Spring.png",
                "/img/Python.png",
                "/img/React.png",
                "/img/SQL.png",
                "/img/AWS.png",
                "/img/Docker.png",
                "/img/Tensorflow.png",
                "/img/MobileDev.png",
                "/img/NoSQL.png",
                "/img/Linux.png",
                "/img/Security.png",
                "/img/Github.png",
                "/img/UI_UX.png",
                "/img/Bigdata.png",
                "/img/DevOps.png",
                "/img/Cybersecurity.png",
                "/img/MicroServices.png"
        );

        for (int i = 1; i <= 100; i++) {
            String name = courseNames.get(random.nextInt(courseNames.size())) + " " + i;
            String description = DESCRIPTIONS[ThreadLocalRandom.current().nextInt(DESCRIPTIONS.length)];
            // 가격을 10,000~50,000원 사이로 제한하고 100원 단위로 절삭
            BigDecimal price = generateRandomPrice();
            // 재고를 30개 이하로 제한
            int stock = random.nextInt(31); // 0~30 사이의 랜덤 값
            String imageUrl = imageUrls.get(random.nextInt(imageUrls.size()));

            // 상품명에 맞게 카테고리 설정
            Product.Category category = getCategoryByName(name);
            
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

    /**
     * 10,000원 ~ 50,000원 사이의 랜덤 가격 생성 (100원 단위로 절삭)
     */
    private BigDecimal generateRandomPrice() {
        // 10,000원에서 50,000원 사이의 랜덤 값 생성
        int randomAmount = 10000 + random.nextInt(40001);
        // 100원 단위로 절삭
        int truncatedAmount = (randomAmount / 100) * 100;
        return new BigDecimal(truncatedAmount);
    }

    /**
     * 상품명 기반으로 적절한 카테고리 반환
     *
     * @param name 상품명
     * @return 적합한 카테고리
     */
    private Product.Category getCategoryByName(String name) {
        String lowerName = name.toLowerCase();

        if (lowerName.contains("자바") || lowerName.contains("파이썬") || lowerName.contains("프로그래밍") ||
                lowerName.contains("코딩") || lowerName.contains("개발") || lowerName.contains("알고리즘") ||
                lowerName.contains("c++") || lowerName.contains("c#") || lowerName.contains("자바스크립트") ||
                lowerName.contains("react") || lowerName.contains("angular") || lowerName.contains("vue") ||
                lowerName.contains("node")) {
            return Product.Category.프로그래밍;
        } else if (lowerName.contains("데이터베이스") || lowerName.contains("sql") || lowerName.contains("nosql") ||
                lowerName.contains("mongodb") || lowerName.contains("mysql") || lowerName.contains("redis") ||
                lowerName.contains("postgresql")) {
            return Product.Category.데이터베이스;
        } else if (lowerName.contains("네트워크") || lowerName.contains("보안") || lowerName.contains("클라우드") ||
                lowerName.contains("네트워크 보안") || lowerName.contains("사이버 보안") || lowerName.contains("정보보호")) {
            return Product.Category.네트워크_보안;
        } else if (lowerName.contains("리눅스") || lowerName.contains("윈도우") || lowerName.contains("운영체제") ||
                lowerName.contains("시스템 관리") || lowerName.contains("가상화") || lowerName.contains("docker") ||
                lowerName.contains("kubernetes") || lowerName.contains("컨테이너")) {
            return Product.Category.시스템_운영;
        } else if (lowerName.contains("ai") || lowerName.contains("인공지능") || lowerName.contains("머신러닝") ||
                lowerName.contains("딥러닝") || lowerName.contains("빅데이터") || lowerName.contains("데이터 분석") ||
                lowerName.contains("데이터 시각화")) {
            return Product.Category.인공지능_데이터과학;
        } else if (lowerName.contains("모바일") || lowerName.contains("안드로이드") || lowerName.contains("ios") ||
                lowerName.contains("모바일 앱")) {
            return Product.Category.모바일_개발;
        } else if (lowerName.contains("ui") || lowerName.contains("ux") || lowerName.contains("프론트엔드") ||
                lowerName.contains("디자인")) {
            return Product.Category.UI_UX;
        } else if (lowerName.contains("devops") || lowerName.contains("ci/cd") || lowerName.contains("애자일") ||
                lowerName.contains("스크럼") || lowerName.contains("깃") || lowerName.contains("git")) {
            return Product.Category.DevOps;
        } else if (lowerName.contains("iot") || lowerName.contains("스마트") || lowerName.contains("로보틱스") ||
                lowerName.contains("ar") || lowerName.contains("vr") || lowerName.contains("메타버스")) {
            return Product.Category.IoT_혁신기술;
        } else if (lowerName.contains("전자상거래") || lowerName.contains("핀테크") || lowerName.contains("헬스케어") ||
                lowerName.contains("스타트업") || lowerName.contains("마케팅") || lowerName.contains("seo")) {
            return Product.Category.IT비즈니스;
        } else {
            // 기본 카테고리 또는 랜덤 카테고리 반환
            Product.Category[] categories = Product.Category.values();
            return categories[ThreadLocalRandom.current().nextInt(categories.length)];
        }
    }
}
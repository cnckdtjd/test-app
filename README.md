# Test App - 부하 테스트용 웹 애플리케이션

이 프로젝트는 3,000~10,000명의 동시 접속 사용자를 대상으로 하는 부하 테스트용 웹 애플리케이션입니다. Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, MariaDB 등을 사용하여 구현되었습니다.

## 기능

- 사용자 관리: 회원가입, 로그인, 프로필 관리
- 상품 관리: 상품 목록, 상세 정보, 검색, 카테고리별 필터링
- 장바구니: 상품 추가, 수량 변경, 삭제, 장바구니 비우기
- 주문 처리: 주문 생성, 결제 처리(모의), 주문 상태 관리
- 관리자 기능: 사용자/상품/주문 관리, 테스트 데이터 생성, 롤백

## 기술 스택

- **백엔드**: Spring Boot 3.4.3, Java 17
- **보안**: Spring Security
- **데이터베이스**: MariaDB, Spring Data JPA
- **프론트엔드**: Thymeleaf, Bootstrap 5, jQuery
- **빌드 도구**: Gradle

## 시작하기

### 사전 요구사항

- JDK 17 이상
- MariaDB 10.x
- Gradle 7.x 이상

### 설치 및 실행

1. 저장소 클론
   ```bash
   git clone https://github.com/yourusername/test-app.git
   cd test-app
   ```

2. MariaDB 데이터베이스 생성
   ```sql
   CREATE DATABASE testapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. `application.yml` 파일에서 데이터베이스 설정 확인/수정
   ```yaml
   spring:
     datasource:
       url: jdbc:mariadb://localhost:3306/testapp
       username: root
       password: password
   ```

4. 애플리케이션 빌드 및 실행
   ```bash
   ./gradlew bootRun
   ```

5. 웹 브라우저에서 접속
   ```
   http://localhost:8080
   ```

## 부하 테스트

### JMeter 스크립트

부하 테스트를 위한 JMeter 스크립트는 `jmeter` 디렉토리에 있습니다. 다음과 같은 시나리오를 포함합니다:

1. 로그인
2. 상품 목록 조회
3. 상품 상세 조회
4. 장바구니에 상품 추가
5. 주문 생성
6. 결제 처리
7. 로그아웃

### 테스트 데이터 생성

관리자 계정으로 로그인한 후 관리자 대시보드에서 테스트 데이터를 생성할 수 있습니다:

- 기본 관리자 계정: admin / admin
- 테스트 데이터 생성: `/admin` 페이지에서 "Generate Test Data" 버튼 클릭
- 데이터 롤백: `/admin` 페이지에서 "Rollback Data" 버튼 클릭

## 프로젝트 구조

```
src/main/java/com/jacob/testapp/
├── config/           # 설정 클래스
├── controller/       # 컨트롤러
├── domain/           # 엔티티 클래스
├── dto/              # 데이터 전송 객체
├── repository/       # 레포지토리 인터페이스
├── security/         # 보안 관련 클래스
├── service/          # 서비스 클래스
└── util/             # 유틸리티 클래스

src/main/resources/
├── static/           # 정적 리소스 (CSS, JS, 이미지)
├── templates/        # Thymeleaf 템플릿
└── application.yml   # 애플리케이션 설정
```

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 기여

이슈 및 풀 리퀘스트는 환영합니다. 주요 변경 사항의 경우, 먼저 이슈를 열어 변경하려는 내용을 논의해 주세요.

## 연락처

프로젝트 관리자: [이메일 주소](mailto:info@testapp.com) 
# 사용자 서비스 (User Service)

## 개요
- 이 서비스는 사용자 관리 기능을 제공하는 독립적인 마이크로서비스입니다.
- JPA 대신 MyBatis를 사용하여 구현되었습니다.

## 기능
- 사용자 등록 및 관리
- 인증 및 인가 (JWT 기반)
- 로그인 및 계정 관리
- 사용자 프로필 관리

## 기술 스택
- Java 17
- Spring Boot 3.4.3
- Spring Security
- MyBatis 3.0.3
- MariaDB
- JWT

## API 엔드포인트

### 인증 API
- `POST /api/auth/login`: 로그인
- `POST /api/auth/validate`: 토큰 검증

### 사용자 API
- `POST /api/users/register`: 회원가입
- `GET /api/users/me`: 현재 로그인한 사용자 정보 조회
- `PUT /api/users/me`: 현재 로그인한 사용자 정보 수정
- `GET /api/users/check-duplicate`: 사용자명/이메일 중복 확인
- `GET /api/users/{id}`: 특정 ID의 사용자 조회 (관리자 전용)
- `GET /api/users`: 모든 사용자 목록 조회 (관리자 전용)
- `DELETE /api/users/{id}`: 사용자 삭제 (관리자 전용)
- `POST /api/users/{username}/unlock`: 계정 잠금 해제 (관리자 전용)

## 실행 방법

### 로컬 개발 환경
1. MariaDB 설치 및 실행
```bash
docker run -d -p 3306:3306 --name mariadb -e MYSQL_ROOT_PASSWORD=password mariadb
```

2. 데이터베이스 생성
```bash
mysql -h localhost -u root -p < src/main/resources/schema.sql
```

3. 애플리케이션 실행
```bash
./gradlew bootRun
```

4. API 접근
```
http://localhost:8081/user-service/api/...
```

## 보안 고려사항
- JWT 토큰 기반 인증
- 비밀번호 BCrypt 암호화
- 계정 잠금 기능
- 로그인 시도 제한

## 기타 정보
- 포트: 8081
- 컨텍스트 패스: /user-service 
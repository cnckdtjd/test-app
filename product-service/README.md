# 상품 서비스 (Product Service)

## 개요
- 이 서비스는 상품 관리 기능을 제공하는 독립적인 마이크로서비스입니다.
- JPA 대신 MyBatis를 사용하여 구현되었습니다.
- 캐싱 기능을 통해 성능을 최적화했습니다.

## 기능
- 상품 목록 조회 및 검색
- 카테고리별 상품 조회
- 상품 상세 정보 조회
- 상품 재고 관리
- 관리자용 상품 관리

## 기술 스택
- Java 17
- Spring Boot 3.4.3
- Spring Security
- MyBatis 3.0.3
- MariaDB
- Caffeine Cache

## API 엔드포인트

### 상품 API
- `GET /api/products`: 상품 목록 조회
- `GET /api/products/{id}`: 상품 상세 조회
- `GET /api/products/latest`: 최근 상품 목록 조회
- `GET /api/products/category/{category}`: 카테고리별 상품 조회
- `GET /api/products/categories`: 카테고리 목록 조회
- `POST /api/products/{id}/decrease-stock`: 재고 감소 (다른 서비스에서 호출)
- `POST /api/products/{id}/increase-stock`: 재고 증가 (다른 서비스에서 호출)

### 관리자 API
- `GET /api/admin/products`: 모든 상품 목록 조회
- `GET /api/admin/products/low-stock`: 재고 부족 상품 수 조회
- `GET /api/admin/products/category-stats`: 카테고리별 상품 수 통계
- `PUT /api/admin/products/category-stock`: 카테고리별 재고 일괄 업데이트
- `DELETE /api/admin/products/range`: 상품 ID 범위로 삭제
- `POST /api/products`: 상품 생성
- `PUT /api/products/{id}`: 상품 수정
- `DELETE /api/products/{id}`: 상품 삭제

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
http://localhost:8082/product-service/api/products
```

## 성능 최적화
- 상품 조회 시 캐싱 적용 (@Cacheable)
- 재고 관리 시 낙관적 락 사용
- CRUD 작업 최적화를 위한 인덱스 설정

## 기타 정보
- 포트: 8082
- 컨텍스트 패스: /product-service 
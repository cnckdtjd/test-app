CREATE DATABASE IF NOT EXISTS product_service_db;
USE product_service_db;

-- 상품 테이블
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    category VARCHAR(100),
    image_url VARCHAR(255),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);

-- 샘플 데이터 삽입 (10개 상품)
INSERT INTO products (name, description, price, stock, image_url, category, status, remarks, created_at, updated_at, version)
VALUES
    ('자바 프로그래밍 기초', '자바 프로그래밍 언어의 기본 개념과 문법을 학습할 수 있는 강의입니다.', 19.99, 100, 'java-basic.jpg', '프로그래밍', 'ACTIVE', '베스트셀러', NOW(), NOW(), 0),
    ('스프링 부트 입문', '스프링 부트를 이용한 웹 애플리케이션 개발의 기초를 다룹니다.', 24.99, 50, 'spring-boot.jpg', '프로그래밍', 'ACTIVE', '인기 강의', NOW(), NOW(), 0),
    ('MySQL 데이터베이스 설계', '관계형 데이터베이스 설계 원칙과 MySQL 구현 방법을 배웁니다.', 29.99, 75, 'mysql-design.jpg', '데이터베이스', 'ACTIVE', '', NOW(), NOW(), 0),
    ('네트워크 보안 기초', '네트워크 보안의 핵심 개념과 방법론을 학습합니다.', 34.99, 30, 'network-security.jpg', '네트워크_보안', 'ACTIVE', '', NOW(), NOW(), 0),
    ('파이썬을 이용한 데이터 분석', '파이썬 언어로 데이터 분석의 기초를 배웁니다.', 27.99, 85, 'python-data.jpg', '인공지능_데이터과학', 'ACTIVE', '추천 강의', NOW(), NOW(), 0),
    ('도커와 쿠버네티스 입문', '컨테이너화 및 오케스트레이션의 기본 개념을 배웁니다.', 39.99, 40, 'docker-kubernetes.jpg', 'DevOps', 'ACTIVE', '', NOW(), NOW(), 0),
    ('리액트 프론트엔드 개발', '리액트를 이용한 모던 프론트엔드 개발 방법을 배웁니다.', 22.99, 65, 'react-frontend.jpg', 'UI_UX', 'ACTIVE', '', NOW(), NOW(), 0),
    ('안드로이드 앱 개발 기초', '안드로이드 스튜디오를 이용한 모바일 앱 개발의 기초를 다룹니다.', 19.99, 55, 'android-app.jpg', '모바일_개발', 'ACTIVE', '', NOW(), NOW(), 0),
    ('IoT 기기 프로그래밍', '라즈베리 파이와 아두이노를 이용한 IoT 기기 프로그래밍 방법을 배웁니다.', 44.99, 25, 'iot-programming.jpg', 'IoT_혁신기술', 'ACTIVE', '', NOW(), NOW(), 0),
    ('클라우드 컴퓨팅 기초', 'AWS, Azure, Google Cloud의 기본 서비스와 아키텍처를 학습합니다.', 32.99, 70, 'cloud-computing.jpg', 'IT비즈니스', 'ACTIVE', '', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE updated_at = NOW();
CREATE DATABASE IF NOT EXISTS user_service_db;
USE user_service_db;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(200),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    login_attempts INT NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    lock_time DATETIME,
    cash_balance BIGINT DEFAULT 0,
    remarks VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_login_at DATETIME
);

-- 초기 관리자 계정 생성 (비밀번호: admin)
INSERT INTO users (
    username, password, name, email, phone, address, role, enabled, 
    login_attempts, account_locked, cash_balance, remarks, 
    created_at, updated_at, status, last_login_at
) VALUES (
    'admin', 
    '$2a$10$QaCvWJQkNqc6tLUBGQXYve1dj1d5vl5.W8Sb1XlMDx0XC6uIGMzZu', 
    '관리자', 
    'admin@example.com', 
    '010-1234-5678', 
    '서울시 강남구', 
    'ROLE_ADMIN', 
    true, 
    0, 
    false, 
    0, 
    '관리자 계정', 
    NOW(), 
    NOW(), 
    'ACTIVE', 
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW(); 
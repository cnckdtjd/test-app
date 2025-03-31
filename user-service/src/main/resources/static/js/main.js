// 공통 JavaScript 함수

// 문서가 로드되면 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('문서가 로드되었습니다.');
    initializeMenu();
});

// 모바일 메뉴 토글 함수
function initializeMenu() {
    const menuToggle = document.querySelector('.menu-toggle');
    if (menuToggle) {
        menuToggle.addEventListener('click', function() {
            const navMenu = document.querySelector('.nav-menu');
            navMenu.classList.toggle('active');
        });
    }
}

// 폼 유효성 검사 함수
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return true;

    let isValid = true;
    const requiredFields = form.querySelectorAll('[required]');
    
    requiredFields.forEach(field => {
        if (!field.value.trim()) {
            isValid = false;
            showFieldError(field, '이 필드는 필수입니다.');
        } else {
            clearFieldError(field);
        }
        
        // 이메일 형식 검사
        if (field.type === 'email' && field.value.trim()) {
            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(field.value)) {
                isValid = false;
                showFieldError(field, '유효한 이메일 주소를 입력하세요.');
            }
        }
        
        // 비밀번호 길이 검사
        if (field.name === 'password' && field.value.trim()) {
            if (field.value.length < 8) {
                isValid = false;
                showFieldError(field, '비밀번호는 최소 8자 이상이어야 합니다.');
            }
        }
    });
    
    return isValid;
}

// 필드 오류 표시 함수
function showFieldError(field, message) {
    clearFieldError(field);
    
    field.classList.add('is-invalid');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    field.parentNode.appendChild(errorDiv);
}

// 필드 오류 제거 함수
function clearFieldError(field) {
    field.classList.remove('is-invalid');
    const errorMessage = field.parentNode.querySelector('.error-message');
    if (errorMessage) {
        errorMessage.remove();
    }
}

// 알림 메시지 표시 함수
function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) return;
    
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    
    // 닫기 버튼 추가
    const closeButton = document.createElement('button');
    closeButton.className = 'close-alert';
    closeButton.innerHTML = '&times;';
    closeButton.addEventListener('click', function() {
        alertDiv.remove();
    });
    
    alertDiv.appendChild(closeButton);
    alertContainer.appendChild(alertDiv);
    
    // 5초 후 자동으로 사라짐
    setTimeout(function() {
        alertDiv.remove();
    }, 5000);
}

// AJAX 요청 함수
async function fetchData(url, options = {}) {
    try {
        const response = await fetch(url, options);
        
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('데이터 가져오기 오류:', error);
        showAlert(error.message || '데이터를 가져오는데 실패했습니다.', 'danger');
        throw error;
    }
}

// 토큰 관리 함수
function getToken() {
    return localStorage.getItem('auth_token');
}

function setToken(token) {
    localStorage.setItem('auth_token', token);
}

function removeToken() {
    localStorage.removeItem('auth_token');
} 
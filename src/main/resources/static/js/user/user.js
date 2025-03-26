/**
 * 유저 대시보드에서 공통으로 사용되는 자바스크립트 함수
 */

// 페이지 로드 시 각종 이벤트와 기능 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 새로고침 버튼 이벤트
    document.querySelectorAll('.btn-refresh').forEach(btn => {
        btn.addEventListener('click', function() {
            const target = this.getAttribute('data-target');
            if (target) {
                window.location.href = target;
            } else {
                window.location.reload();
            }
        });
    });
    
    // 날짜 범위 필터 변경 시 자동 제출
    const dateRangeSelector = document.getElementById('dateRange');
    if (dateRangeSelector) {
        dateRangeSelector.addEventListener('change', function() {
            this.closest('form').submit();
        });
    }
    
    // 서버 측 알림 자동 숨김
    setTimeout(function() {
        document.querySelectorAll('.alert-dismissible').forEach(alert => {
            alert.classList.add('fade-out');
            setTimeout(() => {
                alert.remove();
            }, 500);
        });
    }, 5000);
    
    // 테이블 정렬 헤더 기능
    document.querySelectorAll('th[data-sort]').forEach(header => {
        header.addEventListener('click', function() {
            const sort = this.getAttribute('data-sort');
            const order = this.getAttribute('data-order') === 'asc' ? 'desc' : 'asc';
            
            // URL 파라미터 수정
            const url = new URL(window.location.href);
            url.searchParams.set('sort', sort);
            url.searchParams.set('order', order);
            window.location.href = url.toString();
        });
    });
    
    // CSRF 토큰 AJAX 요청에 추가
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    if (csrfToken && csrfHeader) {
        document.addEventListener('ajax:beforeSend', function(e) {
            e.detail.xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    }
    
    // 부트스트랩 모달 초기화
    document.querySelectorAll('.modal').forEach(modal => {
        new bootstrap.Modal(modal);
    });

});

// 확인 다이얼로그를 표시하고 사용자가 확인하면 지정된 URL로 이동하거나 폼을 제출
function confirmAction(message, targetUrl, submitForm = false) {
    if (confirm(message)) {
        if (submitForm && targetUrl) {
            document.getElementById(targetUrl)?.submit();
        } else if (targetUrl) {
            window.location.href = targetUrl;
        }
    }
}

// 숫자 형식화 유틸리티
function formatNumber(value) {
    return new Intl.NumberFormat('ko-KR').format(value);
}

// 날짜 형식화 유틸리티
function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

// 페이지네이션 생성 (현재 페이지, 전체 페이지)
function createPagination(currentPage, totalPages) {
    const pagination = document.createElement('ul');
    pagination.className = 'pagination justify-content-center';
    
    // 이전 페이지 버튼
    const prevPageItem = document.createElement('li');
    prevPageItem.className = `page-item ${currentPage <= 1 ? 'disabled' : ''}`;
    const prevPageLink = document.createElement('a');
    prevPageLink.className = 'page-link';
    prevPageLink.href = currentPage > 1 ? `?page=${currentPage - 1}` : '#';
    prevPageLink.innerHTML = '&laquo;';
    prevPageItem.appendChild(prevPageLink);
    pagination.appendChild(prevPageItem);
    
    // 페이지 번호
    for (let i = 1; i <= totalPages; i++) {
        const pageItem = document.createElement('li');
        pageItem.className = `page-item ${i === currentPage ? 'active' : ''}`;
        const pageLink = document.createElement('a');
        pageLink.className = 'page-link';
        pageLink.href = `?page=${i}`;
        pageLink.textContent = i;
        pageItem.appendChild(pageLink);
        pagination.appendChild(pageItem);
    }
    
    // 다음 페이지 버튼
    const nextPageItem = document.createElement('li');
    nextPageItem.className = `page-item ${currentPage >= totalPages ? 'disabled' : ''}`;
    const nextPageLink = document.createElement('a');
    nextPageLink.className = 'page-link';
    nextPageLink.href = currentPage < totalPages ? `?page=${currentPage + 1}` : '#';
    nextPageLink.innerHTML = '&raquo;';
    nextPageItem.appendChild(nextPageLink);
    pagination.appendChild(nextPageItem);
    
    return pagination;
}

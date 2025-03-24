/**
 * 관리자 대시보드에서 공통으로 사용되는 자바스크립트 함수
 */

// CPU 사용률 차트 초기화
function initCpuUsageChart() {
    const cpuChart = document.getElementById('cpuUsageChart');
    if (!cpuChart) return;
    
    const cpuUsageValue = document.getElementById('cpuUsageValue')?.innerText.replace('%', '') || '0';
    const cpuUsage = parseFloat(cpuUsageValue);
    
    new Chart(cpuChart, {
        type: 'doughnut',
        data: {
            labels: ['사용 중', '여유'],
            datasets: [{
                data: [cpuUsage, 100 - cpuUsage],
                backgroundColor: ['#4e73df', '#eaecf4'],
                hoverBackgroundColor: ['#2e59d9', '#dddfeb'],
                hoverBorderColor: "rgba(234, 236, 244, 1)",
            }]
        },
        options: {
            maintainAspectRatio: false,
            cutout: '70%',
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });
}

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
    
    // 시스템 모니터링 차트 초기화
    initCpuUsageChart();
    
    // 테스트 데이터 생성 폼 제출
    const generateDataForm = document.getElementById('generateDataForm');
    if (generateDataForm) {
        generateDataForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const userCount = document.getElementById('userCount').value;
            const productCount = document.getElementById('productCount').value;
            
            fetch('/admin/test-data/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [csrfHeader]: csrfToken
                },
                body: `userCount=${userCount}&productCount=${productCount}`
            })
            .then(response => response.json())
            .then(data => {
                if (data.success === 'true') {
                    showAlert('success', data.message);
                } else {
                    showAlert('danger', data.message);
                }
            })
            .catch(error => {
                showAlert('danger', '데이터 생성 중 오류가 발생했습니다: ' + error);
            });
        });
    }
    
    // 테스트 데이터 롤백 버튼
    const rollbackDataBtn = document.getElementById('rollbackDataBtn');
    if (rollbackDataBtn) {
        rollbackDataBtn.addEventListener('click', function() {
            if (confirm('모든 테스트 데이터를 롤백하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
                fetch('/admin/test-data/rollback', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    }
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success === 'true') {
                        showAlert('success', data.message);
                    } else {
                        showAlert('danger', data.message);
                    }
                })
                .catch(error => {
                    showAlert('danger', '데이터 롤백 중 오류가 발생했습니다: ' + error);
                });
            }
        });
    }
});

// 사용자에게 알림 메시지 표시
function showAlert(type, message) {
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) {
        const container = document.createElement('div');
        container.id = 'alert-container';
        container.style.position = 'fixed';
        container.style.top = '20px';
        container.style.right = '20px';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }
    
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    document.getElementById('alert-container').appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.classList.add('fade-out');
        setTimeout(() => {
            alertDiv.remove();
        }, 500);
    }, 5000);
}

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

// 상품 삭제 확인 함수
function confirmDeleteProduct(button) {
    const productId = button.getAttribute('data-product-id');
    const productName = button.getAttribute('data-product-name');
    
    document.getElementById('deleteProductMessage').textContent = '상품 "' + productName + '"을(를) 정말 삭제하시겠습니까?';
    document.getElementById('deleteProductForm').action = '/admin/products/' + productId + '/delete';
    
    var deleteModal = new bootstrap.Modal(document.getElementById('deleteProductModal'));
    deleteModal.show();
} 
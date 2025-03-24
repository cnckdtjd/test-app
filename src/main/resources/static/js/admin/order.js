/**
 * 주문 관리에서 사용되는 JavaScript 함수들
 */

document.addEventListener('DOMContentLoaded', function() {
    // 새로고침 버튼 이벤트
    const refreshBtn = document.getElementById('refreshOrdersBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            window.location.reload();
        });
    }
    
    // 주문 내보내기 버튼 이벤트
    const exportBtn = document.getElementById('exportOrdersBtn');
    if (exportBtn) {
        exportBtn.addEventListener('click', function() {
            const startDate = document.getElementById('startDate')?.value || '';
            const endDate = document.getElementById('endDate')?.value || '';
            const status = document.getElementById('statusFilter')?.value || '';
            
            window.location.href = `/admin/orders/export?startDate=${startDate}&endDate=${endDate}&status=${status}`;
        });
    }
    
    // 배송 추적 링크 업데이트
    const trackingLinks = document.querySelectorAll('[data-tracking-company]');
    trackingLinks.forEach(link => {
        const company = link.getAttribute('data-tracking-company');
        const trackingNumber = link.getAttribute('data-tracking-number');
        
        if (company && trackingNumber) {
            let trackingUrl = '#';
            
            // 각 배송사별 배송 조회 URL 맵핑
            switch (company) {
                case 'CJ대한통운':
                    trackingUrl = `https://www.cjlogistics.com/ko/tool/parcel/tracking?gnbInvcNo=${trackingNumber}`;
                    break;
                case '롯데택배':
                    trackingUrl = `https://www.lotteglogis.com/home/reservation/tracking/linkView?InvNo=${trackingNumber}`;
                    break;
                case '우체국택배':
                    trackingUrl = `https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=${trackingNumber}`;
                    break;
                case '한진택배':
                    trackingUrl = `https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&schLang=KR&wblnumText=${trackingNumber}`;
                    break;
                case '로젠택배':
                    trackingUrl = `https://www.ilogen.com/m/personal/trace/${trackingNumber}`;
                    break;
            }
            
            link.href = trackingUrl;
        }
    });
    
    // 날짜 필터 초기화
    initDateFilters();
    
    // 주문 상태 변경 시 확인
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect) {
        const originalStatus = statusSelect.value;
        
        statusSelect.addEventListener('change', function() {
            const newStatus = this.value;
            
            // 주문 상태가 완료 또는 취소로 변경될 때 확인
            if ((newStatus === 'COMPLETED' || newStatus === 'CANCELLED') && 
                originalStatus !== 'COMPLETED' && originalStatus !== 'CANCELLED') {
                
                if (!confirm(`주문 상태를 '${getStatusDisplayName(newStatus)}'(으)로 변경하시겠습니까? 이 작업은 되돌릴 수 없습니다.`)) {
                    this.value = originalStatus;
                    return;
                }
            }
        });
    }
});

/**
 * 날짜 필터 초기화
 */
function initDateFilters() {
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    
    if (startDateInput && endDateInput) {
        // 날짜가 설정되어 있지 않으면 기본값 설정 (최근 30일)
        if (!startDateInput.value) {
            const thirtyDaysAgo = new Date();
            thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
            startDateInput.value = formatDate(thirtyDaysAgo);
        }
        
        if (!endDateInput.value) {
            const today = new Date();
            endDateInput.value = formatDate(today);
        }
        
        // 시작일이 종료일보다 이후일 수 없도록 제한
        startDateInput.addEventListener('change', function() {
            if (startDateInput.value > endDateInput.value) {
                endDateInput.value = startDateInput.value;
            }
        });
        
        // 종료일이 시작일보다 이전일 수 없도록 제한
        endDateInput.addEventListener('change', function() {
            if (endDateInput.value < startDateInput.value) {
                startDateInput.value = endDateInput.value;
            }
        });
    }
}

/**
 * 주문 상태 표시 이름 가져오기
 */
function getStatusDisplayName(status) {
    const statusMap = {
        'PENDING': '결제대기',
        'PAID': '결제완료',
        'SHIPPING': '배송중',
        'COMPLETED': '배송완료',
        'CANCELLED': '주문취소'
    };
    
    return statusMap[status] || status;
}

/**
 * 날짜 포맷 (YYYY-MM-DD)
 */
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    return `${year}-${month}-${day}`;
}

/**
 * 통화 포맷 (원)
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(amount);
} 
package com.jacob.testapp.order.service;

import com.jacob.testapp.cart.entity.Cart;
import com.jacob.testapp.cart.entity.CartItem;
import com.jacob.testapp.cart.service.CartService;
import com.jacob.testapp.order.entity.Order;
import com.jacob.testapp.order.entity.OrderItem;
import com.jacob.testapp.order.repository.OrderItemRepository;
import com.jacob.testapp.order.repository.OrderRepository;
import com.jacob.testapp.product.entity.Product;
import com.jacob.testapp.product.service.ProductService;
import com.jacob.testapp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final Random random = new Random();

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByIdWithItems(Long id) {
        return orderRepository.findByIdWithItems(id);
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public Optional<Order> findByOrderNumberWithItems(String orderNumber) {
        return orderRepository.findByOrderNumberWithItems(orderNumber);
    }

    public List<Order> findByUser(User user) {
        return orderRepository.findByUser(user);
    }

    public Page<Order> findByUser(User user, Pageable pageable) {
        return orderRepository.findByUser(user, pageable);
    }

    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Order createOrder(User user, String shippingAddress, String paymentMethod) {
        // 사용자의 장바구니 조회
        Cart cart = cartService.findByUserWithItems(user.getId())
                .orElseThrow(() -> new IllegalStateException("장바구니가 비어 있습니다"));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있습니다");
        }

        // 타임스탬프를 활용한 고유 주문번호 생성 (SQL 복잡성 증가를 위한 추가 처리)
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        
        // 랜덤 전화번호 생성 (010-XXXX-XXXX 형식)
        String randomPhone = generateRandomPhoneNumber();
        
        // 랜덤 주소 생성 (부하테스트용 복잡성 추가)
        String randomAddress = generateRandomAddress();
        String randomAddressDetail = "상세주소 " + (int)(Math.random() * 100);
        
        // 랜덤 이메일 생성
        String randomEmail = generateRandomEmail(user.getUsername());
        
        // 랜덤 우편번호 생성
        String randomZipcode = String.format("%05d", (int)(Math.random() * 100000));

        // 주문 생성
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(orderNumber);  // 고유 주문번호 설정
        order.setTotalAmount(cart.getTotalPrice());
        order.setSubtotalAmount(cart.getTotalPrice());
        order.setShippingAmount(0.0);
        order.setDiscountAmount(0.0);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        
        // 복잡성을 유지하기 위한 랜덤 데이터 사용
        order.setReceiverName(user.getName() != null ? user.getName() : "테스트 사용자" + (int)(Math.random() * 100));
        order.setReceiverPhone(user.getPhone() != null ? user.getPhone() : randomPhone);
        order.setReceiverAddress1(shippingAddress != null && !shippingAddress.equals("테스트 주문 (배송 없음)") 
                                ? shippingAddress : randomAddress);
        order.setReceiverAddress2(randomAddressDetail);
        order.setReceiverZipcode(randomZipcode);
        order.setPhoneNumber(user.getPhone() != null ? user.getPhone() : randomPhone);
        order.setEmail(user.getEmail() != null ? user.getEmail() : randomEmail);
        order.setDeliveryMessage(generateRandomDeliveryMessage());
        
        // SQL 부하 증가를 위한 추가 데이터
        order.setTrackingNumber(generateRandomTrackingNumber());
        order.setCarrier(generateRandomCarrier());
        order.setAdminMemo("시스템 자동 생성 데이터 #" + (int)(Math.random() * 10000));
        
        order = orderRepository.save(order);

        // 장바구니 아이템을 주문 아이템으로 변환
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            // 재고 확인 및 감소
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("상품 재고 부족: " + product.getName());
            }
            
            // 재고 감소
            productService.decreaseStock(product.getId(), cartItem.getQuantity());
            
            // 주문 아이템 생성
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice().doubleValue());
            
            order.addItem(orderItem);
        }

        // 장바구니 비우기
        cartService.clearCart(user);

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }
        
        User user = order.getUser();
        
        // 현금결제였던 경우 환불 처리
        if ("현금결제".equals(order.getPaymentMethod())) {
            user.setCashBalance(user.getCashBalance() + Math.round(order.getTotalAmount()));
        }
        
        // 장바구니에 상품 복원
        for (OrderItem item : order.getItems()) {
            // 재고 복원
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
            
            // 장바구니에 상품 추가 (CartService 메서드에 맞게 수정)
            cartService.addProductToCart(user, item.getProduct().getId(), item.getQuantity());
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    /**
     * 특정 사용자의 최근 주문 내역을 조회합니다.
     * @param user 조회할 사용자
     * @param limit 최대 조회 개수
     * @return 최근 주문 목록
     */
    public List<Order> getRecentOrdersByUser(User user, int limit) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 결제 처리 (모의 구현)
    @Transactional
    public Order processPayment(Long orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // 실제로는 외부 결제 API 연동 로직이 들어갈 자리
        
        order.setStatus(Order.OrderStatus.PAID);
        return orderRepository.save(order);
    }

    // 사용자 보유 현금으로 결제 처리
    @Transactional
    public Order processPaymentWithCash(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
        
        // 주문이 현재 사용자의 것인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근이 거부되었습니다");
        }
        
        // 주문 상태 확인 (결제 대기 상태여야 함)
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("이미 결제 처리된 주문입니다");
        }
        
        // 현금 잔액 확인
        if (user.getCashBalance() < order.getTotalAmount()) {
            throw new IllegalStateException("현금 잔액이 부족합니다");
        }
        
        // 현금 잔액 차감
        user.setCashBalance(user.getCashBalance() - Math.round(order.getTotalAmount()));
        
        // 주문 상태 변경
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentMethod("현금결제");
        
        return orderRepository.save(order);
    }

    /**
     * 랜덤 전화번호 생성 (010-XXXX-XXXX 형식)
     */
    private String generateRandomPhoneNumber() {
        return String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));
    }

    /**
     * 랜덤 배송지 주소 생성
     */
    private String generateRandomAddress() {
        String[] cities = {"서울특별시", "부산광역시", "인천광역시", "대구광역시", "대전광역시", "광주광역시", "울산광역시", "세종특별자치시", 
                         "경기도", "강원도", "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도"};
        String[] districts = {"중구", "동구", "서구", "남구", "북구", "강남구", "강서구", "강동구", "송파구", "마포구", "성동구", 
                            "성북구", "영등포구", "동작구", "서초구", "광진구", "용산구", "종로구", "은평구", "도봉구", "노원구"};
        String[] details = {"대로", "로", "길", "거리", "아파트", "빌딩", "오피스텔", "주택", "타워", "맨션", "리젠시", "파크", "푸르지오", "힐스테이트"};
        
        String randomCity = cities[random.nextInt(cities.length)];
        String randomDistrict = districts[random.nextInt(districts.length)];
        String randomDetail = details[random.nextInt(details.length)];
        int randomNumber = random.nextInt(100) + 1;
        
        // 경기도 등 도 단위는 시/군을 추가
        if (randomCity.endsWith("도")) {
            String[] cities2 = {"수원시", "성남시", "안양시", "안산시", "고양시", "용인시", "부천시", "의정부시", "화성시", "광명시", "평택시", "과천시"};
            randomCity = randomCity + " " + cities2[random.nextInt(cities2.length)];
        }
        
        return randomCity + " " + randomDistrict + " " + randomNumber + randomDetail + " " + 
               (random.nextInt(100) + 1) + "동 " + (random.nextInt(1000) + 1) + "호";
    }

    /**
     * 랜덤 이메일 생성
     */
    private String generateRandomEmail(String baseUsername) {
        String[] domains = {"gmail.com", "naver.com", "daum.net", "kakao.com", "outlook.com", "hotmail.com", "yahoo.com"};
        String randomDomain = domains[random.nextInt(domains.length)];
        return baseUsername.toLowerCase() + "_" + (random.nextInt(9000) + 1000) + "@" + randomDomain;
    }

    /**
     * 랜덤 택배사 생성
     */
    private String generateRandomCarrier() {
        String[] carriers = {"대한통운", "로젠택배", "한진택배", "롯데택배", "우체국택배", "CJ대한통운", "KGB택배", "합동택배", "일양로지스"};
        return carriers[random.nextInt(carriers.length)];
    }

    /**
     * 랜덤 송장번호 생성
     */
    private String generateRandomTrackingNumber() {
        return String.format("TRACK-%d-%d", System.currentTimeMillis(), random.nextInt(10000));
    }

    /**
     * 랜덤 배송 메시지 생성
     */
    private String generateRandomDeliveryMessage() {
        String[] messages = {
            "배송 전 연락 부탁드립니다.",
            "문 앞에 놓아주세요.",
            "경비실에 맡겨주세요.",
            "부재시 연락주세요.",
            "배송 시 파손되지 않도록 주의해주세요.",
            "안전하게 배송 부탁드립니다.",
            "빠른 배송 부탁드립니다.",
            "문 앞에 놓아주시고 택배함에 보관해주세요.",
            "문에 비밀번호 # 누르고 열어주세요.",
            "부재시 경비실에 맡겨주세요.",
            "테스트 배송입니다. (부하테스트용)"
        };
        return messages[random.nextInt(messages.length)] + " #" + (random.nextInt(10000));
    }

    /**
     * 주문을 삭제된 상태로 표시합니다. (이력은 남지만 사용자 화면에서는 보이지 않음)
     */
    @Transactional
    public Order markOrderAsDeleted(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));
        
        order.setStatus(Order.OrderStatus.DELETED);
        return orderRepository.save(order);
    }

    /**
     * 특정 사용자의 주문 목록을 조회합니다. (삭제된 주문 제외)
     */
    public Page<Order> findByUserExcludeDeleted(User user, Pageable pageable) {
        return orderRepository.findByUserAndStatusNot(user, Order.OrderStatus.DELETED, pageable);
    }

    /**
     * 특정 사용자의 최근 주문 내역을 조회합니다. (삭제된 주문 제외)
     */
    public List<Order> getRecentOrdersByUserExcludeDeleted(User user, int limit) {
        return orderRepository.findByUserAndStatusNotOrderByCreatedAtDesc(user, Order.OrderStatus.DELETED)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
} 
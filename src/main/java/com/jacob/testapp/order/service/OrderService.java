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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;

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

        // 주문 생성
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(cart.getTotalPrice());
        order.setSubtotalAmount(cart.getTotalPrice());
        order.setShippingAmount(0.0);
        order.setDiscountAmount(0.0);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setReceiverName(user.getName());
        order.setReceiverPhone(user.getPhone());
        order.setReceiverAddress1(shippingAddress);
        
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
        
        // 재고 복구
        for (OrderItem item : order.getItems()) {
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.deleteById(id);
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
} 
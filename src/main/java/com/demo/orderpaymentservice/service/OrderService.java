package com.demo.orderpaymentservice.service;

import com.demo.orderpaymentservice.domain.*;
import com.demo.orderpaymentservice.dto.request.OrderRequest;
import com.demo.orderpaymentservice.dto.request.PaymentRequest;
import com.demo.orderpaymentservice.dto.response.OrderItemResponse;
import com.demo.orderpaymentservice.dto.response.OrderResponse;
import com.demo.orderpaymentservice.dto.response.PaymentResponse;
import com.demo.orderpaymentservice.exception.ConflictException;
import com.demo.orderpaymentservice.exception.NotFoundException;
import com.demo.orderpaymentservice.exception.PaymentAmountMismatchException;
import com.demo.orderpaymentservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ArticleItemRepository articleItemRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, ArticleItemRepository articleItemRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.articleItemRepository = articleItemRepository;
        this.paymentRepository = paymentRepository;
    }

//    Per garantire atomicità.
//    Il pagamento e l'aggiornamento dello stato dell'ordine
//    devono avvenire nella stessa transazione.
//    Se il salvataggio del payment fallisce,
//    lo stato dell'ordine non deve diventare PAID.

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        Customer customer = customerRepository.findById(request.customerId()).orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;

        for (var itemReq : request.items()) {

            ArticleItem article = articleItemRepository.findByIdAndActiveTrue(itemReq.articleItemId())
                    .orElseThrow(() -> new NotFoundException("Article not found or inactive: " + itemReq.articleItemId()));

            OrderItem item = new OrderItem();
            item.setArticleItem(article);
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(article.getBasePrice()); // prezzo storicizzato al momento dell'ordineSe il prezzo dell'articolo cambia in futuro, l'ordine mantiene il prezzo originale.

            order.addItem(item);

            total = total.add(item.getLineTotal());
        }

        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    public List<OrderResponse> getOrders(Long customerId, OrderStatus status) {

        List<Order> orders;

        if (customerId != null && status != null) {

            orders = orderRepository.findByCustomerIdAndStatus(customerId, status);
        } else if (customerId != null) {

            orders = orderRepository.findByCustomerId(customerId);
        } else if (status != null) {

            orders = orderRepository.findByStatus(status);

        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse payOrder(Long orderId, PaymentRequest request) {

        Order order = orderRepository.findByIdWithDetails(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.isPaid()) {
            throw new ConflictException("Order is already paid: " + orderId);
        }

        if (order.isCanceled()) {
            throw new ConflictException("Cannot pay a canceled order: " + orderId);
        }

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new ConflictException("Order already has a payment: " + orderId);
        }

        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentAmountMismatchException(order.getTotalAmount(), request.amount());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPayment(payment);

        order.setStatus(OrderStatus.PAID);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.isPaid()) {
            throw new ConflictException("Cannot cancel a paid order: " + orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Order is already canceled: " + orderId);
        }

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new ConflictException("Cannot cancel an order with an existing payment: " + orderId);
        }

        order.setStatus(OrderStatus.CANCELED);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = order.getItems().stream().map(item -> new OrderItemResponse(item.getId(), item.getArticleItem().getId(), item.getArticleItem().getName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal())).toList();

        PaymentResponse paymentResponse = null;

        if (order.getPayment() != null) {
            Payment p = order.getPayment();
            paymentResponse = new PaymentResponse(p.getId(), p.getAmount(), p.getMethod(), p.getPaidAt());
        }

        return new OrderResponse(order.getId(), order.getCustomer().getId(), order.getCustomer().getName(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), items, paymentResponse);
    }
}

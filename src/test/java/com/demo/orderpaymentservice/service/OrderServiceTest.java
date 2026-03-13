package com.demo.orderpaymentservice.service;

import com.demo.orderpaymentservice.domain.*;
import com.demo.orderpaymentservice.dto.request.OrderItemRequest;
import com.demo.orderpaymentservice.dto.request.OrderRequest;
import com.demo.orderpaymentservice.dto.request.PaymentRequest;
import com.demo.orderpaymentservice.dto.response.OrderResponse;
import com.demo.orderpaymentservice.exception.ConflictException;
import com.demo.orderpaymentservice.exception.NotFoundException;
import com.demo.orderpaymentservice.exception.PaymentAmountMismatchException;
import com.demo.orderpaymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ArticleItemRepository articleItemRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OrderService orderService;

    private Order createdOrder;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setName("Mario Rossi");
        customer.setEmail("mario@example.com");

        ArticleItem article = new ArticleItem();
        article.setName("Laptop");
        article.setBasePrice(new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setArticleItem(article);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));

        createdOrder = new Order();
        createdOrder.setCustomer(customer);
        createdOrder.setStatus(OrderStatus.CREATED);
        createdOrder.setTotalAmount(new BigDecimal("100.00"));
        createdOrder.setCreatedAt(LocalDateTime.now());
        createdOrder.getItems().add(item);
    }

    // --- payOrder ---

    @Test
    void payOrder_success() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.payOrder(1L,
                new PaymentRequest(new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD));

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void payOrder_alreadyPaid_throwsConflict() {
        createdOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.payOrder(1L,
                new PaymentRequest(new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already paid");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void payOrder_amountMismatch_throwsException() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.payOrder(1L,
                new PaymentRequest(new BigDecimal("50.00"), PaymentMethod.CASH)))
                .isInstanceOf(PaymentAmountMismatchException.class)
                .hasMessageContaining("100.00")
                .hasMessageContaining("50.00");
    }

    @Test
    void payOrder_duplicatePaymentInDb_throwsConflict() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> orderService.payOrder(1L,
                new PaymentRequest(new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD)))
                .isInstanceOf(ConflictException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void payOrder_canceledOrder_throwsConflict() {
        createdOrder.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.payOrder(1L,
                new PaymentRequest(new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("canceled");
    }

    // --- cancelOrder ---

    @Test
    void cancelOrder_success() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        OrderResponse response = orderService.cancelOrder(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void cancelOrder_alreadyPaid_throwsConflict() {
        createdOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("paid");
    }

    @Test
    void cancelOrder_withExistingPayment_throwsConflict() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(createdOrder));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("payment");
    }

    // --- createOrder ---

    @Test
    void createOrder_customerNotFound_throwsNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(
                new OrderRequest(99L, List.of(new OrderItemRequest(1L, 1)))))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void createOrder_inactiveArticle_throwsNotFound() {
        Customer customer = new Customer();
        customer.setName("Test");
        customer.setEmail("test@test.com");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(articleItemRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(
                new OrderRequest(1L, List.of(new OrderItemRequest(99L, 1)))))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("inactive");
    }
}
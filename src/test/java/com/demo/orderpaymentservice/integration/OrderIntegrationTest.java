package com.demo.orderpaymentservice.integration;

import com.demo.orderpaymentservice.domain.OrderStatus;
import com.demo.orderpaymentservice.domain.PaymentMethod;
import com.demo.orderpaymentservice.dto.request.CustomerRequest;
import com.demo.orderpaymentservice.dto.request.OrderItemRequest;
import com.demo.orderpaymentservice.dto.request.OrderRequest;
import com.demo.orderpaymentservice.dto.request.PaymentRequest;
import com.demo.orderpaymentservice.dto.response.CustomerResponse;
import com.demo.orderpaymentservice.dto.response.OrderResponse;
import com.demo.orderpaymentservice.exception.ConflictException;
import com.demo.orderpaymentservice.exception.PaymentAmountMismatchException;
import com.demo.orderpaymentservice.repository.PaymentRepository;
import com.demo.orderpaymentservice.service.CustomerService;
import com.demo.orderpaymentservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderIntegrationTest {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentRepository paymentRepository;

    // IDs articoli seed: 1=Laptop 1299.99, 2=Mouse 29.99, 3=Hub 49.99, 4=Inactive

    @Test
    void fullFlow_createOrderAndPay() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Mario Rossi", "mario@test.com"));

        OrderResponse order = orderService.createOrder(new OrderRequest(
                customer.id(),
                List.of(new OrderItemRequest(1L, 1), new OrderItemRequest(2L, 2))
        ));

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.totalAmount()).isEqualByComparingTo("1359.97");
        assertThat(order.payment()).isNull();

        OrderResponse paid = orderService.payOrder(order.id(),
                new PaymentRequest(new BigDecimal("1359.97"), PaymentMethod.CREDIT_CARD));

        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.payment()).isNotNull();
        assertThat(paid.payment().amount()).isEqualByComparingTo("1359.97");
    }

    @Test
    void payOrder_wrongAmount_orderRemainsCreated() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Luigi Verdi", "luigi@test.com"));

        OrderResponse order = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(1L, 1))
        ));

        long paymentsBefore = paymentRepository.count();

        assertThatThrownBy(() -> orderService.payOrder(order.id(),
                new PaymentRequest(new BigDecimal("999.00"), PaymentMethod.CASH)))
                .isInstanceOf(PaymentAmountMismatchException.class);

        assertThat(paymentRepository.count()).isEqualTo(paymentsBefore);
        assertThat(orderService.getOrder(order.id()).status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void payOrder_twice_throwsConflict() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Anna Blu", "anna@test.com"));

        OrderResponse order = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(2L, 1))
        ));
        orderService.payOrder(order.id(),
                new PaymentRequest(new BigDecimal("29.99"), PaymentMethod.BANK_TRANSFER));

        assertThatThrownBy(() -> orderService.payOrder(order.id(),
                new PaymentRequest(new BigDecimal("29.99"), PaymentMethod.BANK_TRANSFER)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelOrder_success() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Carlo Neri", "carlo@test.com"));

        OrderResponse order = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(3L, 2))
        ));

        assertThat(orderService.cancelOrder(order.id()).status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void cancelOrder_afterPay_throwsConflict() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Giulia Rosa", "giulia@test.com"));

        OrderResponse order = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(2L, 1))
        ));
        orderService.payOrder(order.id(),
                new PaymentRequest(new BigDecimal("29.99"), PaymentMethod.CREDIT_CARD));

        assertThatThrownBy(() -> orderService.cancelOrder(order.id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getOrders_filterByCustomerAndStatus() {
        CustomerResponse customer = customerService.createCustomer(
                new CustomerRequest("Filippo Giallo", "filippo@test.com"));

        OrderResponse order1 = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(1L, 1))
        ));
        OrderResponse order2 = orderService.createOrder(new OrderRequest(
                customer.id(), List.of(new OrderItemRequest(2L, 1))
        ));

        orderService.payOrder(order1.id(),
                new PaymentRequest(new BigDecimal("1299.99"), PaymentMethod.CREDIT_CARD));

        assertThat(orderService.getOrders(customer.id(), OrderStatus.PAID)).hasSize(1);
        assertThat(orderService.getOrders(customer.id(), OrderStatus.CREATED)).hasSize(1);
        assertThat(orderService.getOrders(customer.id(), null)).hasSize(2);
    }

    @Test
    void createCustomer_duplicateEmail_throwsConflict() {
        customerService.createCustomer(new CustomerRequest("Alice", "alice@test.com"));

        assertThatThrownBy(() ->
                customerService.createCustomer(new CustomerRequest("Alice2", "alice@test.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice@test.com");
    }
}
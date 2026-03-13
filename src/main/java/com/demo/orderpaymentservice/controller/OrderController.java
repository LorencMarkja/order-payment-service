package com.demo.orderpaymentservice.controller;

import com.demo.orderpaymentservice.domain.OrderStatus;
import com.demo.orderpaymentservice.dto.request.OrderRequest;
import com.demo.orderpaymentservice.dto.request.PaymentRequest;
import com.demo.orderpaymentservice.dto.response.OrderResponse;
import com.demo.orderpaymentservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@RequestParam(required = false) Long customerId,
                                                         @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(customerId, status));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> payOrder(@PathVariable Long id,
                                                  @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(orderService.payOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
package com.demo.orderpaymentservice.dto.response;

import com.demo.orderpaymentservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        PaymentResponse payment      // null se non ancora pagato
) {
}
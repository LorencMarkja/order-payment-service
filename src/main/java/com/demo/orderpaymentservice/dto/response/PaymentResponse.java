package com.demo.orderpaymentservice.dto.response;

import com.demo.orderpaymentservice.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime paidAt
) {
}
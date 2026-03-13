package com.demo.orderpaymentservice.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long articleItemId,
        String articleName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
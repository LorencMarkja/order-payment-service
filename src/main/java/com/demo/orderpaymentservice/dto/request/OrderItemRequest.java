package com.demo.orderpaymentservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long articleItemId,
        @NotNull @Min(1) Integer quantity
) {
}
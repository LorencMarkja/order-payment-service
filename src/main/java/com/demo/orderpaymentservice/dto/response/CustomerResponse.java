package com.demo.orderpaymentservice.dto.response;

public record CustomerResponse(
        Long id,
        String name,
        String email
) {
}
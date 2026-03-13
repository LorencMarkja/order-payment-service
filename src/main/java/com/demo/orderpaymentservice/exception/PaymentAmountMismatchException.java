package com.demo.orderpaymentservice.exception;

import java.math.BigDecimal;

public class PaymentAmountMismatchException extends RuntimeException {

    public PaymentAmountMismatchException(BigDecimal expected, BigDecimal actual) {
        super("Payment amount mismatch: expected %s but received %s".formatted(expected, actual));
    }
}
package com.banking.paymentservice.service;

import com.banking.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor

public class PaymentService {

    private final PaymentRepository paymentRepository;

    private static final String PAYMENT_COMPLETED = "Payment.Completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    //4:31:17
}

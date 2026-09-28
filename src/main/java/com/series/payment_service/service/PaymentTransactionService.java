package com.series.payment_service.service;

import org.springframework.stereotype.Service;

import com.series.payment_service.entity.Payment;
import com.series.payment_service.repositories.PaymentRepo;

import jakarta.transaction.Transactional;

@Service
public class PaymentTransactionService {

    private final PaymentRepo paymentRepo;

    public PaymentTransactionService(PaymentRepo paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    @Transactional
    public Payment savePayment(Payment payment) {
        return paymentRepo.save(payment);
    }
}
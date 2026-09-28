package com.series.payment_service.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.series.payment_service.Constants.PaymentStatus;
import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;
import com.series.payment_service.entity.Payment;
import com.series.payment_service.repositories.PaymentRepo;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentRepo paymentRepo;

    public PaymentServiceImpl(
            PaymentTransactionService paymentTransactionService,
            PaymentRepo paymentRepo) {
        this.paymentTransactionService = paymentTransactionService;
        this.paymentRepo = paymentRepo;
    }

    /*
     * ============================================================
     * V1 / V2 — Basic Payment Processing
     * ============================================================
     *
     * Simple implementation with no idempotency protection.
     *
     * V2 exposed the duplicate-payment problem when multiple
     * concurrent requests hit the API.
     */

    // @Override
    // public PaymentResponse processPayment(PaymentRequest request)
    //         throws InterruptedException {
    //
    //     Payment payment = requestMapper(request);
    //
    //     Thread.sleep(1000);
    //
    //     payment.setStatus(PaymentStatus.SUCCESS);
    //
    //     PaymentResponse response =
    //             responseMapper(paymentRepo.save(payment));
    //
    //     return response;
    // }


    /*
     * ============================================================
     * V3 — Naive Duplicate Check
     * ============================================================
     *
     * Tried to prevent duplicates using userId + amount.
     *
     * Problem:
     * CHECK -> SAVE is not atomic.
     *
     * Multiple concurrent requests can all see "not found"
     * and then create multiple payments.
     *
     * Also, userId + amount does not uniquely identify
     * a logical payment operation.
     */

    // @Override
    // public PaymentResponse processPayment(PaymentRequest request)
    //         throws InterruptedException {
    //
    //     if (!paymentRepo.existsByUserIdAndAmount(
    //             request.getUserId(),
    //             request.getAmount())) {
    //
    //         Payment payment = requestMapper(request);
    //
    //         Thread.sleep(1000);
    //
    //         payment.setStatus(PaymentStatus.SUCCESS);
    //
    //         PaymentResponse response =
    //                 responseMapper(paymentRepo.save(payment));
    //
    //         return response;
    //
    //     } else {
    //
    //         PaymentResponse response = new PaymentResponse();
    //         response.setMessage("Payment Failed");
    //         response.setStatus(PaymentStatus.FAILED);
    //
    //         return response;
    //     }
    // }


    /*
     * ============================================================
     * V4 / V5 / V6 / V7 — Idempotent Payment Processing
     * ============================================================
     *
     * V4:
     * Introduced client-provided Idempotency-Key.
     *
     * V5:
     * Added database UNIQUE constraint on idempotency_key.
     *
     * V6:
     * Investigated transaction boundaries using @Transactional.
     *
     * V7:
     * Moved the transaction boundary into
     * PaymentTransactionService so duplicate transaction
     * failures do not affect the transaction used to fetch
     * the existing payment.
     */

    @Override
    public PaymentResponse processPayment(
            PaymentRequest request,
            String key) {

        Payment payment = requestMapper(request);
        payment.setIdempotencyKey(key);
        payment.setStatus(PaymentStatus.SUCCESS);

        try {

            Payment savedPayment =
                    paymentTransactionService.savePayment(payment);

            return responseMapper(savedPayment);

        } catch (DataIntegrityViolationException e) {

        	Optional<Payment> existingPayment =
        	        paymentRepo.findByIdempotencyKey(key);

        	if (existingPayment.isPresent()) {

        	    Payment existing = existingPayment.get();

        	    if (!existing.getUserId().equals(request.getUserId())
        	            || existing.getAmount().compareTo(request.getAmount()) != 0) {

        	        PaymentResponse response = new PaymentResponse();
        	        response.setMessage(
        	                "Idempotency key already used for a different payment");
        	        response.setStatus(PaymentStatus.FAILED);

        	        return response;
        	    }

        	    return responseMapper(existing);
        	}else {
        	    PaymentResponse response = new PaymentResponse();
        	    response.setMessage("Payment result could not be retrieved");
        	    response.setStatus(PaymentStatus.FAILED);
        	    return response;
        	}
        }
    }


    private PaymentResponse responseMapper(Payment savedPayment) {

        PaymentResponse response = new PaymentResponse();

        response.setMessage("Payment Successful");
        response.setPaymentReference(
                savedPayment.getPaymentReference());
        response.setStatus(savedPayment.getStatus());

        return response;
    }


    private Payment requestMapper(PaymentRequest request) {

        Payment payment = new Payment();

        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaymentReference(UUID.randomUUID().toString());
        payment.setStatus(PaymentStatus.INITIATED);

        return payment;
    }
}
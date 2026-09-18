package com.series.payment_service.service;

import org.springframework.stereotype.Service;

import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;

@Service
public interface PaymentService {
	
	PaymentResponse processPayment(PaymentRequest request) throws InterruptedException;

}

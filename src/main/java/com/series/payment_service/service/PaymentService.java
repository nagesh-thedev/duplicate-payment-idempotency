package com.series.payment_service.service;

import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;

public interface PaymentService {
	
	PaymentResponse processPayment(PaymentRequest request) throws InterruptedException;

}

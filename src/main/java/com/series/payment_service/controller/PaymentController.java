package com.series.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;
import com.series.payment_service.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {
	
	private PaymentService paymentService;
	
	
	
	public PaymentController(PaymentService paymentService) {
		super();
		this.paymentService = paymentService;
	}


     @PostMapping
	 public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) throws InterruptedException{
		PaymentResponse response = paymentService.processPayment(request);
		return ResponseEntity.ok(response);
	}

}

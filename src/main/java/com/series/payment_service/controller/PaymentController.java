package com.series.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.series.payment_service.Constants.PaymentStatus;
import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;
import com.series.payment_service.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payment")
public class PaymentController {
	
	private PaymentService paymentService;
	
	
	
	public PaymentController(PaymentService paymentService) {
		super();
		this.paymentService = paymentService;
	}


     @PostMapping
	 public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request,
			 @RequestHeader("Idempotency-Key") String idempotencyKey) throws InterruptedException{
    	 if (idempotencyKey == null || idempotencyKey.isBlank()) {
    	        PaymentResponse response = new PaymentResponse();
    	        response.setMessage("Idempotency-Key is required");
    	        response.setStatus(PaymentStatus.FAILED);

    	        return ResponseEntity.badRequest().body(response);
    	    }
		PaymentResponse response = paymentService.processPayment(request,idempotencyKey);
		return ResponseEntity.ok(response);
	}

}

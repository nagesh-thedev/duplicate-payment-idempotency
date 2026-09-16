package com.series.payment_service.dto;

import com.series.payment_service.Constants.PaymentStatus;

import lombok.Data;

@Data
public class PaymentResponse {
  
	
	private String message;
	private String paymentReference;
	private PaymentStatus status;
	
}

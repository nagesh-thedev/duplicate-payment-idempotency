package com.series.payment_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.series.payment_service.Constants.PaymentStatus;
import com.series.payment_service.dto.PaymentRequest;
import com.series.payment_service.dto.PaymentResponse;
import com.series.payment_service.entity.Payment;
import com.series.payment_service.repositories.PaymentRepo;

@Service
public class PaymentServiceImpl implements PaymentService {
     
	private PaymentRepo paymentRepo;
	
	
	
	public PaymentServiceImpl(PaymentRepo paymentRepo) {
		super();
		this.paymentRepo = paymentRepo;
	}



	@Override
	public PaymentResponse processPayment(PaymentRequest request) throws InterruptedException {
		Payment payment = requestMapper(request);
		
			Thread.sleep(1000);
			payment.setStatus(PaymentStatus.SUCCESS);
			PaymentResponse response = responseMapper(paymentRepo.save(payment));
			return response;
	
	}



	private PaymentResponse responseMapper(Payment savedPayment) {
		PaymentResponse pay = new PaymentResponse();
		pay.setMessage("Payment Successfull");
		pay.setPaymentReference(savedPayment.getPaymentReference());
		pay.setStatus(savedPayment.getStatus());
		return pay;
	}



	private Payment requestMapper(PaymentRequest request) {
		Payment pay = new Payment();
		pay.setUserId(request.getUserId());
		pay.setAmount(request.getAmount());
		pay.setCreatedAt(LocalDateTime.now());
		pay.setPaymentReference(UUID.randomUUID().toString());
		pay.setStatus(PaymentStatus.INITIATED);
		return pay;
	}

}

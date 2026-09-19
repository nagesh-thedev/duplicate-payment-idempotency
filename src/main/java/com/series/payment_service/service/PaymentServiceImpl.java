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
     
	private PaymentRepo paymentRepo;
	
	
	
	public PaymentServiceImpl(PaymentRepo paymentRepo) {
		super();
		this.paymentRepo = paymentRepo;
	}
// V1
//	@Override
//	public PaymentResponse processPayment(PaymentRequest request) throws InterruptedException {
//			Payment payment = requestMapper(request);
//			Thread.sleep(1000);
//			payment.setStatus(PaymentStatus.SUCCESS);
//			PaymentResponse response = responseMapper(paymentRepo.save(payment));
//			return response;
//	}
// V2
//	@Override
//	public PaymentResponse processPayment(PaymentRequest request) throws InterruptedException {
//		if(!paymentRepo.existsByUserIdAndAmount(request.getUserId(),request.getAmount())) {
//			Payment payment = requestMapper(request);
//			Thread.sleep(1000);
//			payment.setStatus(PaymentStatus.SUCCESS);
//			PaymentResponse response = responseMapper(paymentRepo.save(payment));
//			return response;
//		}else {
//			PaymentResponse response = new PaymentResponse();
//			response.setMessage("PaymentFailed");
//			response.setStatus(PaymentStatus.FAILED);
//			return response;
//		}
//		
//	
//	}
    
	
	@Override
	public PaymentResponse processPayment(PaymentRequest request,String key) throws InterruptedException {
		if(!paymentRepo.existsByIdempotencyKey(key)) {
			Payment payment = requestMapper(request);
			payment.setIdempotencyKey(key);
			Thread.sleep(1000);
			payment.setStatus(PaymentStatus.SUCCESS);
			try {
			Payment savedpayment = paymentRepo.save(payment);
			PaymentResponse response = responseMapper(savedpayment);
			return response;
			}catch(DataIntegrityViolationException e) {
				Optional<Payment> savedpayment = paymentRepo.findByIdempotencyKey(key);
				if(savedpayment.isPresent()) {
					PaymentResponse response = responseMapper(savedpayment.get());
					return response;
				}else {
					PaymentResponse response = new PaymentResponse();
					response.setMessage("Payment result could not be retrieved");
					response.setStatus(PaymentStatus.FAILED);
					return response;
				}
			}
		}else {
				PaymentResponse response = new PaymentResponse();
				response.setMessage("PaymentFailed");
				response.setStatus(PaymentStatus.FAILED);
				return response;
		}
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

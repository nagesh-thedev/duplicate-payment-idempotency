package com.series.payment_service.repositories;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.series.payment_service.entity.Payment;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
     
	boolean existsByUserIdAndAmount(long userId,BigDecimal amount);
	
	boolean existsByIdempotencyKey(String key);
	
	Optional<Payment> findByIdempotencyKey(String key);
	 
}

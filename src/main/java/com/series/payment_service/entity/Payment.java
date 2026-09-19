package com.series.payment_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import com.series.payment_service.Constants.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Payment {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	private String paymentReference;
	
	private Long userId;
	
	private BigDecimal amount;
	
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;
	
	@Column(unique = true)
	private String idempotencyKey;
	
	private LocalDateTime createdAt;

}

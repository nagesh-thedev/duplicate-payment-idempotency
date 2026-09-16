package com.series.payment_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequest {
	
	@NotNull
	private Long userId;
	
	@NotNull
	@Positive
	private BigDecimal amount;
	

}

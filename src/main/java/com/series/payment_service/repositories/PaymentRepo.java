package com.series.payment_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.series.payment_service.entity.Payment;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

}

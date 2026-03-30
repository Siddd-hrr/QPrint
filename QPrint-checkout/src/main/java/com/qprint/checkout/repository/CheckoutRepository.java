package com.qprint.checkout.repository;

import com.qprint.checkout.model.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutRepository extends JpaRepository<Checkout, UUID> {
    Optional<Checkout> findByRazorpayOrderId(String razorpayOrderId);
}

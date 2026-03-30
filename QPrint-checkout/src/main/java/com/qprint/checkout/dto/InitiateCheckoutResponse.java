package com.qprint.checkout.dto;

public record InitiateCheckoutResponse(
        CheckoutSummaryDto checkout,
        String razorpayKeyId
) {
}

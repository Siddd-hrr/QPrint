package com.qprint.orders.controller;

import com.qprint.orders.dto.OrderResponse;
import com.qprint.orders.dto.UpdateOrderStatusRequest;
import com.qprint.orders.model.ApiResponse;
import com.qprint.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @Value("${orders.internal.secret:change-me}")
    private String internalSecret;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatusInternal(
            @RequestHeader("X-Internal-Secret") String secret,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        if (!secretMatches(secret)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("Forbidden"));
        }
        OrderResponse order = orderService.updateStatus(orderId, request.status());
        return ResponseEntity.ok(ApiResponse.ok(order, "Order status updated"));
    }

    private boolean secretMatches(String provided) {
        if (internalSecret == null || "change-me".equals(internalSecret)) {
            return false;
        }
        return internalSecret.equals(provided);
    }
}

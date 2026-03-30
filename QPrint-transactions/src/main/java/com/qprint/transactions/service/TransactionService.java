package com.qprint.transactions.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.transactions.dto.CompletedOrderEvent;
import com.qprint.transactions.dto.PageResponse;
import com.qprint.transactions.dto.TransactionResponse;
import com.qprint.transactions.model.Transaction;
import com.qprint.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleOrderCompleted(CompletedOrderEvent event) {
        UUID orderId = parseUuid(event.orderId(), "orderId");
        UUID userId = parseUuid(event.userId(), "userId");
        if (orderId == null || userId == null) {
            log.warn("Skipping transaction persist due to missing ids: orderId={}, userId={}", event.orderId(), event.userId());
            return;
        }

        if (transactionRepository.findByOrderId(orderId).isPresent()) {
            log.info("Transaction already exists for orderId={}, skipping", orderId);
            return;
        }

        BigDecimal amount = event.totalAmount() != null ? event.totalAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        String itemsJson = writeItems(event.items());
        Instant completedAt = event.completedAt() != null ? event.completedAt() : Instant.now();

        Transaction transaction = Transaction.builder()
                .orderId(orderId)
                .userId(userId)
                .shopId(event.shopId())
                .itemsJson(itemsJson)
                .totalAmount(amount)
                .razorpayPaymentId(event.razorpayPaymentId())
                .completedAt(completedAt)
                .build();

        transactionRepository.save(Objects.requireNonNull(transaction, "transaction"));
        log.info("Saved transaction for orderId={} userId={}", orderId, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, int page, int size) {
        UUID safeUserId = Objects.requireNonNull(userId, "userId");
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Transaction> pageData = transactionRepository.findByUserIdOrderByCompletedAtDesc(safeUserId, pageable);
        List<TransactionResponse> content = pageData.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, pageData.getTotalPages(), pageData.getTotalElements(), pageData.getNumber());
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id, UUID userId) {
        UUID safeId = Objects.requireNonNull(id, "transactionId");
        UUID safeUserId = Objects.requireNonNull(userId, "userId");
        Transaction tx = transactionRepository.findById(safeId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!safeUserId.equals(tx.getUserId())) {
            throw new IllegalArgumentException("Transaction not found");
        }
        return toResponse(tx);
    }

    private TransactionResponse toResponse(Transaction tx) {
        JsonNode items = parseItems(tx.getItemsJson());
        return new TransactionResponse(
                tx.getId(),
                tx.getOrderId(),
                tx.getShopId(),
                tx.getTotalAmount(),
                tx.getRazorpayPaymentId(),
                tx.getCompletedAt(),
                tx.getCreatedAt(),
                items
        );
    }

    private String writeItems(JsonNode itemsNode) {
        try {
            if (itemsNode == null || itemsNode.isNull()) {
                return "[]";
            }
            return objectMapper.writeValueAsString(itemsNode);
        } catch (Exception ex) {
            log.warn("Failed to serialize items JSON, storing empty array", ex);
            return "[]";
        }
    }

    private JsonNode parseItems(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.warn("Failed to parse items JSON for response", ex);
            return objectMapper.createArrayNode();
        }
    }

    private UUID parseUuid(String value, String field) {
        try {
            return value != null ? UUID.fromString(value.trim()) : null;
        } catch (Exception ex) {
            log.warn("Invalid UUID for {}: {}", field, value);
            return null;
        }
    }
}

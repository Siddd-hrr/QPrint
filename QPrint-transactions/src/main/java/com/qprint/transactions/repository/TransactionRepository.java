package com.qprint.transactions.repository;

import com.qprint.transactions.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);
    Optional<Transaction> findByOrderId(UUID orderId);
}

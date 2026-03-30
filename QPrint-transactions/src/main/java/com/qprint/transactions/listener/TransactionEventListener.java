package com.qprint.transactions.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.transactions.dto.CompletedOrderEvent;
import com.qprint.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.completed", groupId = "${spring.kafka.consumer.group-id:QPrint-transactions}")
    public void handleOrderCompleted(String payload) {
        try {
            CompletedOrderEvent event = objectMapper.readValue(payload, CompletedOrderEvent.class);
            transactionService.handleOrderCompleted(event);
        } catch (Exception ex) {
            log.error("Failed to process order.completed event: {}", payload, ex);
        }
    }
}

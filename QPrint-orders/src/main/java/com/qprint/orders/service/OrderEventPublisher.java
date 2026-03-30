package com.qprint.orders.service;

import com.qprint.orders.event.OrderCompletedEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${orders.topic.completed:order.completed}")
    private String completedTopic;

    public void publishOrderCompleted(OrderCompletedEvent event) {
        try {
            String key = Objects.requireNonNull(event.orderId(), "orderId");
            kafkaTemplate.send(Objects.requireNonNull(completedTopic, "completedTopic"), key, event);
            log.info("Published order.completed for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("Failed to publish order.completed for orderId={}", event.orderId(), ex);
            throw ex;
        }
    }
}

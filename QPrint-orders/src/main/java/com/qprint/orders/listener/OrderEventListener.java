package com.qprint.orders.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.orders.event.OrderConfirmedEvent;
import com.qprint.orders.event.OrderStatusUpdatedEvent;
import com.qprint.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "${orders.topic.confirmed:order.confirmed}", groupId = "${spring.kafka.consumer.group-id:QPrint-orders}")
    public void handleOrderConfirmed(String payload) {
        try {
            OrderConfirmedEvent event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
            orderService.createFromEvent(event);
        } catch (Exception ex) {
            log.error("Failed to process order.confirmed event: {}", payload, ex);
        }
    }

    @KafkaListener(topics = "${orders.topic.status-updated:order.status.updated}", groupId = "${spring.kafka.consumer.group-id:QPrint-orders}")
    public void handleStatusUpdated(String payload) {
        try {
            OrderStatusUpdatedEvent event = objectMapper.readValue(payload, OrderStatusUpdatedEvent.class);
            orderService.updateStatus(UUID.fromString(event.orderId()), event.newStatus());
        } catch (Exception ex) {
            log.error("Failed to process order.status.updated event: {}", payload, ex);
        }
    }
}

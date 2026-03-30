package com.qprint.auth.service;

import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Map<String, Object> payload) {
        try {
            kafkaTemplate.send(Objects.requireNonNull(topic, "topic"), Objects.requireNonNull(payload, "payload"));
        } catch (Exception ex) {
            log.warn("Kafka publish failed for topic {}", topic, ex);
        }
    }
}

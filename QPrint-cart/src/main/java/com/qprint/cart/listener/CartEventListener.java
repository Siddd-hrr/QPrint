package com.qprint.cart.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.qprint.cart.model.CartItemEntity;
import com.qprint.cart.repository.CartItemRepository;
import com.qprint.cart.repository.PrintObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {

    private static final String CART_PREFIX = "cart:";
    private static final Duration TTL = Duration.ofHours(24);

    private final CartItemRepository cartItemRepository;
    private final PrintObjectRepository printObjectRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.login", groupId = "${spring.kafka.consumer.group-id:QPrint-cart}")
    @SuppressWarnings("null")
    public void handleUserLogin(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            Object userIdRaw = data.get("userId");
            if (userIdRaw == null) {
                return;
            }
            UUID userId = UUID.fromString(userIdRaw.toString());
            List<CartItemEntity> items = cartItemRepository.findByUserId(userId);
            if (items.isEmpty()) {
                return;
            }

            String redisKey = Objects.requireNonNull(key(userId), "redisKey");
            HashOperations<String, String, String> ops = redisTemplate.opsForHash();
            for (CartItemEntity item : items) {
                printObjectRepository.findByIdAndUserId(item.getObjectId(), userId).ifPresent(obj -> {
                    if (obj.getId() == null) {
                        return;
                    }
                    CartEntry entry = new CartEntry(obj.getId(), obj.getCalculatedPrice(), item.getCreatedAt());
                    String objectKey = Objects.requireNonNull(obj.getId().toString(), "objectId");
                    ops.put(redisKey, objectKey, serialize(entry));
                });
            }
            Duration ttl = TTL;
            redisTemplate.expire(redisKey, Objects.requireNonNull(ttl, "ttl"));
        } catch (Exception ex) {
            log.error("Failed to process user.login event: {}", payload, ex);
        }
    }

    private String key(UUID userId) {
        return CART_PREFIX + userId;
    }

    private String serialize(CartEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize cart entry");
        }
    }

    private record CartEntry(UUID objectId, BigDecimal price, Instant addedAt) {}
}

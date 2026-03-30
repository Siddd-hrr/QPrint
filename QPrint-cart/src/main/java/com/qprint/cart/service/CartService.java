package com.qprint.cart.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.cart.dto.AddItemRequest;
import com.qprint.cart.dto.CartItemDto;
import com.qprint.cart.dto.CartSummaryDto;
import com.qprint.cart.model.CartItemEntity;
import com.qprint.cart.model.PrintObject;
import com.qprint.cart.repository.CartItemRepository;
import com.qprint.cart.repository.PrintObjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_PREFIX = "cart:";
    private static final Duration TTL = Duration.ofHours(24);

    private final PrintObjectRepository repository;
    private final CartItemRepository cartItemRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CartSummaryDto getCart(UUID userId) {
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        Map<String, String> entries = hashOps().entries(redisKey);
        List<CartItemDto> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (String value : entries.values()) {
            CartEntry entry = deserialize(value);
            PrintObject obj = repository.findByIdAndUserId(entry.objectId(), userId)
                    .orElse(null);
            if (obj == null) continue;
            BigDecimal price = obj.getCalculatedPrice();
            items.add(CartItemDto.from(obj, price, entry.addedAt()));
            total = total.add(price);
        }
        return new CartSummaryDto(items, items.size(), total);
    }

    @SuppressWarnings("null")
    @Transactional
    public CartSummaryDto addItem(UUID userId, AddItemRequest request) {
        PrintObject obj = repository.findByIdAndUserId(request.objectId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Print object not found"));

        UUID objectId = Objects.requireNonNull(obj.getId(), "objectId");

        cartItemRepository.findByUserIdAndObjectId(userId, obj.getId())
            .orElseGet(() -> Objects.requireNonNull(cartItemRepository.save(CartItemEntity.builder()
                .userId(userId)
                .objectId(objectId)
                .build()), "cart item"));

        CartEntry entry = new CartEntry(objectId, obj.getCalculatedPrice(), Instant.now());
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        hashOps().put(redisKey, objectId.toString(), serialize(entry));
        Duration ttl = TTL;
        redisTemplate.expire(redisKey, Objects.requireNonNull(ttl, "ttl"));
        return getCart(userId);
    }

    @Transactional
    public CartSummaryDto removeItem(UUID userId, UUID objectId) {
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        hashOps().delete(redisKey, objectId.toString());
        cartItemRepository.deleteByUserIdAndObjectId(userId, objectId);
        return getCart(userId);
    }

    @Transactional
    public void clear(UUID userId) {
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        redisTemplate.delete(redisKey);
        cartItemRepository.deleteByUserId(userId);
    }

    public int count(UUID userId) {
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        Long size = hashOps().size(redisKey);
        return size == null ? 0 : size.intValue();
    }

    private String key(UUID userId) {
        return CART_PREFIX + userId;
    }

    private HashOperations<String, String, String> hashOps() {
        return redisTemplate.opsForHash();
    }

    private String serialize(CartEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize cart entry");
        }
    }

    private CartEntry deserialize(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse cart entry");
        }
    }

    private record CartEntry(UUID objectId, BigDecimal price, Instant addedAt) {}
}

package com.qprint.orders.service;

import com.qprint.orders.dto.CreateOrderRequest;
import com.qprint.orders.dto.OrderItemDto;
import com.qprint.orders.dto.OrderResponse;
import com.qprint.orders.event.OrderCompletedEvent;
import com.qprint.orders.event.OrderConfirmedEvent;
import com.qprint.orders.model.Binding;
import com.qprint.orders.model.ColorMode;
import com.qprint.orders.model.Order;
import com.qprint.orders.model.OrderItem;
import com.qprint.orders.model.OrderStatus;
import com.qprint.orders.model.PaperSize;
import com.qprint.orders.model.Sides;
import com.qprint.orders.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${orders.otp.ttl-seconds:600}")
    private long otpTtlSeconds;

    @Value("${otp.service-url:http://qprint-otp:8086}")
    private String otpServiceUrl;

    @Value("${orders.active.ttl-hours:48}")
    private long activeTtlHours;

    private static final String ACTIVE_PREFIX = "order:active:";
    private static final String ACTIVE_SET_PREFIX = "user-orders:";

    @Transactional
    public OrderResponse create(UUID userId, CreateOrderRequest request) {
        Order existing = orderRepository.findFirstByCheckoutId(request.checkoutId());
        if (existing != null) {
            return toResponse(existing);
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order amount must be positive");
        }

        Order order = Order.builder()
                .userId(userId)
                .checkoutId(request.checkoutId())
                .paymentId(request.paymentId())
                .amount(request.amount())
                .currency(request.currency())
                .status(OrderStatus.PROCESSING)
                .build();

        List<OrderItem> items = request.items().stream()
                .map(payload -> OrderItem.builder()
                        .order(order)
                        .objectId(parseUuid(payload.objectId()))
                        .filename(payload.filename())
                        .price(payload.price())
                        .copies(payload.copies())
                        .pageCount(payload.pageCount())
                        .pageRange(payload.pageRange())
                        .colorMode(parseEnum(payload.colorMode(), ColorMode.class))
                        .sides(parseEnum(payload.sides(), Sides.class))
                        .paperSize(parseEnum(payload.paperSize(), PaperSize.class))
                        .binding(parseEnum(payload.binding(), Binding.class))
                        .build())
                .toList();

        order.setItems(items);
        Order saved = orderRepository.save(order);
        cacheActive(saved);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse createFromEvent(OrderConfirmedEvent event) {
        UUID userId = parseUuid(event.userId());
        if (userId == null) {
            throw new IllegalArgumentException("Invalid userId in event");
        }

        String checkoutId = StringUtils.hasText(event.checkoutId()) ? event.checkoutId() : event.orderId();
        CreateOrderRequest request = new CreateOrderRequest(
                checkoutId,
                event.razorpayPaymentId(),
                event.totalAmount(),
                StringUtils.hasText(event.currency()) ? event.currency() : "INR",
                event.items() == null ? List.of() : event.items().stream()
                        .map(item -> new CreateOrderRequest.OrderItemPayload(
                                item.filename(),
                                item.price(),
                                item.copies(),
                                item.pageCount(),
                                item.pageRange(),
                                item.colorMode(),
                                item.sides(),
                                item.paperSize(),
                                item.binding(),
                                item.objectId()
                        ))
                        .toList()
        );

        return create(userId, request);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<OrderResponse> listActive(UUID userId) {
        UUID safeUserId = requireUserId(userId);
        String activeSetKey = activeSetKey(safeUserId);
        Set<String> activeIds = stringRedisTemplate.opsForSet().members(Objects.requireNonNull(activeSetKey, "activeSetKey"));
        if (activeIds != null && !activeIds.isEmpty()) {
            List<OrderResponse> cached = activeIds.stream()
                    .map(this::readCachedActive)
                    .filter(value -> value != null)
                    .sorted((a, b) -> {
                        if (a.createdAt() == null && b.createdAt() == null) return 0;
                        if (a.createdAt() == null) return 1;
                        if (b.createdAt() == null) return -1;
                        return b.createdAt().compareTo(a.createdAt());
                    })
                    .collect(Collectors.toList());
            if (!cached.isEmpty()) {
                return cached;
            }
        }

        List<OrderStatus> exclude = List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        List<Order> orders = orderRepository.findByUserIdAndStatusNotInOrderByCreatedAtDesc(safeUserId, exclude);
        for (Order order : orders) {
            cacheActive(order);
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId, UUID userId) {
        UUID safeOrderId = Objects.requireNonNull(orderId, "orderId");
        UUID safeUserId = requireUserId(userId);
        Order order = orderRepository.findById(safeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!safeUserId.equals(order.getUserId())) {
            throw new IllegalArgumentException("Order not found");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse getActive(UUID orderId, UUID userId) {
        UUID safeOrderId = Objects.requireNonNull(orderId, "orderId");
        UUID safeUserId = requireUserId(userId);
        String activeSetKey = activeSetKey(safeUserId);
        Object memberKey = Objects.requireNonNull(safeOrderId.toString(), "orderId");
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(Objects.requireNonNull(activeSetKey, "activeSetKey"), memberKey);
        if (Boolean.TRUE.equals(isMember)) {
            OrderResponse cached = readCachedActive(safeOrderId.toString());
            if (cached != null) {
                return cached;
            }
        }

        Order order = orderRepository.findById(safeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!safeUserId.equals(order.getUserId())) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order not active");
        }
        cacheActive(order);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, String statusValue) {
        UUID safeOrderId = Objects.requireNonNull(orderId, "orderId");
        String safeStatusValue = Objects.requireNonNull(statusValue, "statusValue");
        Order order = orderRepository.findById(safeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        String normalized = normalizeStatusValue(safeStatusValue);
        OrderStatus newStatus = parseEnum(normalized, OrderStatus.class);
        if (newStatus == null) {
            throw new IllegalArgumentException("Invalid status");
        }

        if (newStatus == order.getStatus()) {
            return toResponse(order);
        }

        if (!isTransitionAllowed(order.getStatus(), newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition");
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.READY) {
            ensureOtp(order);
        } else {
            clearOtp(order);
        }
        Order saved = orderRepository.save(order);

        if (newStatus == OrderStatus.COMPLETED) {
            publishCompletion(saved);
            removeActive(saved);
        } else if (newStatus == OrderStatus.CANCELLED) {
            removeActive(saved);
        } else {
            cacheActive(saved);
        }

        return toResponse(saved);
    }

    private void ensureOtp(Order order) {
        if (order.getStatus() != OrderStatus.READY) {
            return;
        }

        String key = otpKey(order.getId());
        String cached = stringRedisTemplate.opsForValue().get(Objects.requireNonNull(key, "otpKey"));
        if (cached != null) {
            order.setOtp(cached);
            return;
        }

        String otp = requestOtp(order);
        if (!StringUtils.hasText(otp)) {
            otp = generateOtp();
        }
        order.setOtp(otp);
        Duration otpTtl = Duration.ofSeconds(otpTtlSeconds);
        stringRedisTemplate.opsForValue().set(Objects.requireNonNull(key, "otpKey"), Objects.requireNonNull(otp, "otp"), Objects.requireNonNull(otpTtl, "otpTtl"));
        orderRepository.save(order);
    }

    private void clearOtp(Order order) {
        String key = otpKey(order.getId());
        stringRedisTemplate.delete(Objects.requireNonNull(key, "otpKey"));
        order.setOtp(null);
    }

    private String requestOtp(Order order) {
        if (!StringUtils.hasText(otpServiceUrl)) {
            return null;
        }
        try {
            Map<String, String> body = Map.of(
                    "orderId", order.getId().toString(),
                    "userId", order.getUserId().toString()
            );
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    otpServiceUrl + "/internal/otp/generate",
                    Objects.requireNonNull(HttpMethod.POST, "httpMethod"),
                    new HttpEntity<>(Objects.requireNonNull(body, "body")),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return null;
            }
            Object data = responseBody.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object otp = dataMap.get("otp");
                return otp != null ? otp.toString() : null;
            }
        } catch (Exception ex) {
            log.warn("OTP service generate failed for orderId={}: {}", order.getId(), ex.getMessage());
        }
        return null;
    }

    private void cacheActive(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            removeActive(order);
            return;
        }
        if (order.getStatus() == OrderStatus.READY) {
            ensureOtp(order);
        }
        OrderResponse response = toResponse(order);
        try {
            String json = objectMapper.writeValueAsString(response);
            String key = activeKey(order.getId());
            String userKey = activeSetKey(order.getUserId());
            Duration ttl = Duration.ofHours(activeTtlHours);
            stringRedisTemplate.opsForValue().set(Objects.requireNonNull(key, "activeKey"), Objects.requireNonNull(json, "json"), Objects.requireNonNull(ttl, "ttl"));
            stringRedisTemplate.opsForSet().add(Objects.requireNonNull(userKey, "activeSetKey"), order.getId().toString());
            stringRedisTemplate.expire(Objects.requireNonNull(userKey, "activeSetKey"), Objects.requireNonNull(ttl, "ttl"));
        } catch (Exception ex) {
            log.warn("Failed to cache active orderId={}", order.getId(), ex);
        }
    }

    private void removeActive(Order order) {
        String key = activeKey(order.getId());
        String userKey = activeSetKey(order.getUserId());
        stringRedisTemplate.delete(Objects.requireNonNull(key, "activeKey"));
        stringRedisTemplate.opsForSet().remove(Objects.requireNonNull(userKey, "activeSetKey"), order.getId().toString());
    }

    private OrderResponse readCachedActive(String orderId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(Objects.requireNonNull(activeKey(UUID.fromString(orderId)), "activeKey"));
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, OrderResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String activeKey(UUID orderId) {
        return ACTIVE_PREFIX + Objects.requireNonNull(orderId, "orderId");
    }

    private String activeSetKey(UUID userId) {
        return ACTIVE_SET_PREFIX + Objects.requireNonNull(userId, "userId");
    }

    private UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId");
    }

    private boolean isTransitionAllowed(OrderStatus current, OrderStatus target) {
        Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
                OrderStatus.PENDING, List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
                OrderStatus.PROCESSING, List.of(OrderStatus.READY, OrderStatus.CANCELLED),
                OrderStatus.READY, List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
                OrderStatus.COMPLETED, List.of(),
                OrderStatus.CANCELLED, List.of()
        );
        return allowed.getOrDefault(current, List.of()).contains(target);
    }

    private String otpKey(UUID orderId) {
        return "order:otp:" + orderId;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(OrderItemDto::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCheckoutId(),
                order.getPaymentId(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getFailureReason(),
                order.getOtp(),
                order.getCreatedAt(),
                items
        );
    }

    private void publishCompletion(Order order) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent(
                    order.getId().toString(),
                    order.getUserId() != null ? order.getUserId().toString() : null,
                    "SHOP_001",
                    order.getItems() != null ? order.getItems().stream().map(OrderItemDto::from).toList() : List.<OrderItemDto>of(),
                    order.getAmount(),
                    order.getPaymentId(),
                    order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()
            );
            eventPublisher.publishOrderCompleted(event);
        } catch (Exception ex) {
            log.error("Failed to publish order completion for orderId={}", order.getId(), ex);
        }
    }

    private String generateOtp() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.format("%06d", code);
    }

    private UUID parseUuid(String value) {
        try {
            return StringUtils.hasText(value) ? UUID.fromString(value.trim()) : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
        try {
            return StringUtils.hasText(value) ? Enum.valueOf(enumType, value.trim().toUpperCase()) : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeStatusValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim().toUpperCase();
        if ("RECEIVED".equals(normalized)) {
            return OrderStatus.PENDING.name();
        }
        if ("IN_PRODUCTION".equals(normalized)) {
            return OrderStatus.PROCESSING.name();
        }
        return normalized;
    }
}

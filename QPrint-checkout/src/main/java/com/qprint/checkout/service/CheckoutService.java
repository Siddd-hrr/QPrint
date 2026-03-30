package com.qprint.checkout.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qprint.checkout.dto.CheckoutItemDto;
import com.qprint.checkout.dto.CheckoutSummaryDto;
import com.qprint.checkout.dto.InitiateCheckoutResponse;
import com.qprint.checkout.event.OrderConfirmedEvent;
import com.qprint.checkout.event.OrderConfirmedItem;
import com.qprint.checkout.model.Checkout;
import com.qprint.checkout.model.CheckoutItem;
import com.qprint.checkout.model.PaymentStatus;
import com.qprint.checkout.model.PrintObject;
import com.qprint.checkout.repository.CheckoutRepository;
import com.qprint.checkout.repository.PrintObjectRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private static final String CART_PREFIX = "cart:";

    private final PrintObjectRepository printObjectRepository;
    private final CheckoutRepository checkoutRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @Value("${orders.service-url:http://qprint-orders:8085}")
    private String ordersServiceUrl;

    @Value("${orders.dispatch.max-attempts:3}")
    private int dispatchMaxAttempts;

    @Value("${orders.dispatch.backoff-millis:500}")
    private long dispatchBackoffMillis;

    @Value("${orders.topic.confirmed:order.confirmed}")
    private String confirmedTopic;

    @Transactional
    public InitiateCheckoutResponse initiate(UUID userId) {
        String redisKey = Objects.requireNonNull(key(userId), "redisKey");
        Map<String, String> entries = hashOps().entries(redisKey);
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<CartEntry> cartEntries = entries.values().stream()
                .map(this::deserialize)
                .toList();

        List<UUID> objectIds = cartEntries.stream().map(CartEntry::objectId).toList();
        Iterable<UUID> safeObjectIds = Objects.requireNonNull(objectIds, "objectIds");
        List<PrintObject> objects = printObjectRepository.findAllById(safeObjectIds).stream()
                .filter(obj -> userId.equals(obj.getUserId()))
                .toList();

        if (objects.isEmpty() || objects.size() != objectIds.size()) {
            throw new IllegalArgumentException("Some print objects are missing or unauthorized");
        }

        BigDecimal total = objects.stream()
                .map(PrintObject::getCalculatedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount is invalid");
        }

        Order order = createOrder(total, "INR");

        Checkout checkout = Checkout.builder()
                .userId(userId)
                .razorpayOrderId(order.get("id"))
                .amount(total)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();

        List<CheckoutItem> items = objects.stream()
                .map(obj -> CheckoutItem.builder()
                        .checkout(checkout)
                        .objectId(obj.getId())
                        .filename(obj.getOriginalFilename())
                        .price(obj.getCalculatedPrice())
                        .colorMode(obj.getColorMode())
                        .sides(obj.getSides())
                        .paperSize(obj.getPaperSize())
                        .binding(obj.getBinding())
                        .copies(obj.getCopies())
                        .pageCount(obj.getPageCount())
                        .pageRange(obj.getPageRange())
                        .build())
                .toList();

        checkout.setItems(new ArrayList<>(items));
        Checkout saved = checkoutRepository.save(checkout);

        return new InitiateCheckoutResponse(toSummary(saved), razorpayKeyId);
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException("Missing Razorpay signature");
        }

        verifySignature(payload, signature);
        JSONObject json = new JSONObject(payload);
        JSONObject payloadObj = json.optJSONObject("payload");
        if (payloadObj == null) {
            return;
        }

        JSONObject paymentWrapper = payloadObj.optJSONObject("payment");
        if (paymentWrapper == null) {
            return;
        }

        JSONObject payment = paymentWrapper.optJSONObject("entity");
        if (payment == null) {
            return;
        }

        String orderId = payment.optString("order_id");
        String paymentId = payment.optString("id");
        String status = payment.optString("status");
        String failureReason = payment.optString("error_description", "Payment failed");

        checkoutRepository.findByRazorpayOrderId(orderId).ifPresent(checkout -> {
            checkout.setRazorpayPaymentId(paymentId);
            checkout.setRazorpaySignature(signature);

            if ("captured".equalsIgnoreCase(status)) {
                checkout.setStatus(PaymentStatus.PAID);
                checkout.setFailureReason(null);
                try {
                    dispatchOrderCreation(checkout);
                    publishOrderConfirmed(checkout);
                } catch (Exception ignored) {
                    // If order creation call fails, we still keep payment status; retries can be added later.
                }
            } else if ("failed".equalsIgnoreCase(status)) {
                checkout.setStatus(PaymentStatus.FAILED);
                checkout.setFailureReason(failureReason);
            }

            checkoutRepository.save(checkout);
        });
    }

    @Transactional(readOnly = true)
    public CheckoutSummaryDto getCheckout(UUID checkoutId, UUID userId) {
        UUID safeCheckoutId = Objects.requireNonNull(checkoutId, "checkoutId");
        Checkout checkout = checkoutRepository.findById(safeCheckoutId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found"));

        if (!userId.equals(checkout.getUserId())) {
            throw new IllegalArgumentException("Checkout not found");
        }

        return toSummary(checkout);
    }

    @Transactional(readOnly = true)
    public CheckoutSummaryDto getCheckoutByRazorpayOrderId(String razorpayOrderId, UUID userId) {
        if (!StringUtils.hasText(razorpayOrderId)) {
            throw new IllegalArgumentException("Checkout not found");
        }
        Checkout checkout = checkoutRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found"));
        if (!userId.equals(checkout.getUserId())) {
            throw new IllegalArgumentException("Checkout not found");
        }
        return toSummary(checkout);
    }

    private void publishOrderConfirmed(Checkout checkout) {
        try {
            List<OrderConfirmedItem> items = checkout.getItems().stream()
                    .map(item -> new OrderConfirmedItem(
                            item.getFilename(),
                            item.getPrice(),
                            item.getCopies(),
                            item.getPageCount(),
                            item.getPageRange(),
                            item.getColorMode() != null ? item.getColorMode().name() : null,
                            item.getSides() != null ? item.getSides().name() : null,
                            item.getPaperSize() != null ? item.getPaperSize().name() : null,
                            item.getBinding() != null ? item.getBinding().name() : null,
                            item.getObjectId() != null ? item.getObjectId().toString() : null
                    ))
                    .toList();

            OrderConfirmedEvent event = new OrderConfirmedEvent(
                    checkout.getId() != null ? checkout.getId().toString() : null,
                    checkout.getUserId() != null ? checkout.getUserId().toString() : null,
                    "SHOP_001",
                    items,
                    checkout.getAmount(),
                    checkout.getRazorpayPaymentId(),
                    Instant.now(),
                    checkout.getRazorpayOrderId(),
                    checkout.getCurrency()
            );

            String key = event.orderId() != null ? event.orderId() : checkout.getId().toString();
            kafkaTemplate.send(Objects.requireNonNull(confirmedTopic, "topic"), Objects.requireNonNull(key, "eventKey"), event);
            log.info("Published order.confirmed for checkoutId={}", checkout.getRazorpayOrderId());
        } catch (Exception ex) {
            log.error("Failed to publish order.confirmed for checkoutId={}", checkout.getRazorpayOrderId(), ex);
        }
    }

    private CheckoutSummaryDto toSummary(Checkout checkout) {
        List<CheckoutItemDto> items = checkout.getItems().stream()
                .map(CheckoutItemDto::from)
                .toList();

        return new CheckoutSummaryDto(
                checkout.getId(),
                checkout.getRazorpayOrderId(),
            checkout.getRazorpayPaymentId(),
                checkout.getAmount(),
                checkout.getCurrency(),
                checkout.getStatus().name(),
                items
        );
    }

    private void dispatchOrderCreation(Checkout checkout) {
        if (!StringUtils.hasText(ordersServiceUrl)) {
            return;
        }

        List<Map<String, Object>> itemPayloads = checkout.getItems().stream()
                .map(item -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("filename", item.getFilename());
                    map.put("price", item.getPrice());
                    map.put("copies", item.getCopies());
                    map.put("pageCount", item.getPageCount());
                    map.put("pageRange", item.getPageRange());
                    map.put("colorMode", item.getColorMode() != null ? item.getColorMode().name() : null);
                    map.put("sides", item.getSides() != null ? item.getSides().name() : null);
                    map.put("paperSize", item.getPaperSize() != null ? item.getPaperSize().name() : null);
                    map.put("binding", item.getBinding() != null ? item.getBinding().name() : null);
                    map.put("objectId", item.getObjectId() != null ? item.getObjectId().toString() : null);
                    return map;
                })
                .toList();

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("checkoutId", checkout.getRazorpayOrderId());
        request.put("paymentId", checkout.getRazorpayPaymentId());
        request.put("amount", checkout.getAmount());
        request.put("currency", checkout.getCurrency());
        request.put("items", itemPayloads);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-User-Id", checkout.getUserId().toString());
        org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(request, headers);

        String url = ordersServiceUrl + "/api/orders";
        int attempts = Math.max(dispatchMaxAttempts, 1);
        long backoff = Math.max(dispatchBackoffMillis, 100);

        for (int i = 1; i <= attempts; i++) {
            try {
                restTemplate.postForEntity(url, entity, String.class);
                return;
            } catch (Exception ex) {
                boolean lastAttempt = i == attempts;
                log.warn("Dispatch order creation attempt {} failed: {}", i, ex.getMessage());
                if (lastAttempt) {
                    log.error("Exhausted dispatch attempts for checkoutId={}", checkout.getRazorpayOrderId(), ex);
                    throw ex;
                }
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
                backoff = Math.min(backoff * 2, 5000);
            }
        }
    }

    private Order createOrder(BigDecimal total, String currency) {
        try {
            RazorpayClient client = razorpayClient();
            int amountPaise = total.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();

            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", currency);
            options.put("receipt", "checkout-" + UUID.randomUUID());
            options.put("payment_capture", 1);
            return client.orders.create(options);
        } catch (RazorpayException ex) {
            throw new IllegalStateException("Failed to create Razorpay order: " + ex.getMessage());
        }
    }

    private RazorpayClient razorpayClient() {
        if (!StringUtils.hasText(razorpayKeyId) || !StringUtils.hasText(razorpayKeySecret)) {
            throw new IllegalStateException("Razorpay keys are not configured");
        }
        try {
            return new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        } catch (RazorpayException ex) {
            throw new IllegalStateException("Failed to initialize Razorpay client");
        }
    }

    private void verifySignature(String payload, String signature) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new IllegalStateException("Webhook secret is not configured");
        }
        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
    }

    private String key(UUID userId) {
        return CART_PREFIX + userId;
    }

    private HashOperations<String, String, String> hashOps() {
        return redisTemplate.opsForHash();
    }

    private CartEntry deserialize(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse cart entry");
        }
    }

    private record CartEntry(UUID objectId, BigDecimal price, Instant addedAt) {
    }
}

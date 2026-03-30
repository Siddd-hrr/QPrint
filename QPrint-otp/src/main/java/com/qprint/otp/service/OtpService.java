package com.qprint.otp.service;

import com.qprint.otp.dto.OtpVerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.ttl-seconds:600}")
    private long ttlSeconds;

    public String generate(String orderId, String userId) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
        String key = Objects.requireNonNull(key(orderId), "otpKey");
        Map<String, String> payload = Map.of(
                "otp", otp,
                "userId", userId,
                "attempts", "0"
        );
        ops.putAll(key, Objects.requireNonNull(payload, "payload"));
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        stringRedisTemplate.expire(key, Objects.requireNonNull(ttl, "ttl"));
        return otp;
    }

    public OtpVerificationResult verify(String orderId, String otp) {
        HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
        String key = Objects.requireNonNull(key(orderId), "otpKey");
        Object storedOtp = ops.get(key, "otp");
        Object attemptsRaw = ops.get(key, "attempts");

        if (storedOtp == null) {
            return new OtpVerificationResult(false, null, "OTP expired or not found");
        }

        int attempts = parseAttempts(attemptsRaw);
        if (attempts >= MAX_ATTEMPTS) {
            stringRedisTemplate.delete(Objects.requireNonNull(key, "otpKey"));
            return new OtpVerificationResult(false, 0, "Too many wrong attempts. A new OTP has been sent.");
        }

        if (storedOtp.toString().equals(otp)) {
            return new OtpVerificationResult(true, MAX_ATTEMPTS - attempts, "OTP verified");
        }

        attempts += 1;
        Object attemptsValue = Objects.requireNonNull(Integer.toString(attempts), "attempts");
        ops.put(Objects.requireNonNull(key, "otpKey"), "attempts", attemptsValue);
        int remaining = Math.max(0, MAX_ATTEMPTS - attempts);
        return new OtpVerificationResult(false, remaining, "Invalid OTP");
    }

    private int parseAttempts(Object attemptsRaw) {
        try {
            return attemptsRaw == null ? 0 : Integer.parseInt(attemptsRaw.toString());
        } catch (Exception ex) {
            return 0;
        }
    }

    private String key(String orderId) {
        return OTP_PREFIX + orderId;
    }
}

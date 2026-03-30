package com.qprint.auth.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {
    private final SecureRandom random = new SecureRandom();

    public String sixDigit() {
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}

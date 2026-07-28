package com.infobeans.ibnextstep.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

@Component
public class OtpGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final long OTP_VALIDITY_MINUTES = 5;

    public String generate() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    public Instant expiryFromNow() {
        return Instant.now().plusSeconds(OTP_VALIDITY_MINUTES * 60);
    }

    public String generateTempPassword() {
        // Used for admin-created accounts; user must change on first login.
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

package com.blog.security.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class PasswordResetCodeSecurity {

    private final byte[] hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetCodeSecurity(@Value("${security.password-reset.hmac-key}") String hmacKey) {
        if (hmacKey == null || hmacKey.length() < 32) {
            throw new IllegalArgumentException("Password reset HMAC key must contain at least 32 characters");
        }
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    public String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public String digest(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload(email, code)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to protect password reset code", e);
        }
    }

    public boolean matches(String email, String code, String expectedDigest) {
        if (expectedDigest == null) {
            return false;
        }
        return MessageDigest.isEqual(
                digest(email, code).getBytes(StandardCharsets.US_ASCII),
                expectedDigest.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] payload(String email, String code) {
        String normalized = email.trim().toLowerCase(Locale.ROOT) + ":" + code;
        return normalized.getBytes(StandardCharsets.UTF_8);
    }
}

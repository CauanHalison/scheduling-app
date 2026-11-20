package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class MagicLinkRateLimiter {

    static final String IP_PREFIX = "ratelimit:magic-link:ip:";
    static final String EMAIL_PREFIX = "ratelimit:magic-link:email:";
    static final int MAX_PER_IP = 10;
    static final int MAX_PER_EMAIL = 3;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public void check(String normalizedEmail, String ip) {
        enforce(IP_PREFIX + (ip != null ? ip : "unknown"), MAX_PER_IP, "ip");
        enforce(EMAIL_PREFIX + sha256(normalizedEmail), MAX_PER_EMAIL, "email");
    }

    private void enforce(String key, int max, String scope) {
        Long count = redisTemplate.opsForValue().increment(key);

        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl < 0) {
            redisTemplate.expire(key, WINDOW);
            ttl = WINDOW.toSeconds();
        }

        if (count != null && count > max) {
            log.warn("Magic link rate limit exceeded: scope={}, attempts={}, retryAfterSeconds={}",
                    scope, count, ttl);
            throw new TooManyRequestsException(
                    "Muitas solicitações de link de acesso. Tente novamente mais tarde.", ttl);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}

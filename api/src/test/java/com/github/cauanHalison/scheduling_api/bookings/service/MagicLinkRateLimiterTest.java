package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagicLinkRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MagicLinkRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should allow first attempt and set expiration on both counters")
    void shouldAllowFirstAttemptAndSetExpiration() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(-1L);

        assertThatCode(() -> rateLimiter.check("user@example.com", "203.0.113.7"))
                .doesNotThrowAnyException();

        verify(redisTemplate, times(2)).expire(anyString(), eq(MagicLinkRateLimiter.WINDOW));
    }

    @Test
    @DisplayName("Should allow attempts exactly at the limit without resetting expiration")
    void shouldAllowAttemptsAtTheLimit() {
        when(valueOperations.increment(startsWith(MagicLinkRateLimiter.IP_PREFIX)))
                .thenReturn((long) MagicLinkRateLimiter.MAX_PER_IP);
        when(valueOperations.increment(startsWith(MagicLinkRateLimiter.EMAIL_PREFIX)))
                .thenReturn((long) MagicLinkRateLimiter.MAX_PER_EMAIL);
        when(redisTemplate.getExpire(anyString())).thenReturn(500L);

        assertThatCode(() -> rateLimiter.check("user@example.com", "203.0.113.7"))
                .doesNotThrowAnyException();

        verify(redisTemplate, never()).expire(anyString(), eq(MagicLinkRateLimiter.WINDOW));
    }

    @Test
    @DisplayName("Should throw when IP limit is exceeded and skip the email counter")
    void shouldThrowWhenIpLimitExceeded() {
        when(valueOperations.increment(startsWith(MagicLinkRateLimiter.IP_PREFIX)))
                .thenReturn(MagicLinkRateLimiter.MAX_PER_IP + 1L);
        when(redisTemplate.getExpire(startsWith(MagicLinkRateLimiter.IP_PREFIX))).thenReturn(300L);

        assertThatThrownBy(() -> rateLimiter.check("user@example.com", "203.0.113.7"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(ex -> assertThat(((TooManyRequestsException) ex).getRetryAfterSeconds()).isEqualTo(300L));

        verify(valueOperations, never()).increment(startsWith(MagicLinkRateLimiter.EMAIL_PREFIX));
    }

    @Test
    @DisplayName("Should throw when email limit is exceeded")
    void shouldThrowWhenEmailLimitExceeded() {
        when(valueOperations.increment(startsWith(MagicLinkRateLimiter.IP_PREFIX))).thenReturn(1L);
        when(redisTemplate.getExpire(startsWith(MagicLinkRateLimiter.IP_PREFIX))).thenReturn(800L);
        when(valueOperations.increment(startsWith(MagicLinkRateLimiter.EMAIL_PREFIX)))
                .thenReturn(MagicLinkRateLimiter.MAX_PER_EMAIL + 1L);
        when(redisTemplate.getExpire(startsWith(MagicLinkRateLimiter.EMAIL_PREFIX))).thenReturn(120L);

        assertThatThrownBy(() -> rateLimiter.check("user@example.com", "203.0.113.7"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(ex -> assertThat(((TooManyRequestsException) ex).getRetryAfterSeconds()).isEqualTo(120L));
    }

    @Test
    @DisplayName("Should not store the raw email in the Redis key")
    void shouldHashEmailInRedisKey() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(100L);

        rateLimiter.check("user@example.com", "203.0.113.7");

        verify(valueOperations, never()).increment(
                MagicLinkRateLimiter.EMAIL_PREFIX + "user@example.com");
        verify(valueOperations).increment(startsWith(MagicLinkRateLimiter.EMAIL_PREFIX));
    }

    @Test
    @DisplayName("Should use a fallback key when client IP is unknown")
    void shouldUseFallbackKeyWhenIpIsNull() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(100L);

        rateLimiter.check("user@example.com", null);

        verify(valueOperations).increment(MagicLinkRateLimiter.IP_PREFIX + "unknown");
    }
}

package com.github.cauanHalison.scheduling_api.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("Should store token JTI in Redis with positive remaining TTL")
    void shouldBlacklistTokenWithPositiveTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        Instant expiresAt = Instant.now().plusSeconds(600);

        tokenBlacklistService.blacklistToken(jti, expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("auth:blacklist:" + jti), eq("revoked"), ttlCaptor.capture());

        assertThat(ttlCaptor.getValue().getSeconds()).isBetween(595L, 600L);
    }

    @Test
    @DisplayName("Should not store token in Redis when token is already expired")
    void shouldNotBlacklistAlreadyExpiredToken() {
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        Instant expiredAt = Instant.now().minusSeconds(10);

        tokenBlacklistService.blacklistToken(jti, expiredAt);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should return true when JTI is present in Redis blacklist")
    void shouldReturnTrueWhenJtiIsBlacklisted() {
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        when(redisTemplate.hasKey("auth:blacklist:" + jti)).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    @DisplayName("Should return false when JTI is not present in Redis blacklist or is null")
    void shouldReturnFalseWhenJtiIsNotBlacklistedOrNull() {
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        when(redisTemplate.hasKey("auth:blacklist:" + jti)).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted(jti)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted(null)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted("   ")).isFalse();
    }
}

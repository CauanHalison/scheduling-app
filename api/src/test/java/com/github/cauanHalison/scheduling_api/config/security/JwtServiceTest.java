package com.github.cauanHalison.scheduling_api.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {
    private JwtService jwtService;
    private ProviderPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "chave-secreta-de-teste-com-pelo-menos-256-bits-para-algoritmo-hmac-sha!"
        );

        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900L);

        principal = new ProviderPrincipal(
                UUID.randomUUID(),
                "Test principal",
                "test@principal.com",
                "hashedPassword",
                "slug-test",
                "America/Sao_Paulo"
        );
    }

    @Test
    @DisplayName("Should generate a valid JWT token and extract the provider ID as subject")
    void shoudGenerateTokenAndExtractSubject() {
        String token = jwtService.generateToken(principal);
        String extractedSubject = jwtService.extractUsername(token);

        assertThat(token).isNotBlank();
        assertThat(extractedSubject).isEqualTo(principal.id().toString());
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("Should embed provider claims and JTI into JWT and reconstruct ProviderPrincipal without passwordHash")
    void shouldExtractPrincipalAndJtiFromToken() {
        String token = jwtService.generateToken(principal);

        String jti = jwtService.extractJti(token);
        Instant expiration = jwtService.extractExpiration(token);
        ProviderPrincipal extractedPrincipal = jwtService.extractPrincipal(token);

        assertThat(jti).isNotBlank();
        assertThat(expiration).isAfter(Instant.now());
        assertThat(extractedPrincipal.id()).isEqualTo(principal.id());
        assertThat(extractedPrincipal.name()).isEqualTo(principal.name());
        assertThat(extractedPrincipal.email()).isEqualTo(principal.email());
        assertThat(extractedPrincipal.slug()).isEqualTo(principal.slug());
        assertThat(extractedPrincipal.timeZone()).isEqualTo(principal.timeZone());
        assertThat(extractedPrincipal.passwordHash()).isNull();
    }

    @Test
    @DisplayName("Should return false when validating token against a different user")
    void shouldReturnFalseWhenUserDoesNotMatch() {
        String token = jwtService.generateToken(principal);
        ProviderPrincipal otherPrincipal = new ProviderPrincipal(
                UUID.randomUUID(),
                "Other Principal",
                "other@principal.com",
                "hashpassword",
                "other-slug",
                "America/Sao_Paulo"
        );

        assertThat(jwtService.isTokenValid(token, otherPrincipal)).isFalse();
    }

    @Test
    @DisplayName("Should return false when token is expired")
    void shouldReturnFalseWhenTokenIsExpired() {
        // Arrange: configura expiração negativa (-10 segundos) para já nascer expirado
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -10L);
        String expiredToken = jwtService.generateToken(principal);

        // Act & Assert
        assertThat(jwtService.isTokenValid(expiredToken, principal)).isFalse();
    }
}

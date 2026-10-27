package com.github.cauanHalison.scheduling_api.identity.service;

import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.config.security.TokenBlacklistService;
import com.github.cauanHalison.scheduling_api.exception.exceptions.InvalidTokenException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.domain.RefreshTokenEntity;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.ProviderResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.RefreshTokenRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.RegisterProviderRequest;
import com.github.cauanHalison.scheduling_api.identity.mapper.ProviderMapper;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.identity.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Spy
    private ProviderMapper providerMapper = new ProviderMapper();

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationSeconds", 604800L);
    }

    @Test
    @DisplayName("Should register a new provider successfully with encoded password")
    void shouldRegisterWithEncodedPassword() {
        RegisterProviderRequest request = new RegisterProviderRequest(
                "Provider test",
                "provider@test.com",
                "rawPassword",
                "test-slug",
                "America/Sao_Paulo"
        );

        when(providerRepository.existsByEmail(request.email())).thenReturn(false);
        when(providerRepository.existsBySlug(request.slug())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<ProviderEntity> captor = ArgumentCaptor.forClass(ProviderEntity.class);
        verify(providerRepository).save(captor.capture());

        ProviderEntity saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo(request.name());
        assertThat(saved.getEmail()).isEqualTo(request.email());
        assertThat(saved.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(saved.getSlug()).isEqualTo(request.slug());
        assertThat(saved.getTimeZone()).isEqualTo(request.timeZone());
    }


    @Test
    @DisplayName("Should throw ResourceConflictException when registering with an existing email")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterProviderRequest request = new RegisterProviderRequest(
                "Provider test",
                "duplicated@email.com",
                "password",
                "slug-test",
                "America/Sao_Paulo"
        );

        when(providerRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceConflictException.class);

        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when registering with an existing slug")
    void shouldThrowConflictWhenSlugAlreadyExists() {
        RegisterProviderRequest request = new RegisterProviderRequest(
                "Provider test",
                "provider@test.com",
                "password",
                "slug-duplicated",
                "America/Sao_Paulo"
        );

        when(providerRepository.existsByEmail(request.email())).thenReturn(false);
        when(providerRepository.existsBySlug(request.slug())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceConflictException.class);

        verify(providerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate provider and return access and refresh tokens")
    void shouldLoginSuccessFully() {
        UUID providerId = UUID.randomUUID();

        ProviderPrincipal principal = new ProviderPrincipal(
               providerId,
               "provider test name",
               "provider@test.com",
               "hashPassword",
               "slug-test",
               "America/Sao_Paulo"
        );

        AuthRequest request = new AuthRequest("provider@test.com", "hashPassword");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(providerRepository.getReferenceById(principal.id())).thenReturn(new ProviderEntity());
        when(jwtService.generateToken(principal)).thenReturn("accessToken");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.authToken()).isEqualTo("accessToken");

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshTokenEntity savedToken = captor.getValue();

        assertThat(savedToken.getTokenHash())
                .isNotBlank()
                .hasSize(64)
                .isNotEqualTo(response.refreshToken());

        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should rotate refresh token, revoke old token and return new token pair")
    void shouldRefreshTokenSuccessfully() {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());
        provider.setName("João");
        provider.setEmail("joao@email.com");
        provider.setPasswordHash("hash");
        provider.setSlug("joao-slug");
        provider.setTimeZone("America/Sao_Paulo");

        RefreshTokenEntity storedToken = new RefreshTokenEntity();
        storedToken.setId(UUID.randomUUID());
        storedToken.setProvider(provider);
        storedToken.setTokenHash("old_hash");
        storedToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(jwtService.generateToken(any())).thenReturn("accessToken");

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("raw-refresh-token"));

        assertThat(response).isNotNull();
        assertThat(response.authToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(storedToken.getReplacedBy()).isNotBlank();
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when refresh token is expired")
    void shouldThrowExceptionWhenRefreshTokenIsExpired() {
        RefreshTokenEntity expiredToken = new RefreshTokenEntity();
        expiredToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should revoke refresh token in database and blacklist access token in Redis on logout")
    void shouldRevokeTokenAndBlacklistAccessTokenOnLogout() {
        RefreshTokenEntity activeToken = new RefreshTokenEntity();
        activeToken.setId(UUID.randomUUID());
        activeToken.setExpiresAt(Instant.now().plusSeconds(3600));

        Instant accessTokenExpiresAt = Instant.now().plusSeconds(600);
        String jti = UUID.randomUUID().toString();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(activeToken));
        when(jwtService.extractJti("valid-jwt")).thenReturn(jti);
        when(jwtService.extractExpiration("valid-jwt")).thenReturn(accessTokenExpiresAt);

        authService.logout("Bearer valid-jwt", new RefreshTokenRequest("raw-token"));

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(activeToken);
        verify(tokenBlacklistService).blacklistToken(jti, accessTokenExpiresAt);
    }

    @Test
    @DisplayName("Should return current authenticated provider response")
    void shouldReturnAuthenticatedProviderResponse() {
        UUID providerId = UUID.randomUUID();
        ProviderPrincipal principal = new ProviderPrincipal(
                providerId,
                "Provider Name",
                "provider@test.com",
                "hashPassword",
                "provider-slug",
                "America/Sao_Paulo"
        );

        ProviderResponse response = authService.me(principal);

        assertThat(response.id()).isEqualTo(providerId);
        assertThat(response.name()).isEqualTo("Provider Name");
        assertThat(response.email()).isEqualTo("provider@test.com");
        assertThat(response.slug()).isEqualTo("provider-slug");
        assertThat(response.timeZone()).isEqualTo("America/Sao_Paulo");
    }
}

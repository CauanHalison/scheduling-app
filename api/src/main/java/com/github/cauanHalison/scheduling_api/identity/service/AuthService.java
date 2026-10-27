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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final ProviderRepository providerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProviderMapper providerMapper;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-token-expiration-seconds:604800}")
    private long refreshTokenExpirationSeconds;

    @Transactional
    public void register(RegisterProviderRequest request) {
        if (providerRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException("Este e-mail já está em uso.");
        }

        if (providerRepository.existsBySlug(request.slug())) {
            throw new ResourceConflictException("Este slug já está em uso.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        ProviderEntity providerEntity = providerMapper.toEntity(request, encodedPassword);

        ProviderEntity savedProvider = providerRepository.save(providerEntity);
        log.info("New provider registered successfully: id={}, slug={}", savedProvider.getId(), savedProvider.getSlug());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        ProviderPrincipal principal = (ProviderPrincipal) authentication.getPrincipal();
        ProviderEntity provider = providerRepository.getReferenceById(principal.id());

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = createRefreshToken(provider);

        log.info("Provider logged in successfully: providerId={}", principal.id());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String incomingHash = hashToken(request.refreshToken());

        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido."));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expirado ou revogado.");
        }

        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);

        storedToken.setRevokedAt(Instant.now());
        storedToken.setReplacedBy(newTokenHash);
        refreshTokenRepository.save(storedToken);

        saveRefreshTokenEntity(storedToken.getProvider(), newTokenHash);

        ProviderPrincipal principal = ProviderPrincipal.fromEntity(storedToken.getProvider());
        String newAccessToken = jwtService.generateToken(principal);

        log.info("Refresh token rotated successfully: providerId={}", principal.id());
        return new AuthResponse(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void logout(String authHeader, RefreshTokenRequest request) {
        String incomingHash = hashToken(request.refreshToken());

        refreshTokenRepository.findByTokenHash(incomingHash)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                        log.info("Provider logged out (refresh token revoked): tokenId={}", token.getId());
                    }
                });

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                String jti = jwtService.extractJti(accessToken);
                Instant expiresAt = jwtService.extractExpiration(accessToken);
                tokenBlacklistService.blacklistToken(jti, expiresAt);
                log.info("Access token blacklisted in Redis: jti={}", jti);
            } catch (Exception e) {
                log.debug("Skipped access token blacklisting (expired or invalid token)");
            }
        }
    }

    public ProviderResponse me(ProviderPrincipal principal) {
        return providerMapper.toResponse(principal);
    }

    private String createRefreshToken(ProviderEntity provider) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        saveRefreshTokenEntity(provider, tokenHash);
        return rawToken;
    }

    private void saveRefreshTokenEntity(ProviderEntity provider, String tokenHash) {
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setProvider(provider);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds));
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

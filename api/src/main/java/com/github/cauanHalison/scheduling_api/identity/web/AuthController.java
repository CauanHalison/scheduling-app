package com.github.cauanHalison.scheduling_api.identity.web;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.ProviderResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.RefreshTokenRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.RegisterProviderRequest;
import com.github.cauanHalison.scheduling_api.identity.service.AuthService;
import com.github.cauanHalison.scheduling_api.identity.web.docs.AuthControllerDocs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest authRequest) {
        return ResponseEntity.ok(authService.login(authRequest));
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterProviderRequest register) {
        authService.register(register);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest
    ) {
        authService.logout(authHeader, refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ProviderResponse> me(@AuthenticationPrincipal ProviderPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}

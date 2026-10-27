package com.github.cauanHalison.scheduling_api.identity.web.docs;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.ProviderResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.RefreshTokenRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.RegisterProviderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "Endpoints for login, registration and token renewal")
public interface AuthControllerDocs {

    @Operation(summary = "Authenticate provider", description = "Validates credentials and returns Access and Refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful")
    })
    ResponseEntity<AuthResponse> login(AuthRequest authRequest);

    @Operation(summary = "Register provider", description = "Registers a new provider in the application")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Provider successfully registered")
    })
    ResponseEntity<Void> register(RegisterProviderRequest register);

    @Operation(summary = "Refresh token", description = "Rotates the refresh token and generates a new token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    })
    ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest);

    @Operation(summary = "Logout", description = "Revokes the refresh token, blacklists the access token in Redis and logs out the provider")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Provider successfully logged out")
    })
    ResponseEntity<Void> logout(@Parameter(hidden = true) String authHeader, RefreshTokenRequest refreshTokenRequest);

    @Operation(summary = "Get current provider", description = "Returns the authenticated provider profile data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    })
    ResponseEntity<ProviderResponse> me(@Parameter(hidden = true) ProviderPrincipal principal);
}

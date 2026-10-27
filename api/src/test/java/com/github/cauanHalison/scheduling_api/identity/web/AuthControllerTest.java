package com.github.cauanHalison.scheduling_api.identity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.AuthResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.RefreshTokenRequest;
import com.github.cauanHalison.scheduling_api.identity.dto.RegisterProviderRequest;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderRepository providerRepository;

    @BeforeEach
    void setUp() {
        providerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should execute full auth flow: register -> login -> me -> refresh -> logout -> reject blacklisted token")
    void shouldExecuteFullAuthnticationFlow() throws Exception {
        // REGISTER (201)
        RegisterProviderRequest registerProviderRequest = new RegisterProviderRequest(
                "Provider test",
                "provider@test.com",
                "password",
                "slug-test",
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerProviderRequest)))
                .andExpect(status().isCreated());

        //LOGIN (200 + tokens)
        AuthRequest authRequest = new AuthRequest(
                "provider@test.com",
                "password"
        );

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
        ;

        AuthResponse tokens = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // ME with JWT (200 OK)
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + tokens.authToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("provider@test.com"))
                .andExpect(jsonPath("$.slug").value("slug-test"));

        // ME without JWT (401 Unauthorized)
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        //Refresh token (200 + new tokens)
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(tokens.refreshToken());

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse newTokens = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        //Refresh token (401)
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized());

        //Logout (204) - passing both Authorization header and RefreshToken body
        RefreshTokenRequest newRefreshTokenRequest = new RefreshTokenRequest(newTokens.refreshToken());

        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + newTokens.authToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRefreshTokenRequest)))
                .andExpect(status().isNoContent());

        // ME after Logout with blacklisted JWT in Redis (401 Unauthorized)
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + newTokens.authToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 bad request when try to register with field errors when email and slug is invalid")
    void shouldReturn400WhenValidationFails() throws Exception {
        RegisterProviderRequest request = new RegisterProviderRequest(
                "",
                "invalid-email",
                "123",
                "invalid slug",
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    @DisplayName("Should return 409 when try to register a provider with a existent email or slug")
    void shouldReturn409whenEmailOrSlugAlreadyExists() throws Exception {
        //Register first provider (201)
        RegisterProviderRequest firstProvider = new RegisterProviderRequest(
                "first provider test",
                "provider@test.com",
                "password",
                "first-slug-test",
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstProvider)))
                .andExpect(status().isCreated());

        //Register second provider with duplicated email (409)
        RegisterProviderRequest secondProvider = new RegisterProviderRequest(
                "second provider test",
                "provider@test.com",
                "password",
                "second-slug-test",
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondProvider)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Este e-mail já está em uso."));

        //Register third provider with duplicated slug (409)
        RegisterProviderRequest thirdProvider = new RegisterProviderRequest(
                "third provider test",
                "thirdprovider@test.com",
                "password",
                "first-slug-test",
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(thirdProvider)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Este slug já está em uso."));

    }
}

package com.github.cauanHalison.scheduling_api.eventtype.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EventTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private JwtService jwtService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        eventTypeRepository.deleteAll();
        providerRepository.deleteAll();

        ProviderEntity provider = createAndSaveProvider("provider@test.com", "provider-slug");
        accessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(provider));
    }

    @Test
    @DisplayName("Should execute full event type CRUD flow: create -> list -> findById -> update -> toggle -> delete")
    void shouldExecuteFullEventTypeCrudFlow() throws Exception {
        // Create (201)
        EventTypeRequest request = new EventTypeRequest(
                "Event test",
                "event-test",
                "https://meet.google.com/abc-defg-hij",
                "Event test description",
                50,
                5
        );

        MvcResult createdResult = mockMvc.perform(post("/event-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventLink").value("https://meet.google.com/abc-defg-hij"))
                .andReturn();

        EventTypeResponse eventTypeResponse = objectMapper.readValue(
                createdResult.getResponse().getContentAsString(),
                EventTypeResponse.class
        );

        // Find All (200)
        mockMvc.perform(get("/event-types")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Event test"))
                .andExpect(jsonPath("$[0].slug").value("event-test"))
                .andExpect(jsonPath("$[0].eventLink").value("https://meet.google.com/abc-defg-hij"));

        // Find By Id (200)
        mockMvc.perform(get("/event-types/{eventTypeId}", eventTypeResponse.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventTypeResponse.id().toString()))
                .andExpect(jsonPath("$.title").value("Event test"))
                .andExpect(jsonPath("$.slug").value("event-test"))
                .andExpect(jsonPath("$.eventLink").value("https://meet.google.com/abc-defg-hij"))
                .andExpect(jsonPath("$.description").value("Event test description"))
                .andExpect(jsonPath("$.durationMinutes").value(50))
                .andExpect(jsonPath("$.bufferAfterMinutes").value(5))
                .andExpect(jsonPath("$.isActive").value(true));

        // Update event type (200)
        EventTypeRequest updatedEvent = new EventTypeRequest(
                "Event update test",
                "event-update-test",
                "https://zoom.us/j/123456789",
                "Event update test description",
                40,
                3
        );

        mockMvc.perform(put("/event-types/{eventTypeId}", eventTypeResponse.id())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventTypeResponse.id().toString()))
                .andExpect(jsonPath("$.slug").value("event-update-test"))
                .andExpect(jsonPath("$.eventLink").value("https://zoom.us/j/123456789"))
                .andExpect(jsonPath("$.title").value("Event update test"))
                .andExpect(jsonPath("$.description").value("Event update test description"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.durationMinutes").value(40))
                .andExpect(jsonPath("$.bufferAfterMinutes").value(3));

        // Toggle isActive (200)
        mockMvc.perform(patch("/event-types/{eventTypeId}", eventTypeResponse.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventTypeResponse.id().toString()))
                .andExpect(jsonPath("$.isActive").value(false));

        // Delete event (204)
        mockMvc.perform(delete("/event-types/{eventTypeId}", eventTypeResponse.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when finding a non-existent event type by id")
    void shouldReturn404WhenFindingNonExistentEventTypeById() throws Exception {
        mockMvc.perform(get("/event-types/{eventTypeId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Tipo de evento não encontrado."));
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    @DisplayName("Should return 400 bad request when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        EventTypeRequest invalidRequest = new EventTypeRequest(
                "",
                "invalid slug",
                "javascript:alert(1)",
                "Valid description",
                0,
                -1
        );

        mockMvc.perform(post("/event-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(5));
    }

    @Test
    @DisplayName("Should return 409 conflict when creating an event type with a duplicated slug for the same provider")
    void shouldReturn409WhenSlugAlreadyExistsForSameProvider() throws Exception {
        EventTypeRequest originalEvent = new EventTypeRequest(
                "Event test",
                "event-test",
                null,
                "Event test description",
                50,
                5
        );

        EventTypeRequest newEvent = new EventTypeRequest(
                "Event test",
                "event-test",
                null,
                "Event test description",
                50,
                5
        );

        mockMvc.perform(post("/event-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalEvent)))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/event-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Este slug já está em uso."));
    }

    @Test
    @DisplayName("Should return 404 when a provider tries to access or modify another provider's event type")
    void shouldReturn404WhenAccessingAnotherProviderEventType() throws Exception {
        EventTypeRequest request = new EventTypeRequest(
                "Provider A Event",
                "provider-a-event",
                null,
                "Description",
                30,
                5
        );

        MvcResult createdResult = mockMvc.perform(post("/event-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        EventTypeResponse providerAEvent = objectMapper.readValue(
                createdResult.getResponse().getContentAsString(),
                EventTypeResponse.class
        );

        ProviderEntity otherProvider = createAndSaveProvider("other@test.com", "other-provider-slug");
        String otherAccessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(otherProvider));

        mockMvc.perform(get("/event-types/{eventTypeId}", providerAEvent.id())
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));

        mockMvc.perform(delete("/event-types/{eventTypeId}", providerAEvent.id())
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    private ProviderEntity createAndSaveProvider(String email, String slug) {
        ProviderEntity provider = new ProviderEntity();
        provider.setName("Provider Test");
        provider.setEmail(email);
        provider.setPasswordHash("hashed-password");
        provider.setSlug(slug);
        provider.setTimeZone("America/Sao_Paulo");
        return providerRepository.save(provider);
    }
}

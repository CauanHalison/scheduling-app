package com.github.cauanHalison.scheduling_api.schedule.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import com.github.cauanHalison.scheduling_api.schedule.repository.ScheduleOverrideRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ScheduleOverrideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ScheduleOverrideRepository scheduleOverrideRepository;

    @Autowired
    private JwtService jwtService;

    private ProviderEntity authenticatedProvider;
    private String accessToken;

    @BeforeEach
    void setUp() {
        scheduleOverrideRepository.deleteAll();
        providerRepository.deleteAll();

        authenticatedProvider = createAndSaveProvider("provider@test.com", "provider-slug");
        accessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(authenticatedProvider));
    }

    @Test
    @DisplayName("Should execute full schedule override flow: get empty -> create -> list -> delete -> get empty")
    void shouldExecuteFullScheduleOverrideLifecycleFlow() throws Exception {
        // List empty (200)
        mockMvc.perform(get("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Create (201)
        Instant start = hoursFromNow(24);
        Instant end = hoursFromNow(28);
        ScheduleOverrideRequest createRequest = new ScheduleOverrideRequest(start, end, "Medical appointment");

        MvcResult createdResult = mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.startTime").value(start.toString()))
                .andExpect(jsonPath("$.endTime").value(end.toString()))
                .andExpect(jsonPath("$.reason").value("Medical appointment"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        ScheduleOverrideResponse createdResponse = objectMapper.readValue(
                createdResult.getResponse().getContentAsString(),
                ScheduleOverrideResponse.class
        );

        // List with 1 item (200)
        mockMvc.perform(get("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(createdResponse.id().toString()))
                .andExpect(jsonPath("$[0].reason").value("Medical appointment"));

        // Delete (204)
        mockMvc.perform(delete("/schedules/overrides/{overrideId}", createdResponse.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // List empty again (200)
        mockMvc.perform(get("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(scheduleOverrideRepository.findById(createdResponse.id())).isEmpty();
    }

    @Test
    @DisplayName("Should return only future or ongoing overrides ordered by start time")
    void shouldReturnOnlyFutureOverridesOrderedByStartTime() throws Exception {
        ScheduleOverrideEntity expiredOverride = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(-48), hoursFromNow(-24), "Past holiday"
        );
        ScheduleOverrideEntity laterFutureOverride = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(72), hoursFromNow(76), "Later block"
        );
        ScheduleOverrideEntity ongoingOverride = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(-2), hoursFromNow(4), "Ongoing block"
        );
        ScheduleOverrideEntity soonerFutureOverride = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(24), hoursFromNow(28), "Sooner block"
        );

        mockMvc.perform(get("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(ongoingOverride.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(soonerFutureOverride.getId().toString()))
                .andExpect(jsonPath("$[2].id").value(laterFutureOverride.getId().toString()));

        assertThat(scheduleOverrideRepository.findById(expiredOverride.getId())).isPresent();
    }

    @Test
    @DisplayName("Should return 409 Conflict when creating an overlapping override for the same provider")
    void shouldReturn409WhenCreatingOverlappingOverrideForSameProvider() throws Exception {
        ScheduleOverrideRequest firstBlock = new ScheduleOverrideRequest(
                hoursFromNow(24),
                hoursFromNow(28),
                "First block"
        );

        ScheduleOverrideRequest overlappingBlock = new ScheduleOverrideRequest(
                hoursFromNow(26),
                hoursFromNow(30),
                "Overlapping block"
        );

        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBlock)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingBlock)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Já existe um bloqueio de agenda que se sobrepõe ao período informado."));
    }

    @Test
    @DisplayName("Should allow adjacent overrides [) and identical time range for different providers")
    void shouldAllowAdjacentOverridesAndSameRangeForDifferentProviders() throws Exception {
        Instant t1 = hoursFromNow(24);
        Instant t2 = hoursFromNow(26);
        Instant t3 = hoursFromNow(28);

        // Provider A creates [t1, t2)
        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleOverrideRequest(t1, t2, "Morning block"))))
                .andExpect(status().isCreated());

        // Provider A creates adjacent [t2, t3) -> allowed by '[)' range
        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleOverrideRequest(t2, t3, "Afternoon block"))))
                .andExpect(status().isCreated());

        // Provider B creates identical [t1, t2) -> allowed because provider_id differs
        ProviderEntity otherProvider = createAndSaveProvider("other@test.com", "other-slug");
        String otherAccessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(otherProvider));

        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + otherAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleOverrideRequest(t1, t2, "Other provider block"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent override or another provider's override")
    void shouldReturn404WhenDeletingNonExistentOrAnotherProviderOverride() throws Exception {
        // Non-existent UUID
        mockMvc.perform(delete("/schedules/overrides/{overrideId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Exceção não encontrada na base de dados"));

        // Another provider attempting to delete Provider A's override
        ScheduleOverrideEntity providerAOverride = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(24), hoursFromNow(26), "Provider A block"
        );

        ProviderEntity otherProvider = createAndSaveProvider("other@test.com", "other-slug");
        String otherAccessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(otherProvider));

        mockMvc.perform(delete("/schedules/overrides/{overrideId}", providerAOverride.getId())
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));

        assertThat(scheduleOverrideRepository.findById(providerAOverride.getId())).isPresent();
    }

    @Test
    @DisplayName("Should return 400 Bad Request when payload violates validation rules")
    void shouldReturn400WhenValidationFails() throws Exception {
        ScheduleOverrideRequest invalidRangeRequest = new ScheduleOverrideRequest(
                hoursFromNow(28),
                hoursFromNow(24),
                "a".repeat(256)
        );

        mockMvc.perform(post("/schedules/overrides")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Erro de validação nos campos informados."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when request is unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/schedules/overrides")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/schedules/overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/schedules/overrides/{overrideId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should find only overrides overlapping the requested time window")
    void shouldFindOverlappingOverridesInRange() {
        Instant windowStart = hoursFromNow(24);
        Instant windowEnd = hoursFromNow(32);

        // Before window [18h, 22h) -> should NOT overlap
        saveOverrideDirectly(authenticatedProvider, hoursFromNow(18), hoursFromNow(22), "Before");
        // Overlaps window start [22h, 26h) -> SHOULD overlap
        ScheduleOverrideEntity overlappingStart = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(22), hoursFromNow(26), "Overlaps start"
        );
        // Inside window [27h, 29h) -> SHOULD overlap
        ScheduleOverrideEntity insideWindow = saveOverrideDirectly(
                authenticatedProvider, hoursFromNow(27), hoursFromNow(29), "Inside"
        );
        // Adjacent at windowEnd [32h, 36h) -> should NOT overlap
        saveOverrideDirectly(authenticatedProvider, windowEnd, hoursFromNow(36), "After");

        List<ScheduleOverrideEntity> overlapping = scheduleOverrideRepository.findOverlapping(
                authenticatedProvider.getId(),
                windowStart,
                windowEnd
        );

        assertThat(overlapping)
                .extracting(ScheduleOverrideEntity::getId)
                .containsExactly(overlappingStart.getId(), insideWindow.getId());
    }

    private static Instant hoursFromNow(long hours) {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(hours, ChronoUnit.HOURS);
    }

    private ScheduleOverrideEntity saveOverrideDirectly(
            ProviderEntity provider,
            Instant startTime,
            Instant endTime,
            String reason
    ) {
        ScheduleOverrideEntity entity = new ScheduleOverrideEntity();
        entity.setProvider(provider);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setReason(reason);
        return scheduleOverrideRepository.saveAndFlush(entity);
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

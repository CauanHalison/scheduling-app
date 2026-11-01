package com.github.cauanHalison.scheduling_api.schedule.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.DayScheduleRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.TimeIntervalRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleRequest;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvailabilityScheduleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private AvailabilityScheduleRepository availabilityScheduleRepository;

    @Autowired
    private JwtService jwtService;

    private ProviderEntity authenticatedProvider;
    private String accessToken;

    @BeforeEach
    void setUp() {
        availabilityScheduleRepository.deleteAll();
        providerRepository.deleteAll();

        authenticatedProvider = createAndSaveProvider();

        accessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(authenticatedProvider));
    }

    @Test
    @DisplayName("Should execute full weekly schedule flow: get empty -> replace -> overwrite -> clear")
    void shouldExecuteFullScheduleLifecycleFlow() throws Exception {
        // find empty (200)
        mockMvc.perform(get("/schedules")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[1].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[2].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[3].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[4].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[5].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[6].intervals.length()").value(0));

        // Add time interval on monday (200)
        WeeklyScheduleRequest mondayRequest = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new TimeIntervalRequest(LocalTime.of(13, 0), LocalTime.of(17, 0))
                ))
        ));

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mondayRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].intervals.length()").value(2))
                .andExpect(jsonPath("$.days[0].intervals[0].startTime").value("08:00"))
                .andExpect(jsonPath("$.days[0].intervals[0].endTime").value("12:00"))
                .andExpect(jsonPath("$.days[0].intervals[1].startTime").value("13:00"))
                .andExpect(jsonPath("$.days[0].intervals[1].endTime").value("17:00"));

        List<AvailabilityScheduleEntity> inDb = availabilityScheduleRepository.findAllByProviderId(authenticatedProvider.getId());
        assertThat(inDb).hasSize(2);
        assertThat(inDb).allMatch(entity -> entity.getDayOfWeek() == MONDAY);

        // Overwriting interval in tuesday (200)
        WeeklyScheduleRequest tuesdayRequest = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(TUESDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new TimeIntervalRequest(LocalTime.of(13, 0), LocalTime.of(17, 0))
                ))
        ));

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tuesdayRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].intervals.length()").value(0))
                .andExpect(jsonPath("$.days[1].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.days[1].intervals.length()").value(2))
                .andExpect(jsonPath("$.days[1].intervals[0].startTime").value("08:00"))
                .andExpect(jsonPath("$.days[1].intervals[0].endTime").value("12:00"))
                .andExpect(jsonPath("$.days[1].intervals[1].startTime").value("13:00"))
                .andExpect(jsonPath("$.days[1].intervals[1].endTime").value("17:00"));

        inDb = availabilityScheduleRepository.findAllByProviderId(authenticatedProvider.getId());
        assertThat(inDb).hasSize(2);
        assertThat(inDb).allMatch(entity -> entity.getDayOfWeek() == TUESDAY);

        // Cleaning schedule (200)
        WeeklyScheduleRequest cleaningRequest = new WeeklyScheduleRequest(List.of());

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cleaningRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].intervals").isEmpty())
                .andExpect(jsonPath("$.days[1].intervals").isEmpty()
                );

        inDb = availabilityScheduleRepository.findAllByProviderId(authenticatedProvider.getId());
        assertThat(inDb).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 Bad Request when payload violates validation rules")
    void shouldReturnBadRequestOnValidationErrors() throws Exception {
        // Add different interval blocks for the same day (400)
        WeeklyScheduleRequest sameDayRequest = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0))
                )),
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(13, 0), LocalTime.of(17, 0))
                ))
        ));

        mockMvc.perform(put("/schedules")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sameDayRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Erro de validação nos campos informados."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].detail").value("Cada dia da semana só pode aparecer uma vez"));

        // Add intervals with time collision (400)
        WeeklyScheduleRequest timeCollisionRequest = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new TimeIntervalRequest(LocalTime.of(10, 0), LocalTime.of(12, 0))
                ))
        ));

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timeCollisionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Erro de validação nos campos informados."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].detail").value("Os blocos de horário do mesmo dia não podem se sobrepor"));

        // Add interval with the end time before start time (400)
        WeeklyScheduleRequest timeInvalidRequest = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(12, 0), LocalTime.of(8, 0))
                ))
        ));

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timeInvalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Erro de validação nos campos informados."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].detail").value("A hora de inicio deve ser anterior a hora de fim"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when JSON format is invalid")
    void shouldReturnBadRequestOnMalformedJson() throws Exception {
        // Invalid day of week (400)
        String invalidDayJson = """
                {
                    "days": [
                        {
                            "dayOfWeek": "INVALID_DAY",
                            "intervals": []
                        }
                    ]
                }
                """;

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDayJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        // invalid time value (400)
        String invalidTimeJson = """
                {
                    "days": [
                        {
                            "dayOfWeek": "MONDAY",
                            "intervals": [
                                {
                                    "startTime": "25:99",
                                    "endTime": "12:00"
                                }
                            ]
                        }
                    ]
                }
                """;

        mockMvc.perform(put("/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTimeJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when request is unauthenticated")
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {
        mockMvc.perform(get("/schedules")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/schedules")).andExpect(status().isUnauthorized());
    }

    private ProviderEntity createAndSaveProvider() {
        ProviderEntity provider = new ProviderEntity();
        provider.setName("Provider Test");
        provider.setEmail("provider@test.com");
        provider.setPasswordHash("hashed-password");
        provider.setSlug("provider-slug");
        provider.setTimeZone("America/Sao_Paulo");
        return providerRepository.save(provider);
    }
}
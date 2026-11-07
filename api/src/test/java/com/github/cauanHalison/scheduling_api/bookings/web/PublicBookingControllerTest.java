package com.github.cauanHalison.scheduling_api.bookings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.SlotResponse;
import com.github.cauanHalison.scheduling_api.bookings.service.PublicBookingService;
import com.github.cauanHalison.scheduling_api.bookings.service.SlotCalculationService;
import com.github.cauanHalison.scheduling_api.config.security.JwtAuthenticationFilter;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.TokenBlacklistService;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PublicBookingService publicBookingService;

    @MockitoBean
    private SlotCalculationService slotCalculationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Should return provider active events showcase")
    void shouldReturnProviderEvents() throws Exception {
        EventTypeResponse event = new EventTypeResponse(
                UUID.randomUUID(), "Consulta", "consulta", "Desc", 30, 0, true, Instant.now()
        );

        when(publicBookingService.getProviderActiveEvents("dr-john")).thenReturn(List.of(event));

        mockMvc.perform(get("/public/users/{userSlug}", "dr-john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("consulta"));
    }

    @Test
    @DisplayName("Should return public event details")
    void shouldReturnPublicEventDetails() throws Exception {
        PublicEventDetailsResponse response = new PublicEventDetailsResponse(
                "Dr. John", "dr-john", "America/Sao_Paulo", "Consulta", "consulta", "Desc", 30
        );

        when(publicBookingService.getPublicEventDetails("dr-john", "consulta")).thenReturn(response);

        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}", "dr-john", "consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("Dr. John"))
                .andExpect(jsonPath("$.durationMinutes").value(30));
    }

    @Test
    @DisplayName("Should return available days for month")
    void shouldReturnAvailableDays() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        when(slotCalculationService.getAvailableDays(eq("dr-john"), eq("consulta"), any(YearMonth.class), any(ZoneId.class)))
                .thenReturn(new AvailableDaysResponse(List.of(date)));

        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/available-days", "dr-john", "consulta")
                        .param("month", "11")
                        .param("year", "2026")
                        .param("timeZone", "America/Sao_Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableDays[0]").value("2026-11-10"));
    }

    @Test
    @DisplayName("Should return available slots for date")
    void shouldReturnAvailableSlots() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        Instant slotStart = Instant.now().plusSeconds(3600);
        Instant slotEnd = slotStart.plusSeconds(1800);

        when(slotCalculationService.getAvailableSlots(eq("dr-john"), eq("consulta"), eq(date), any(ZoneId.class)))
                .thenReturn(new AvailableSlotsResponse(date, "America/Sao_Paulo", List.of(new SlotResponse(slotStart, slotEnd))));

        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/slots", "dr-john", "consulta")
                        .param("date", "2026-11-10")
                        .param("timeZone", "America/Sao_Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(1));
    }

    @Test
    @DisplayName("Should create booking and return 201 Created")
    void shouldCreateBooking() throws Exception {
        Instant startTime = Instant.now().plusSeconds(86400);
        CreateBookingRequest request = new CreateBookingRequest("Carlos", "carlos@gmail.com", startTime);

        BookingConfirmationResponse response = new BookingConfirmationResponse(
                UUID.randomUUID(), "Consulta", "Dr. John", "Carlos", "carlos@gmail.com",
                startTime, startTime.plusSeconds(1800),
                com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus.CONFIRMED,
                UUID.randomUUID(), Instant.now()
        );

        when(publicBookingService.createBooking(eq("dr-john"), eq("consulta"), any(CreateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/public/users/{userSlug}/events/{eventSlug}/book", "dr-john", "consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientEmail").value("carlos@gmail.com"))
                .andExpect(jsonPath("$.cancelToken").isNotEmpty());
    }
    @Test
    @DisplayName("Should return 400 Bad Request when available days timezone is invalid")
    void shouldReturnBadRequestWhenAvailableDaysTimeZoneIsInvalid() throws Exception {
        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/available-days", "dr-john", "consulta")
                        .param("month", "11")
                        .param("year", "2026")
                        .param("timeZone", "Invalid/Zone"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("O fuso horário, mês ou data informado é inválido."));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when available days month is invalid")
    void shouldReturnBadRequestWhenAvailableDaysMonthIsInvalid() throws Exception {
        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/available-days", "dr-john", "consulta")
                        .param("month", "13")
                        .param("year", "2026")
                        .param("timeZone", "America/Sao_Paulo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when slots timezone is invalid")
    void shouldReturnBadRequestWhenSlotsTimeZoneIsInvalid() throws Exception {
        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/slots", "dr-john", "consulta")
                        .param("date", "2026-11-10")
                        .param("timeZone", "Invalid/Zone"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("O fuso horário, mês ou data informado é inválido."));
    }
}

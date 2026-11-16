package com.github.cauanHalison.scheduling_api.bookings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.service.GuestBookingService;
import com.github.cauanHalison.scheduling_api.config.security.JwtAuthenticationFilter;
import com.github.cauanHalison.scheduling_api.exception.exceptions.TooManyRequestsException;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuestBookingService guestBookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Should get booking by cancel token")
    void shouldGetBookingByCancelToken() throws Exception {
        UUID cancelToken = UUID.randomUUID();
        GuestBookingResponse response = new GuestBookingResponse(
                UUID.randomUUID(), "Mentoria", "Desc", 30, "Dr. John", "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CONFIRMED, null
        );

        when(guestBookingService.getBookingByCancelToken(cancelToken)).thenReturn(response);

        mockMvc.perform(get("/guest/bookings/{cancelToken}", cancelToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Carlos"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should cancel booking by token")
    void shouldCancelBookingByToken() throws Exception {
        UUID cancelToken = UUID.randomUUID();
        CancelBookingRequest request = new CancelBookingRequest("Cancelamento");

        GuestBookingResponse response = new GuestBookingResponse(
                UUID.randomUUID(), "Mentoria", "Desc", 30, "Dr. John", "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CANCELED, "Cancelamento"
        );

        when(guestBookingService.cancelBookingByToken(eq(cancelToken), any(CancelBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/guest/bookings/{cancelToken}/cancel", cancelToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Should request magic link")
    void shouldRequestMagicLink() throws Exception {
        MagicLinkRequest request = new MagicLinkRequest("carlos@gmail.com");

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(guestBookingService).requestMagicLink(any(MagicLinkRequest.class), anyString());
    }

    @Test
    @DisplayName("Should return 429 with Retry-After header when magic link rate limit is exceeded")
    void shouldReturn429WhenMagicLinkRateLimited() throws Exception {
        doThrow(new TooManyRequestsException("Muitas solicitações.", 120))
                .when(guestBookingService).requestMagicLink(any(MagicLinkRequest.class), anyString());

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MagicLinkRequest("carlos@gmail.com"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"))
                .andExpect(jsonPath("$.statusCode").value(429));
    }

    @Test
    @DisplayName("Should verify magic link token")
    void shouldVerifyMagicLinkToken() throws Exception {
        GuestBookingResponse response = new GuestBookingResponse(
                UUID.randomUUID(), "Mentoria", "Desc", 30, "Dr. John", "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CONFIRMED, null
        );

        when(guestBookingService.verifyMagicLink("valid-token")).thenReturn(List.of(response));

        mockMvc.perform(get("/guest/magic-link/verify")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientEmail").value("carlos@gmail.com"));
    }
}

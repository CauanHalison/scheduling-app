package com.github.cauanHalison.scheduling_api.bookings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.service.ProviderBookingService;
import com.github.cauanHalison.scheduling_api.config.security.JwtAuthenticationFilter;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.config.security.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProviderBookingControllerTest.TestWebMvcConfig.class)
class ProviderBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProviderBookingService providerBookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    static final UUID FIXED_PROVIDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @TestConfiguration
    static class TestWebMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    return new ProviderPrincipal(
                            FIXED_PROVIDER_ID, "Provider Name", "provider@test.com", "hash", "provider-slug", "America/Sao_Paulo"
                    );
                }
            });
        }
    }

    @Test
    @DisplayName("Should list bookings for provider")
    void shouldListBookings() throws Exception {
        ProviderBookingResponse response = new ProviderBookingResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Mentoria", 30, "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CONFIRMED, null, Instant.now()
        );

        when(providerBookingService.listBookings(eq(FIXED_PROVIDER_ID), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/provider/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clientName").value("Carlos"));
    }

    @Test
    @DisplayName("Should get booking by id for provider")
    void shouldGetBookingById() throws Exception {
        UUID bookingId = UUID.randomUUID();

        ProviderBookingResponse response = new ProviderBookingResponse(
                bookingId, UUID.randomUUID(), "Mentoria", 30, "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CONFIRMED, null, Instant.now()
        );

        when(providerBookingService.getBookingById(FIXED_PROVIDER_ID, bookingId)).thenReturn(response);

        mockMvc.perform(get("/provider/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()));
    }

    @Test
    @DisplayName("Should cancel booking by provider")
    void shouldCancelBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();

        CancelBookingRequest request = new CancelBookingRequest("Compromisso urgente");
        ProviderBookingResponse response = new ProviderBookingResponse(
                bookingId, UUID.randomUUID(), "Mentoria", 30, "Carlos", "carlos@gmail.com",
                Instant.now(), Instant.now().plusSeconds(1800), BookingStatus.CANCELED, "Compromisso urgente", Instant.now()
        );

        when(providerBookingService.cancelBooking(eq(FIXED_PROVIDER_ID), eq(bookingId), any(CancelBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/provider/bookings/{id}/cancel", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.cancellationReason").value("Compromisso urgente"));
    }
}

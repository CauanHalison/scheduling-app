package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.exception.exceptions.InvalidTokenException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.TooManyRequestsException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MagicLinkRateLimiter magicLinkRateLimiter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    @InjectMocks
    private GuestBookingService guestBookingService;

    private ProviderEntity provider;
    private EventTypeEntity eventType;
    private BookingEntity booking;
    private UUID cancelToken;

    @BeforeEach
    void setUp() {
        provider = new ProviderEntity();
        provider.setName("Provider Name");
        provider.setEmail("provider@test.com");

        eventType = new EventTypeEntity();
        eventType.setTitle("Mentoria");
        eventType.setDurationMinutes(30);

        cancelToken = UUID.randomUUID();

        booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setProvider(provider);
        booking.setEventType(eventType);
        booking.setClientName("Client");
        booking.setClientEmail("client@test.com");
        booking.setStartTime(Instant.now());
        booking.setEndTime(Instant.now().plusSeconds(1800));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCancelToken(cancelToken);
    }

    @Test
    @DisplayName("Should get booking details by cancel token")
    void shouldGetBookingByCancelToken() {
        when(bookingRepository.findByCancelToken(cancelToken)).thenReturn(Optional.of(booking));

        GuestBookingResponse response = guestBookingService.getBookingByCancelToken(cancelToken);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(booking.getId());
        assertThat(response.clientName()).isEqualTo("Client");
        assertThat(response.providerName()).isEqualTo("Provider Name");
    }

    @Test
    @DisplayName("Should cancel booking by token with reason")
    void shouldCancelBookingByToken() {
        when(bookingRepository.findByCancelToken(cancelToken)).thenReturn(Optional.of(booking));

        GuestBookingResponse response = guestBookingService.cancelBookingByToken(
                cancelToken, new CancelBookingRequest("Imprevisto pessoal")
        );

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(booking.getCancellationReason()).isEqualTo("Imprevisto pessoal");
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELED);

        ArgumentCaptor<BookingCanceledEvent> captor = ArgumentCaptor.forClass(BookingCanceledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingSnapshot snapshot = captor.getValue().booking();
        assertThat(snapshot.bookingId()).isEqualTo(booking.getId());
        assertThat(snapshot.providerEmail()).isEqualTo("provider@test.com");
        assertThat(snapshot.clientEmail()).isEqualTo("client@test.com");
        assertThat(snapshot.cancellationReason()).isEqualTo("Imprevisto pessoal");
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when guest tries to cancel a completed booking")
    void shouldThrowWhenGuestCancelsCompletedBooking() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByCancelToken(cancelToken)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> guestBookingService.cancelBookingByToken(cancelToken, null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Não é possível cancelar um agendamento já concluído.");
        verifyNoInteractions(eventPublisher);
    }
    @Test
    @DisplayName("Should store magic link in redis with 15min TTL")
    void shouldStoreMagicLinkInRedis() {
        when(bookingRepository.existsByClientEmailIgnoreCase("user@example.com")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        guestBookingService.requestMagicLink(new MagicLinkRequest("User@Example.com"), "203.0.113.7");

        ArgumentCaptor<MagicLinkRequestedEvent> captor = ArgumentCaptor.forClass(MagicLinkRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("user@example.com");
        assertThat(captor.getValue().token()).isNotBlank();
        verify(valueOperations).set(any(String.class), eq("user@example.com"), eq(Duration.ofMinutes(15)));
        verify(magicLinkRateLimiter).check("user@example.com", "203.0.113.7");
    }

    @Test
    @DisplayName("Should not store token nor publish event when email has no bookings")
    void shouldNotSendMagicLinkWhenEmailHasNoBookings() {
        when(bookingRepository.existsByClientEmailIgnoreCase("ghost@example.com")).thenReturn(false);

        guestBookingService.requestMagicLink(new MagicLinkRequest("Ghost@Example.com"), "203.0.113.7");

        verify(magicLinkRateLimiter).check("ghost@example.com", "203.0.113.7");
        verifyNoInteractions(redisTemplate, eventPublisher);
    }

    @Test
    @DisplayName("Should propagate TooManyRequestsException and do nothing else when rate limit is exceeded")
    void shouldStopWhenRateLimitExceeded() {
        doThrow(new TooManyRequestsException("limit", 60))
                .when(magicLinkRateLimiter).check(anyString(), anyString());

        assertThatThrownBy(() -> guestBookingService.requestMagicLink(
                new MagicLinkRequest("user@example.com"), "203.0.113.7"))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(bookingRepository, redisTemplate, eventPublisher);
    }
    @Test
    @DisplayName("Should verify magic link and return bookings for valid token")
    void shouldVerifyMagicLinkAndReturnBookings() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("guest:magic-link:valid-token")).thenReturn("user@example.com");
        when(bookingRepository.findAllByClientEmailIgnoreCaseOrderByStartTimeDesc("user@example.com"))
                .thenReturn(List.of(booking));

        List<GuestBookingResponse> responses = guestBookingService.verifyMagicLink("valid-token");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().clientEmail()).isEqualTo("client@test.com");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when magic link token is expired or absent")
    void shouldThrowInvalidTokenExceptionWhenTokenInvalid() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("guest:magic-link:expired-token")).thenReturn(null);

        assertThatThrownBy(() -> guestBookingService.verifyMagicLink("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Link de acesso expirado ou inválido.");
    }
}

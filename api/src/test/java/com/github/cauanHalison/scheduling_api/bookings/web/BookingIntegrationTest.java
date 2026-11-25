package com.github.cauanHalison.scheduling_api.bookings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.bookings.scheduler.BookingCompletionScheduler;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.notification.BookingReminderScheduler;
import com.github.cauanHalison.scheduling_api.notification.service.EmailService;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingIntegrationTest {

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private AvailabilityScheduleRepository availabilityScheduleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BookingReminderScheduler bookingReminderScheduler;

    @Autowired
    private BookingCompletionScheduler bookingCompletionScheduler;

    private ProviderEntity provider;
    private EventTypeEntity eventType;
    private String accessToken;
    private final ZoneId providerZone = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
        clearRateLimitKeys();
        bookingRepository.deleteAll();
        availabilityScheduleRepository.deleteAll();
        eventTypeRepository.deleteAll();
        providerRepository.deleteAll();

        provider = new ProviderEntity();
        provider.setName("Dr. Provider");
        provider.setEmail("provider@test.com");
        provider.setPasswordHash("hashedpassword");
        provider.setSlug("dr-provider");
        provider.setTimeZone(providerZone.getId());
        provider = providerRepository.save(provider);

        accessToken = jwtService.generateToken(ProviderPrincipal.fromEntity(provider));

        eventType = new EventTypeEntity();
        eventType.setProvider(provider);
        eventType.setTitle("Consulta Médica");
        eventType.setSlug("consulta-medica");
        eventType.setEventLink("https://meet.google.com/consulta-medica");
        eventType.setDurationMinutes(30);
        eventType.setBufferAfterMinutes(15);
        eventType.setIsActive(true);
        eventType = eventTypeRepository.save(eventType);
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        availabilityScheduleRepository.deleteAll();
        eventTypeRepository.deleteAll();
        providerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should execute full public booking lifecycle and allow provider and guest cancellation")
    void shouldExecuteFullBookingLifecycle() throws Exception {
        LocalDate futureMonday = getNextFutureMonday();

        AvailabilityScheduleEntity schedule = new AvailabilityScheduleEntity();
        schedule.setProvider(provider);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        availabilityScheduleRepository.save(schedule);

        // 0. Public catalog must not expose eventLink before booking
        mockMvc.perform(get("/public/users/{userSlug}", provider.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("consulta-medica"))
                .andExpect(jsonPath("$[0].eventLink").doesNotExist());

        // 1. Get available slots
        mockMvc.perform(get("/public/users/{userSlug}/events/{eventSlug}/slots", provider.getSlug(), eventType.getSlug())
                        .param("date", futureMonday.toString())
                        .param("timeZone", providerZone.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(6));

        // 2. Book 09:00 slot
        Instant requestedSlot = futureMonday.atTime(9, 0).atZone(providerZone).toInstant();
        CreateBookingRequest bookingRequest = new CreateBookingRequest("Carlos Cliente", "carlos@gmail.com", requestedSlot);

        String bookingJson = mockMvc.perform(post("/public/users/{userSlug}/events/{eventSlug}/book", provider.getSlug(), eventType.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.cancelToken").isNotEmpty())
                .andExpect(jsonPath("$.eventLink").value("https://meet.google.com/consulta-medica"))
                .andExpect(jsonPath("$.clientEmail").value("carlos@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        UUID cancelToken = UUID.fromString(objectMapper.readTree(bookingJson).get("cancelToken").asText());
        UUID bookingId = UUID.fromString(objectMapper.readTree(bookingJson).get("id").asText());

        // 3. Verify provider listing
        mockMvc.perform(get("/provider/bookings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clientName").value("Carlos Cliente"))
                .andExpect(jsonPath("$.content[0].eventLink").value("https://meet.google.com/consulta-medica"));

        // 4. Guest views booking by cancel token
        mockMvc.perform(get("/guest/bookings/{cancelToken}", cancelToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.eventLink").value("https://meet.google.com/consulta-medica"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 5. Guest cancels booking (eventLink becomes null)
        mockMvc.perform(post("/guest/bookings/{cancelToken}/cancel", cancelToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Desisti"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.eventLink").doesNotExist());

        BookingEntity updatedBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(updatedBooking.getCancellationReason()).isEqualTo("Desisti");
    }

    @Test
    @DisplayName("Should prevent double booking under high concurrency (PostgreSQL exclusion constraint test)")
    void shouldPreventDoubleBookingUnderHighConcurrency() throws Exception {
        LocalDate futureMonday = getNextFutureMonday();

        AvailabilityScheduleEntity schedule = new AvailabilityScheduleEntity();
        schedule.setProvider(provider);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(14, 0));
        schedule.setEndTime(LocalTime.of(15, 0));
        availabilityScheduleRepository.save(schedule);

        Instant targetSlot = futureMonday.atTime(14, 0).atZone(providerZone).toInstant();

        int numberOfConcurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(numberOfConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    CreateBookingRequest request = new CreateBookingRequest(
                            "Client " + index, "client" + index + "@test.com", targetSlot
                    );

                    int httpStatus = mockMvc.perform(post("/public/users/{userSlug}/events/{eventSlug}/book", provider.getSlug(), eventType.getSlug())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();

                    if (httpStatus == 201) {
                        successCount.incrementAndGet();
                    } else if (httpStatus == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); 

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(numberOfConcurrentRequests - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should test full magic link request and verify flow")
    void shouldTestMagicLinkFlow() throws Exception {
        BookingEntity directBooking = new BookingEntity();
        directBooking.setProvider(provider);
        directBooking.setEventType(eventType);
        directBooking.setClientName("Maria");
        directBooking.setClientEmail("maria@test.com");
        directBooking.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS));
        directBooking.setEndTime(Instant.now().plus(1, ChronoUnit.DAYS).plusSeconds(1800));
        directBooking.setBlockedUntil(Instant.now().plus(1, ChronoUnit.DAYS).plusSeconds(2700));
        directBooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(directBooking);

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MagicLinkRequest("maria@test.com"))))
                .andExpect(status().isOk());

        String testToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("guest:magic-link:" + testToken, "maria@test.com", Duration.ofMinutes(15));

        mockMvc.perform(get("/guest/magic-link/verify")
                        .param("token", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientEmail").value("maria@test.com"))
                .andExpect(jsonPath("$[0].eventLink").value("https://meet.google.com/consulta-medica"));
    }

    @Test
    @DisplayName("Should send creation emails to client and provider after booking commit")
    void shouldSendEmailsWhenBookingIsCreated() throws Exception {
        createMondayAvailability(LocalTime.of(9, 0), LocalTime.of(12, 0));
        bookSlot(LocalTime.of(9, 0), "Carlos Cliente", "carlos@gmail.com");

        verify(emailService, timeout(5000)).sendEmail(
                eq("carlos@gmail.com"), contains("Agendamento Confirmado"), contains("https://meet.google.com/consulta-medica"), any(byte[].class));
        verify(emailService, timeout(5000)).sendEmail(
                eq("provider@test.com"), contains("Novo Agendamento"), contains("https://meet.google.com/consulta-medica"), any(byte[].class));
    }

    @Test
    @DisplayName("Should send cancellation emails when provider cancels (regression: lazy provider in async listener)")
    void shouldSendEmailsWhenProviderCancels() throws Exception {
        createMondayAvailability(LocalTime.of(9, 0), LocalTime.of(12, 0));
        UUID bookingId = bookSlot(LocalTime.of(9, 0), "Carlos Cliente", "carlos@gmail.com");

        mockMvc.perform(post("/provider/bookings/{id}/cancel", bookingId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Prestador doente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(emailService, timeout(5000)).sendEmail(
                eq("carlos@gmail.com"), contains("Agendamento Cancelado"), contains("Prestador doente"), any(byte[].class));
        verify(emailService, timeout(5000)).sendEmail(
                eq("provider@test.com"), contains("Agendamento Cancelado"), contains("Prestador doente"), any(byte[].class));
    }

    @Test
    @DisplayName("Should send cancellation emails when guest cancels by token")
    void shouldSendEmailsWhenGuestCancels() throws Exception {
        createMondayAvailability(LocalTime.of(9, 0), LocalTime.of(12, 0));
        String bookingJson = bookSlotRaw(LocalTime.of(9, 0), "Carlos Cliente", "carlos@gmail.com");
        UUID cancelToken = UUID.fromString(objectMapper.readTree(bookingJson).get("cancelToken").asText());

        mockMvc.perform(post("/guest/bookings/{cancelToken}/cancel", cancelToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Desisti"))))
                .andExpect(status().isOk());

        verify(emailService, timeout(5000)).sendEmail(
                eq("carlos@gmail.com"), contains("Agendamento Cancelado"), contains("Desisti"), any(byte[].class));
        verify(emailService, timeout(5000)).sendEmail(
                eq("provider@test.com"), contains("Agendamento Cancelado"), contains("Desisti"), any(byte[].class));
    }

    @Test
    @DisplayName("Should not send any email when booking creation is rejected")
    void shouldNotSendEmailsWhenBookingRejected() throws Exception {
        // sem disponibilidade cadastrada: slot indisponível -> 409
        Instant slot = getNextFutureMonday().atTime(9, 0).atZone(providerZone).toInstant();

        mockMvc.perform(post("/public/users/{userSlug}/events/{eventSlug}/book", provider.getSlug(), eventType.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest("Carlos", "carlos@gmail.com", slot))))
                .andExpect(status().isConflict());

        verify(emailService, after(1000).never()).sendEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should send magic link email")
    void shouldSendMagicLinkEmail() throws Exception {
        saveDirectBooking("maria@test.com");

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MagicLinkRequest("maria@test.com"))))
                .andExpect(status().isOk());

        verify(emailService, timeout(5000)).sendEmail(
                eq("maria@test.com"), contains("link de acesso"), contains("/guest/magic-link?token="), eq(null));
    }

    @Test
    @DisplayName("Should respond 200 but send no email when email has no bookings")
    void shouldNotSendMagicLinkWhenEmailHasNoBookings() throws Exception {
        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MagicLinkRequest("ghost@test.com"))))
                .andExpect(status().isOk());

        verify(emailService, after(1000).never()).sendEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should return 429 with Retry-After after exceeding the per-email limit")
    void shouldRateLimitMagicLinkPerEmail() throws Exception {
        String body = objectMapper.writeValueAsString(new MagicLinkRequest("maria@test.com"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/guest/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.statusCode").value(429));
    }

    @Test
    @DisplayName("Should return 429 after exceeding the per-IP limit using different emails")
    void shouldRateLimitMagicLinkPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/guest/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MagicLinkRequest("user" + i + "@test.com"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/guest/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MagicLinkRequest("another@test.com"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should send reminder email once and mark reminderSentAt (idempotent on second run)")
    void shouldSendReminderEmailOnceForUpcomingBooking() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        BookingEntity upcoming = new BookingEntity();
        upcoming.setProvider(provider);
        upcoming.setEventType(eventType);
        upcoming.setClientName("Ana Lembrete");
        upcoming.setClientEmail("ana@test.com");
        upcoming.setStartTime(start);
        upcoming.setEndTime(start.plusSeconds(1800));
        upcoming.setBlockedUntil(start.plusSeconds(2700));
        upcoming.setStatus(BookingStatus.CONFIRMED);
        upcoming = bookingRepository.save(upcoming);

        // 1a execucao: deve enviar o lembrete e gravar reminderSentAt
        bookingReminderScheduler.sendUpcomingBookingReminders();

        verify(emailService, timeout(5000)).sendEmail(
                eq("ana@test.com"),
                contains("Lembrete de agendamento"),
                contains("/guest/bookings/" + upcoming.getCancelToken()),
                eq(null)
        );

        BookingEntity afterFirstRun = bookingRepository.findById(upcoming.getId()).orElseThrow();
        assertThat(afterFirstRun.getReminderSentAt()).isNotNull();

        // 2a execucao imediata: nao deve reenviar
        bookingReminderScheduler.sendUpcomingBookingReminders();

        verify(emailService, after(1000).times(1)).sendEmail(
                eq("ana@test.com"),
                contains("Lembrete de agendamento"),
                anyString(),
                any()
        );
    }

    @Test
    @DisplayName("Should mark only expired confirmed bookings as COMPLETED and reject subsequent cancellation")
    void shouldCompleteExpiredConfirmedBookings() throws Exception {
        Instant pastStart = Instant.now().minus(2, ChronoUnit.HOURS);
        BookingEntity expiredConfirmed = new BookingEntity();
        expiredConfirmed.setProvider(provider);
        expiredConfirmed.setEventType(eventType);
        expiredConfirmed.setClientName("Past Confirmed");
        expiredConfirmed.setClientEmail("past@test.com");
        expiredConfirmed.setStartTime(pastStart);
        expiredConfirmed.setEndTime(pastStart.plusSeconds(1800));
        expiredConfirmed.setBlockedUntil(pastStart.plusSeconds(2700));
        expiredConfirmed.setStatus(BookingStatus.CONFIRMED);
        expiredConfirmed = bookingRepository.save(expiredConfirmed);

        BookingEntity expiredCanceled = new BookingEntity();
        expiredCanceled.setProvider(provider);
        expiredCanceled.setEventType(eventType);
        expiredCanceled.setClientName("Past Canceled");
        expiredCanceled.setClientEmail("canceled@test.com");
        expiredCanceled.setStartTime(pastStart);
        expiredCanceled.setEndTime(pastStart.plusSeconds(1800));
        expiredCanceled.setBlockedUntil(pastStart.plusSeconds(2700));
        expiredCanceled.setStatus(BookingStatus.CANCELED);
        expiredCanceled = bookingRepository.save(expiredCanceled);

        Instant futureStart = Instant.now().plus(2, ChronoUnit.HOURS);
        BookingEntity futureConfirmed = new BookingEntity();
        futureConfirmed.setProvider(provider);
        futureConfirmed.setEventType(eventType);
        futureConfirmed.setClientName("Future Confirmed");
        futureConfirmed.setClientEmail("future@test.com");
        futureConfirmed.setStartTime(futureStart);
        futureConfirmed.setEndTime(futureStart.plusSeconds(1800));
        futureConfirmed.setBlockedUntil(futureStart.plusSeconds(2700));
        futureConfirmed.setStatus(BookingStatus.CONFIRMED);
        futureConfirmed = bookingRepository.save(futureConfirmed);

        bookingCompletionScheduler.completeExpiredBookings();

        BookingEntity updatedExpiredConfirmed = bookingRepository.findById(expiredConfirmed.getId()).orElseThrow();
        BookingEntity updatedExpiredCanceled = bookingRepository.findById(expiredCanceled.getId()).orElseThrow();
        BookingEntity updatedFutureConfirmed = bookingRepository.findById(futureConfirmed.getId()).orElseThrow();

        assertThat(updatedExpiredConfirmed.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(updatedExpiredConfirmed.getVersion()).isEqualTo(1L);
        assertThat(updatedExpiredCanceled.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(updatedFutureConfirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        mockMvc.perform(post("/provider/bookings/{id}/cancel", expiredConfirmed.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/guest/bookings/{cancelToken}/cancel", expiredConfirmed.getCancelToken()))
                .andExpect(status().isConflict());
    }

    private void clearRateLimitKeys() {
        Set<String> keys = redisTemplate.keys("ratelimit:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void saveDirectBooking(String clientEmail) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        BookingEntity booking = new BookingEntity();
        booking.setProvider(provider);
        booking.setEventType(eventType);
        booking.setClientName("Maria");
        booking.setClientEmail(clientEmail);
        booking.setStartTime(start);
        booking.setEndTime(start.plusSeconds(1800));
        booking.setBlockedUntil(start.plusSeconds(2700));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private void createMondayAvailability(LocalTime start, LocalTime end) {
        AvailabilityScheduleEntity schedule = new AvailabilityScheduleEntity();
        schedule.setProvider(provider);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        availabilityScheduleRepository.save(schedule);
    }

    private String bookSlotRaw(LocalTime time, String name, String email) throws Exception {
        Instant slot = getNextFutureMonday().atTime(time).atZone(providerZone).toInstant();
        return mockMvc.perform(post("/public/users/{userSlug}/events/{eventSlug}/book", provider.getSlug(), eventType.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(name, email, slot))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private UUID bookSlot(LocalTime time, String name, String email) throws Exception {
        String json = bookSlotRaw(time, name, email);
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    private LocalDate getNextFutureMonday() {
        LocalDate date = LocalDate.now(providerZone).plusDays(2);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}

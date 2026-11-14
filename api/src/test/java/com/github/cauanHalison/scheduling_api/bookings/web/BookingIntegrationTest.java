package com.github.cauanHalison.scheduling_api.bookings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cauanHalison.scheduling_api.TestcontainersConfiguration;
import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.config.security.JwtService;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingIntegrationTest {

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

    private ProviderEntity provider;
    private EventTypeEntity eventType;
    private String accessToken;
    private final ZoneId providerZone = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
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
                .andExpect(jsonPath("$.clientEmail").value("carlos@gmail.com"))
                .andReturn().getResponse().getContentAsString();

        UUID cancelToken = UUID.fromString(objectMapper.readTree(bookingJson).get("cancelToken").asText());
        UUID bookingId = UUID.fromString(objectMapper.readTree(bookingJson).get("id").asText());

        // 3. Verify provider listing
        mockMvc.perform(get("/provider/bookings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].clientName").value("Carlos Cliente"));

        // 4. Guest views booking by cancel token
        mockMvc.perform(get("/guest/bookings/{cancelToken}", cancelToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 5. Guest cancels booking
        mockMvc.perform(post("/guest/bookings/{cancelToken}/cancel", cancelToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Desisti"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

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
                .andExpect(jsonPath("$[0].clientEmail").value("maria@test.com"));
    }

    private LocalDate getNextFutureMonday() {
        LocalDate date = LocalDate.now(providerZone).plusDays(2);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}

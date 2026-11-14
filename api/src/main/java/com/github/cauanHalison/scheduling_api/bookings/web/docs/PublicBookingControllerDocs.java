package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Public Bookings", description = "Endpoints públicos de consulta de disponibilidade e agendamento")
public interface PublicBookingControllerDocs {

    @Operation(summary = "Vitrine de eventos do prestador", description = "Lista todos os tipos de eventos ativos configurados pelo prestador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de eventos ativos retornada com sucesso")
    })
    ResponseEntity<List<EventTypeResponse>> getProviderEvents(
            @Parameter(description = "Slug do prestador de serviço", example = "cauan-halison") String userSlug
    );

    @Operation(summary = "Detalhes públicos do evento", description = "Retorna os metadados do tipo de evento e do prestador para a página pública")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadados do evento recuperados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Evento ou prestador não encontrado")
    })
    ResponseEntity<PublicEventDetailsResponse> getPublicEventDetails(
            @Parameter(description = "Slug do prestador de serviço", example = "cauan-halison") String userSlug,
            @Parameter(description = "Slug do tipo de evento", example = "consultoria-30min") String eventSlug
    );

    @Operation(summary = "Dias disponíveis no mês", description = "Retorna a lista de datas com slots livres para o mês/ano informado no fuso do cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de datas disponíveis retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Tipo de evento ou prestador não encontrado")
    })
    ResponseEntity<AvailableDaysResponse> getAvailableDays(
            @Parameter(description = "Slug do prestador de serviço", example = "cauan-halison") String userSlug,
            @Parameter(description = "Slug do tipo de evento", example = "consultoria-30min") String eventSlug,
            @Parameter(description = "Mês desejado (1-12)", example = "11") Integer month,
            @Parameter(description = "Ano desejado", example = "2026") Integer year,
            @Parameter(description = "Fuso horário IANA do cliente", example = "America/Sao_Paulo") String timeZone
    );

    @Operation(summary = "Horários livres para uma data", description = "Calcula e retorna os slots de horários livres para um dia específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slots livres retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Tipo de evento ou prestador não encontrado")
    })
    ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @Parameter(description = "Slug do prestador de serviço", example = "cauan-halison") String userSlug,
            @Parameter(description = "Slug do tipo de evento", example = "consultoria-30min") String eventSlug,
            @Parameter(description = "Data no formato YYYY-MM-DD", example = "2026-11-10") LocalDate date,
            @Parameter(description = "Fuso horário IANA do cliente", example = "America/Sao_Paulo") String timeZone
    );

    @Operation(summary = "Criar agendamento público", description = "Reserva um horário livre com garantia anti double-booking")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agendamento confirmado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Tipo de evento ou prestador não encontrado"),
            @ApiResponse(responseCode = "409", description = "Horário indisponível ou conflito de concorrência simultânea")
    })
    ResponseEntity<BookingConfirmationResponse> book(
            @Parameter(description = "Slug do prestador de serviço", example = "cauan-halison") String userSlug,
            @Parameter(description = "Slug do tipo de evento", example = "consultoria-30min") String eventSlug,
            CreateBookingRequest request
    );
}

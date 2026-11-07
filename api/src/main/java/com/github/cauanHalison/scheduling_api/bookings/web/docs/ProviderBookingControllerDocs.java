package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Provider Bookings", description = "Endpoints de gestão de agendamentos para o prestador autenticado")
@SecurityRequirement(name = "bearerAuth")
public interface ProviderBookingControllerDocs {

    @Operation(summary = "Listar agendamentos", description = "Lista todos os agendamentos do prestador autenticado com filtros opcionais de status, datas e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de agendamentos retornada com sucesso")
    })
    ResponseEntity<Page<ProviderBookingResponse>> listBookings(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "Filtro por status (CONFIRMED ou CANCELED)") BookingStatus status,
            @Parameter(description = "Data de início mínima em UTC") Instant startDate,
            @Parameter(description = "Data de início máxima em UTC") Instant endDate,
            Pageable pageable
    );

    @Operation(summary = "Detalhes do agendamento", description = "Retorna os detalhes de um agendamento específico pertencente ao prestador autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    ResponseEntity<ProviderBookingResponse> getBookingById(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "ID do agendamento") UUID id
    );

    @Operation(summary = "Cancelar agendamento", description = "Cancela um agendamento com justificativa, liberando o horário imediatamente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Agendamento já estava cancelado")
    })
    ResponseEntity<ProviderBookingResponse> cancelBooking(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "ID do agendamento") UUID id,
            CancelBookingRequest request
    );
}

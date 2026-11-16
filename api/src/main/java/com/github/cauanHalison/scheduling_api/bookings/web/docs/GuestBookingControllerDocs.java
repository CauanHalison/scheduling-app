package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Guest Bookings", description = "Endpoints de autoatendimento para o convidado via token seguro e Magic Link")
public interface GuestBookingControllerDocs {

    @Operation(summary = "Consultar agendamento por token", description = "Recupera os detalhes completos do agendamento utilizando o token de cancelamento enviado no e-mail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento localizado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado para o token fornecido")
    })
    ResponseEntity<GuestBookingResponse> getBookingByCancelToken(
            @Parameter(description = "Token UUID seguro recebido pelo cliente") UUID cancelToken
    );

    @Operation(summary = "Cancelar agendamento por token", description = "Cancela a reunião utilizando o token seguro, liberando o horário para novos agendamentos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Agendamento já estava cancelado")
    })
    ResponseEntity<GuestBookingResponse> cancelBookingByToken(
            @Parameter(description = "Token UUID seguro recebido pelo cliente") UUID cancelToken,
            CancelBookingRequest request
    );

    @Operation(summary = "Solicitar Magic Link", description = "Gera um token temporário com validade de 15 minutos no Redis para consulta de todos os agendamentos vinculados ao e-mail informado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação processada com sucesso"),
            @ApiResponse(responseCode = "429", description = "Muitas solicitações. Veja o header Retry-After")
    })
    ResponseEntity<Void> requestMagicLink(MagicLinkRequest request, @Parameter(hidden = true) HttpServletRequest httpRequest);

    @Operation(summary = "Verificar Magic Link", description = "Valida o token temporário do Magic Link e retorna a lista de agendamentos associados ao e-mail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamentos listados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token expirado ou inválido")
    })
    ResponseEntity<List<GuestBookingResponse>> verifyMagicLink(
            @Parameter(description = "Token temporário do Magic Link recebido no link") String token
    );
}

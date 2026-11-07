package com.github.cauanHalison.scheduling_api.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.github.cauanHalison.scheduling_api.exception.dto.ErrorField;
import com.github.cauanHalison.scheduling_api.exception.dto.ErrorResponse;
import com.github.cauanHalison.scheduling_api.exception.exceptions.InvalidTokenException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.DateTimeException;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "uk_provider_email", "Este e-mail já está em uso.",
            "uk_provider_slug", "Este slug já está em uso.",
            "uk_event_type_provider_slug", "Este slug já está em uso.",
            "excl_availability_schedule_no_overlap", "Os horários informados se sobrepõem a outro já cadastrado.",
            "excl_schedule_override_no_overlap", "Já existe um bloqueio de agenda que se sobrepõe ao período informado.",
            "excl_booking_no_overlap", "Este horário acabou de ser reservado por outra pessoa."
    );

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed (bad credentials) on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        request.getRequestURI(),
                        "Email ou senha incorretos."
                ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid or expired token on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        request.getRequestURI(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Unauthorized access attempt on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        request.getRequestURI(),
                        "Autenticação necessária para acessar este recurso. Token ausente ou inválido."
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        request.getRequestURI(),
                        "Você não tem permissão para acessar este recurso."
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        request.getRequestURI(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleResourceConflictException(
            ResourceConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource conflict on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        log.warn("Optimistic locking conflict on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI(),
                        "Este agendamento foi atualizado simultaneamente. Tente novamente."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorField> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorField(error.getField(), error.getDefaultMessage()))
                .toList();

        log.warn("Field validation failed on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        "Erro de validação nos campos informados.",
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed JSON or unreadable message on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            String fieldName = invalidFormatEx.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .reduce((first, second) -> second)
                    .orElse("campo");

            String invalidValue = String.valueOf(invalidFormatEx.getValue());
            String targetType = invalidFormatEx.getTargetType().getSimpleName();

            String message = String.format("O valor '%s' é inválido para o campo '%s' (tipo esperado: %s).",
                    invalidValue, fieldName, targetType);

            List<ErrorField> fieldErrors = List.of(new ErrorField(fieldName, message));

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value(),
                            request.getRequestURI(),
                            "Erro de validação nos campos informados.",
                            fieldErrors
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        "Corpo da requisição mal formatado ou JSON com erro de sintaxe."
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String paramName = ex.getName();
        String message = String.format("O parâmetro '%s' recebeu um valor inválido.", paramName);

        log.warn("Type mismatch on param [{}] on route [{} {}] - IP [{}]: {}",
                paramName, request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        message
                ));
    }

    @ExceptionHandler(DateTimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleDateTimeException(
            DateTimeException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid date/time or timezone on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI(),
                        "O fuso horário, mês ou data informado é inválido."
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String rootMessage = ex.getMostSpecificCause().getMessage();

        log.warn("Data integrity violation on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), rootMessage);

        String message = CONSTRAINT_MESSAGES.entrySet().stream()
                .filter(entry -> rootMessage != null && rootMessage.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("A operação conflita com dados já existentes.");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI(),
                        message
                ));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on route [{} {}] - IP [{}]: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI(),
                        "Ocorreu um erro interno inesperado no servidor."
                ));
    }
}

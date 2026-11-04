package br.com.salgados.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.salgados.dto.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // O Spring escolhe o handler da classe mais específica: NotFound e Conflict
    // vencem BusinessException, que é o "resto" das regras de negócio.
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Há campos inválidos. Corrija-os e tente novamente.", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Requisição inválida: verifique o formato do JSON, os números e as unidades informadas.",
                Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Valor inválido para o parâmetro '" + ex.getName() + "'.", Map.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException ex) {
        // O detalhe técnico vai só para o log; o usuário recebe uma mensagem genérica.
        log.error("Falha de acesso ao banco de dados", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_OPERATION_FAILED",
                "Não foi possível concluir a operação no banco de dados.", Map.of());
    }

    private static ResponseEntity<ErrorResponse> build(
            HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), status.value(), code, message, fieldErrors));
    }
}
package com.missio.fluencia_leitora.common.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts exceptions into RFC 7807 {@link ProblemDetail} responses with a
 * {@code code} extension field, matching the error codes defined in the spec
 * (e.g. {@code ANO_LETIVO_DUPLICADO}, {@code TURMA_DUPLICADA}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problemDetail.setProperty("code", ex.getCode());
        ex.getDetails().forEach(problemDetail::setProperty);
        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.CONFLICT, "Conflito de versão ao salvar o registro");
        problemDetail.setProperty("code", "CONFLITO_DE_VERSAO");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorMap)
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Payload inválido");
        problemDetail.setProperty("code", "VALIDACAO_INVALIDA");
        problemDetail.setProperty("errors", fieldErrors);

        return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    private Map<String, String> toFieldErrorMap(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage());
    }
}

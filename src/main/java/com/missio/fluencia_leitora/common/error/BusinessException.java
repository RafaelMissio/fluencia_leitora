package com.missio.fluencia_leitora.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Generic runtime exception for business-rule violations (duplicates, invalid
 * references, invalid state transitions). Carries the HTTP status and the
 * domain error {@code code} that {@link GlobalExceptionHandler} copies into
 * the {@code ProblemDetail} response (e.g. {@code ANO_LETIVO_DUPLICADO}).
 *
 * <p>AD-008: an optional {@code details} map carries structured data beyond
 * the {@code code} (e.g. the positions of the invalid items for
 * {@code NAO_CANONICA_PROIBIDA_1_ANO}). Each entry is copied onto the
 * {@code ProblemDetail} as its own extension property.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public BusinessException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

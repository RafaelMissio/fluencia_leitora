package com.missio.fluencia_leitora.common.error;

import org.springframework.http.HttpStatus;

/**
 * Generic runtime exception for business-rule violations (duplicates, invalid
 * references, invalid state transitions). Carries the HTTP status and the
 * domain error {@code code} that {@link GlobalExceptionHandler} copies into
 * the {@code ProblemDetail} response (e.g. {@code ANO_LETIVO_DUPLICADO}).
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

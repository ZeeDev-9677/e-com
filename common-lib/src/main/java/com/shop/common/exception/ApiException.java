package com.shop.common.exception;

import org.springframework.http.HttpStatus;

/** Base type for all expected (business) errors. Mapped to HTTP by GlobalExceptionHandler. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}

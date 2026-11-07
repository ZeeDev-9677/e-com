package com.shop.common.exception;

import com.shop.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * One place that turns every exception into a stable JSON contract.
 * Rule: 4xx -> WARN without stack trace, 5xx -> ERROR with stack trace. Never leak internals to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        log.warn("api_error code={} message={}", ex.getCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldViolation> v = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldViolation(f.getField(), f.getDefaultMessage())).toList();
        log.warn("validation_failed violations={}", v.size());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, v);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiError.FieldViolation> v = ex.getConstraintViolations().stream()
                .map(c -> new ApiError.FieldViolation(c.getPropertyPath().toString(), c.getMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, v);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed request body", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("access_denied path={}", req.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission", req, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "Resource was modified concurrently, retry", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("data_integrity_violation cause={}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with existing data", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        // Spring MVC's own exceptions (404 no handler, 405, 415, missing header...) carry their status.
        if (ex instanceof ErrorResponse er) {
            HttpStatus s = HttpStatus.valueOf(er.getStatusCode().value());
            return build(s, "HTTP_" + s.value(), s.getReasonPhrase(), req, null);
        }
        log.error("unhandled_exception path={}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error. Quote traceId when contacting support.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus s, String code, String msg, HttpServletRequest req,
                                           List<ApiError.FieldViolation> v) {
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), code, msg,
                req.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY), v));
    }
}

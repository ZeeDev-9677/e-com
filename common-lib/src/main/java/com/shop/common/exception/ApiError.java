package com.shop.common.exception;

import java.time.Instant;
import java.util.List;

/** Uniform error payload. traceId == correlation id, so support can grep logs by it. */
public record ApiError(Instant timestamp, int status, String code, String message, String path,
                       String traceId, List<FieldViolation> errors) {
    public record FieldViolation(String field, String message) {}
}

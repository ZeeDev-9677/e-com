package com.shop.common.exception;

import org.springframework.http.HttpStatus;

/** Valid request, but violates a business rule (422). */
public class BusinessException extends ApiException {
    public BusinessException(String code, String message) { super(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
}

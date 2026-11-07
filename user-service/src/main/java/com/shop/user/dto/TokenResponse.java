package com.shop.user.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

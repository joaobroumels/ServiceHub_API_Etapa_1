package com.servicehub.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) { }

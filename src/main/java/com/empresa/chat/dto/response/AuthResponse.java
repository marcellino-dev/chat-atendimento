package com.empresa.chat.dto.response;

public record AuthResponse(
    String token,
    Long userId,
    String nome,
    String role
) {}

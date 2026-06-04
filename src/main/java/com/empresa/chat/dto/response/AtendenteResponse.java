package com.empresa.chat.dto.response;

public record AtendenteResponse(
    Long id,
    Long userId,
    String nome,
    String email,
    String status,
    String setor,
    Integer maxSimultaneous
) {}

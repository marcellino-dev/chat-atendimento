package com.empresa.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MensagemRequest(
    @NotBlank String conteudo,
    String tipo
) {}

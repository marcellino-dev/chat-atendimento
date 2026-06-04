package com.empresa.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetorRequest(
    @NotBlank @Size(min = 2, max = 100) String nome,
    String descricao
) {}

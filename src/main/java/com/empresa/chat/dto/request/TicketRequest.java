package com.empresa.chat.dto.request;

import com.empresa.chat.domain.enums.CanalOrigem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketRequest(
    @NotBlank String clienteNome,
    String clienteTel,
    @NotNull Long setorId,
    CanalOrigem canal
) {}

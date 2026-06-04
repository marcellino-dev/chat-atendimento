package com.empresa.chat.dto.request;

import com.empresa.chat.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 120) String nome,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String senha,
    Role role
) {}

package com.empresa.chat.controller;

import com.empresa.chat.dto.request.LoginRequest;
import com.empresa.chat.dto.request.RegisterRequest;
import com.empresa.chat.dto.response.AuthResponse;
import com.empresa.chat.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e registro de usuários")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    @Operation(summary = "Registra novo usuário (apenas ADMIN)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }
}

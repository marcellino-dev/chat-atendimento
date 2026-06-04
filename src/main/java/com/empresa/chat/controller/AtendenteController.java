package com.empresa.chat.controller;

import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.dto.response.AtendenteResponse;
import com.empresa.chat.service.atendimento.AtendenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atendentes")
@RequiredArgsConstructor
@Tag(name = "Atendentes", description = "Gerenciamento de atendentes")
public class AtendenteController {

    private final AtendenteService atendenteService;

    @GetMapping
    @Operation(summary = "Lista todos os atendentes")
    public List<AtendenteResponse> listar() {
        return atendenteService.listar();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria novo atendente (apenas ADMIN)")
    public AtendenteResponse criar(@RequestBody CriarAtendenteRequest req) {
        return atendenteService.criar(req.nome(), req.email(), req.senha(), req.setorId());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza status do atendente (online/offline/ausente)")
    public AtendenteResponse atualizarStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        return atendenteService.atualizarStatus(id, StatusAtendente.valueOf(req.status()));
    }

    record CriarAtendenteRequest(String nome, String email, String senha, Long setorId) {}
    record StatusRequest(String status) {}
}

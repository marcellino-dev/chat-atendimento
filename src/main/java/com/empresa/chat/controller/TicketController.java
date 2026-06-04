package com.empresa.chat.controller;

import com.empresa.chat.dto.request.TicketRequest;
import com.empresa.chat.dto.response.TicketResponse;
import com.empresa.chat.service.atendimento.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Gerenciamento de tickets de atendimento")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/meus")
    @Operation(summary = "Lista tickets do atendente logado")
    public List<TicketResponse> meusTickets() {
        return ticketService.listarMeusTickets();
    }

    @GetMapping("/fila")
    @Operation(summary = "Lista tickets aguardando na fila")
    public List<TicketResponse> fila(@RequestParam(required = false) Long setorId) {
        return ticketService.listarFila(setorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria novo ticket (entrada de cliente)")
    public TicketResponse criar(@Valid @RequestBody TicketRequest req) {
        return ticketService.criarTicket(req);
    }

    @PostMapping("/{id}/assumir")
    @Operation(summary = "Atendente assume o ticket")
    public TicketResponse assumir(@PathVariable Long id) {
        return ticketService.assumirTicket(id);
    }

    @PostMapping("/{id}/fechar")
    @Operation(summary = "Finaliza o atendimento")
    public ResponseEntity<Void> fechar(@PathVariable Long id) {
        ticketService.fecharTicket(id);
        return ResponseEntity.noContent().build();
    }
}

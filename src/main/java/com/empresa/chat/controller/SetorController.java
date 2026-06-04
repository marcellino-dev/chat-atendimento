package com.empresa.chat.controller;

import com.empresa.chat.domain.model.Setor;
import com.empresa.chat.dto.request.SetorRequest;
import com.empresa.chat.dto.response.SetorResponse;
import com.empresa.chat.exception.BusinessException;
import com.empresa.chat.exception.NotFoundException;
import com.empresa.chat.repository.SetorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setores")
@RequiredArgsConstructor
@Tag(name = "Setores", description = "Gerenciamento de setores de atendimento")
public class SetorController {

    private final SetorRepository setorRepository;

    @GetMapping
    @Operation(summary = "Lista todos os setores ativos")
    public List<SetorResponse> listar() {
        return setorRepository.findByAtivoTrue().stream()
                .map(s -> new SetorResponse(s.getId(), s.getNome(), s.getDescricao(), s.getAtivo()))
                .toList();
    }

    @GetMapping("/{id}")
    public SetorResponse buscar(@PathVariable Long id) {
        var s = setorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Setor", id));
        return new SetorResponse(s.getId(), s.getNome(), s.getDescricao(), s.getAtivo());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SetorResponse criar(@Valid @RequestBody SetorRequest req) {
        if (setorRepository.existsByNomeIgnoreCase(req.nome()))
            throw new BusinessException("Setor com nome '" + req.nome() + "' já existe");
        var setor = Setor.builder().nome(req.nome()).descricao(req.descricao()).build();
        var saved = setorRepository.save(setor);
        return new SetorResponse(saved.getId(), saved.getNome(), saved.getDescricao(), saved.getAtivo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest req) {
        var setor = setorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Setor", id));
        setor.setNome(req.nome());
        setor.setDescricao(req.descricao());
        var saved = setorRepository.save(setor);
        return new SetorResponse(saved.getId(), saved.getNome(), saved.getDescricao(), saved.getAtivo());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        var setor = setorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Setor", id));
        setor.setAtivo(false);
        setorRepository.save(setor);
        return ResponseEntity.noContent().build();
    }
}

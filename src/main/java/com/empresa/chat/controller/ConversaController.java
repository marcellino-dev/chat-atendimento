package com.empresa.chat.controller;

import com.empresa.chat.domain.enums.RemetenteTipo;
import com.empresa.chat.domain.enums.TipoMensagem;
import com.empresa.chat.domain.model.Mensagem;
import com.empresa.chat.dto.request.MensagemRequest;
import com.empresa.chat.dto.response.MensagemResponse;
import com.empresa.chat.exception.NotFoundException;
import com.empresa.chat.repository.ConversaRepository;
import com.empresa.chat.repository.MensagemRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversas")
@RequiredArgsConstructor
@Tag(name = "Conversas", description = "Mensagens por conversa")
public class ConversaController {

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;

    @GetMapping("/{conversaId}/mensagens")
    public List<MensagemResponse> listarMensagens(@PathVariable Long conversaId) {
        return mensagemRepository.findByConversaIdOrderByCreatedAtAsc(conversaId)
            .stream().map(MensagemResponse::from).toList();
    }

    @PostMapping("/{conversaId}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse enviarMensagem(@PathVariable Long conversaId,
                                           @Valid @RequestBody MensagemRequest req) {
        var conversa = conversaRepository.findById(conversaId)
            .orElseThrow(() -> NotFoundException.of("Conversa", conversaId));

        var mensagem = Mensagem.builder()
            .conversa(conversa)
            .remetente(RemetenteTipo.ATENDENTE)
            .conteudo(req.conteudo())
            .tipo(req.tipo() != null ? TipoMensagem.valueOf(req.tipo()) : TipoMensagem.TEXTO)
            .build();
        return MensagemResponse.from(mensagemRepository.save(mensagem));
    }
}

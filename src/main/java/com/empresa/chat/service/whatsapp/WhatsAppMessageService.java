package com.empresa.chat.service.whatsapp;

import com.empresa.chat.domain.enums.CanalOrigem;
import com.empresa.chat.domain.enums.RemetenteTipo;
import com.empresa.chat.domain.enums.StatusConversa;
import com.empresa.chat.domain.enums.StatusTicket;
import com.empresa.chat.domain.model.Conversa;
import com.empresa.chat.domain.model.Mensagem;
import com.empresa.chat.domain.model.Setor;
import com.empresa.chat.domain.model.Ticket;
import com.empresa.chat.repository.ConversaRepository;
import com.empresa.chat.repository.MensagemRepository;
import com.empresa.chat.repository.SetorRepository;
import com.empresa.chat.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMessageService {

    private final ConversaRepository conversaRepository;
    private final TicketRepository ticketRepository;
    private final MensagemRepository mensagemRepository;
    private final SetorRepository setorRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processarMensagem(String telefone, String nome, String mensagem, String remetente) {
        Conversa conversa = buscarOuCriarConversa(telefone, nome);

        Mensagem msg = Mensagem.builder()
                .conversa(conversa)
                .remetente(converterRemetente(remetente))
                .conteudo(mensagem)
                .build();
        mensagemRepository.save(msg);

        conversa.setUpdatedAt(java.time.LocalDateTime.now());
        conversaRepository.save(conversa);

        if (remetente.equals("CLIENTE")) {
            notificarFrontendTicket(conversa, nome, telefone, mensagem);
        }
    }

    private Conversa buscarOuCriarConversa(String telefone, String nome) {
        Optional<Conversa> conversaOpt = conversaRepository.findAll().stream()
                .filter(c -> telefone.equals(c.getClienteTel()) &&
                        c.getStatus() != StatusConversa.ENCERRADA)
                .findFirst();

        if (conversaOpt.isPresent()) {
            return conversaOpt.get();
        }

        // Busca um setor padrão (ex: "Geral" ou o primeiro disponível)
        Setor setorPadrao = setorRepository.findByAtivoTrue().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum setor ativo encontrado"));

        String protocolo = "CHAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Conversa conversa = Conversa.builder()
                .protocolo(protocolo)
                .canal(CanalOrigem.WHATSAPP)
                .clienteNome(nome)
                .clienteTel(telefone)
                .setor(setorPadrao)
                .status(StatusConversa.AGUARDANDO)
                .build();
        conversa = conversaRepository.save(conversa);

        Ticket ticket = Ticket.builder()
                .conversa(conversa)
                .setor(setorPadrao)
                .status(StatusTicket.ABERTO)
                .build();
        ticket = ticketRepository.save(ticket);

        log.info("Nova conversa criada: {} - {} com setor: {}", nome, telefone, setorPadrao.getNome());

        Map<String, Object> ticketMsg = Map.of(
                "id", ticket.getId(),
                "protocolo", conversa.getProtocolo(),
                "clienteNome", nome,
                "clienteTel", telefone,
                "setor", setorPadrao.getNome(),
                "status", "ABERTO",
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/fila", ticketMsg);

        return conversa;
    }

    private void notificarFrontendTicket(Conversa conversa, String nome, String telefone, String mensagem) {
        Map<String, Object> ticketMsg = Map.of(
                "id", conversa.getId(),
                "protocolo", conversa.getProtocolo(),
                "clienteNome", nome,
                "clienteTel", telefone,
                "ultimaMensagem", mensagem,
                "status", conversa.getStatus().toString(),
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/fila", ticketMsg);
    }

    private RemetenteTipo converterRemetente(String remetente) {
        switch (remetente) {
            case "CLIENTE": return RemetenteTipo.CLIENTE;
            case "ATENDENTE": return RemetenteTipo.ATENDENTE;
            case "BOT": return RemetenteTipo.BOT;
            default: return RemetenteTipo.CLIENTE;
        }
    }
}
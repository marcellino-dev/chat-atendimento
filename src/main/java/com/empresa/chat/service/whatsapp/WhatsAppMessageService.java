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

import java.time.LocalDateTime;
import java.util.List;
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
    public void processarMensagem(String jid, String nome, String mensagem, String remetente) {
        Conversa conversa = buscarOuCriarConversa(jid, nome);

        Mensagem msg = Mensagem.builder()
                .conversa(conversa)
                .remetente(converterRemetente(remetente))
                .conteudo(mensagem)
                .build();
        mensagemRepository.save(msg);

        // Usa o @PreUpdate da entidade — basta salvar
        conversaRepository.save(conversa);

        if ("CLIENTE".equals(remetente)) {
            notificarFrontend(conversa, nome, jid, mensagem);
        }
    }

    /**
     * Migra conversas criadas com LID temporário para o JID real.
     * Chamado quando o contacts.upsert resolve o mapeamento LID→JID.
     *
     * Atualiza clienteTel de todas as conversas não encerradas que ainda
     * usam o LID como identificador, para que as próximas mensagens do
     * cliente (que já chegam pelo JID real) sejam associadas à mesma conversa.
     */
    @Transactional
    public void migrarClienteTel(String lidAntigo, String jidNovo) {
        List<Conversa> conversas = conversaRepository.findByClienteTelAndStatus(
                lidAntigo, StatusConversa.AGUARDANDO);

        // Inclui conversas EM_ATENDIMENTO também
        List<Conversa> emAtendimento = conversaRepository.findByClienteTelAndStatus(
                lidAntigo, StatusConversa.EM_ATENDIMENTO);

        conversas.addAll(emAtendimento);

        if (conversas.isEmpty()) {
            log.debug("Nenhuma conversa ativa com LID {} para migrar", lidAntigo);
            return;
        }

        for (Conversa c : conversas) {
            c.setClienteTel(jidNovo);
            conversaRepository.save(c);
            log.info("🔄 Conversa {} migrada: {} → {}", c.getProtocolo(), lidAntigo, jidNovo);
        }
    }

    private Conversa buscarOuCriarConversa(String jid, String nome) {
        // Busca direta por JID — sem findAll()
        Optional<Conversa> existente = conversaRepository
                .findByClienteTelAndStatusNot(jid, StatusConversa.ENCERRADA);

        if (existente.isPresent()) {
            Conversa c = existente.get();
            // Atualiza nome se estava genérico
            if ("Cliente".equals(c.getClienteNome()) && nome != null && !nome.equals("Cliente")) {
                c.setClienteNome(nome);
                conversaRepository.save(c);
            }
            return c;
        }

        // Cria nova conversa
        Setor setorPadrao = setorRepository.findByAtivoTrue().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum setor ativo encontrado"));

        String protocolo = "CHAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Conversa conversa = Conversa.builder()
                .protocolo(protocolo)
                .canal(CanalOrigem.WHATSAPP)
                .clienteNome(nome)
                .clienteTel(jid)
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

        log.info("📋 Nova conversa: protocolo={} | cliente={} | jid={}", protocolo, nome, jid);

        messagingTemplate.convertAndSend("/topic/fila", Map.of(
                "id",          ticket.getId(),
                "protocolo",   conversa.getProtocolo(),
                "clienteNome", nome,
                "clienteTel",  jid,
                "setor",       setorPadrao.getNome(),
                "status",      "ABERTO",
                "timestamp",   System.currentTimeMillis()
        ));

        return conversa;
    }

    private void notificarFrontend(Conversa conversa, String nome, String jid, String mensagem) {
        messagingTemplate.convertAndSend("/topic/fila", Map.of(
                "id",             conversa.getId(),
                "protocolo",      conversa.getProtocolo(),
                "clienteNome",    nome,
                "clienteTel",     jid,
                "ultimaMensagem", mensagem,
                "status",         conversa.getStatus().toString(),
                "timestamp",      System.currentTimeMillis()
        ));
    }

    private RemetenteTipo converterRemetente(String remetente) {
        return switch (remetente) {
            case "ATENDENTE" -> RemetenteTipo.ATENDENTE;
            case "BOT"       -> RemetenteTipo.BOT;
            default          -> RemetenteTipo.CLIENTE;
        };
    }
}
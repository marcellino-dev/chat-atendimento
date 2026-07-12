package com.empresa.chat.service.bot;

import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.domain.enums.StatusConversa;
import com.empresa.chat.domain.enums.StatusTicket;
import com.empresa.chat.domain.model.Atendente;
import com.empresa.chat.domain.model.Conversa;
import com.empresa.chat.domain.model.Setor;
import com.empresa.chat.domain.model.Ticket;
import com.empresa.chat.repository.AtendenteRepository;
import com.empresa.chat.repository.ConversaRepository;
import com.empresa.chat.repository.SetorRepository;
import com.empresa.chat.repository.TicketRepository;
import com.empresa.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {

    private final AtendenteRepository atendenteRepository;
    private final SetorRepository setorRepository;
    private final UserRepository userRepository;
    private final ConversaRepository conversaRepository;
    private final TicketRepository ticketRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // ── Prefixos Redis ──────────────────────────────────────────────────────
    // Estado do fluxo do bot por cliente (TTL 1 hora de inatividade)
    private static final String PREFIX_ETAPA        = "bot:etapa:";
    private static final String PREFIX_SETOR        = "bot:setor:";
    private static final String PREFIX_ULTIMA_MSG   = "bot:ultimamsg:";
    private static final String PREFIX_FINALIZADO   = "bot:finalizado:";

    private static final Duration TTL_SESSAO        = Duration.ofHours(1);
    private static final Duration TTL_FINALIZADO    = Duration.ofHours(24);

    // =========================================================================
    // ENTRADA PRINCIPAL
    // =========================================================================

    /**
     * Processa a mensagem do cliente e retorna a resposta do bot,
     * ou "em_atendimento" se o cliente já foi encaminhado para um atendente,
     * ou null se a mensagem deve ser ignorada.
     */
    public String processarMensagem(String jid, String nome, String mensagem) {

        // Atendimento já encerrado — bot não interfere mais
        if (Boolean.parseBoolean(redisTemplate.opsForValue().get(PREFIX_FINALIZADO + jid))) {
            log.debug("Bot inativo para {} (atendimento finalizado)", jid);
            return "em_atendimento";
        }

        String msgLower = mensagem.toLowerCase().trim();

        // Deduplicação: evita processar a mesma mensagem duas vezes seguidas
        String ultimaMsg = redisTemplate.opsForValue().get(PREFIX_ULTIMA_MSG + jid);
        if (mensagem.equals(ultimaMsg)) {
            log.debug("Mensagem repetida ignorada para {}", jid);
            return null;
        }
        redisTemplate.opsForValue().set(PREFIX_ULTIMA_MSG + jid, mensagem, TTL_SESSAO);

        int etapa = getEtapa(jid);
        log.info("🤖 Bot | jid={} | etapa={} | msg={}", jid, etapa, mensagem);

        // Comandos globais de reset
        if (msgLower.equals("menu") || msgLower.equals("sair") || msgLower.equals("0")) {
            resetar(jid);
            return menuPrincipal(nome);
        }

        return switch (etapa) {
            case 0 -> {
                setEtapa(jid, 1);
                yield menuPrincipal(nome);
            }
            case 1 -> processarEscolhaSetor(jid, nome, mensagem);
            case 2 -> processarEscolhaAtendente(jid, nome, mensagem);
            default -> "em_atendimento";
        };
    }

    // =========================================================================
    // ETAPAS DO FLUXO
    // =========================================================================

    private String processarEscolhaSetor(String jid, String nome, String mensagem) {
        List<Setor> setores = setorRepository.findByAtivoTrue();
        Long setorId = mapearOpcao(mensagem.trim(), setores.size());

        if (setorId == null) {
            return "❌ Opção inválida! Digite um número de *1 a " + setores.size() + "*.\n\n"
                    + menuPrincipal(nome);
        }

        Setor setor = setorRepository.findById(setorId).orElse(null);
        if (setor == null || !setor.getAtivo()) {
            return "❌ Setor não encontrado.\n\n" + menuPrincipal(nome);
        }

        List<Atendente> atendentes = atendenteRepository.findBySetorIdAndStatus(
                setorId, StatusAtendente.ONLINE);

        if (atendentes.isEmpty()) {
            return "⚠️ Nenhum atendente disponível para *" + setor.getNome() + "* no momento.\n\n"
                    + "Digite outro número ou *MENU* para voltar.";
        }

        setSetorEscolhido(jid, setorId);
        setEtapa(jid, 2);
        return listaAtendentes(atendentes, setor.getNome());
    }

    private String processarEscolhaAtendente(String jid, String nome, String mensagem) {
        Long setorId = getSetorEscolhido(jid);
        if (setorId == null) {
            resetar(jid);
            return menuPrincipal(nome);
        }

        List<Atendente> atendentes = atendenteRepository.findBySetorIdAndStatus(
                setorId, StatusAtendente.ONLINE);

        int opcao;
        try {
            opcao = Integer.parseInt(mensagem.trim());
        } catch (NumberFormatException e) {
            Setor setor = setorRepository.findById(setorId).orElse(null);
            return "❌ Digite apenas o *número* do atendente.\n\n"
                    + listaAtendentes(atendentes, setor != null ? setor.getNome() : "setor");
        }

        if (opcao < 1 || opcao > atendentes.size()) {
            Setor setor = setorRepository.findById(setorId).orElse(null);
            return "❌ Opção inválida! Escolha entre 1 e " + atendentes.size() + ".\n\n"
                    + listaAtendentes(atendentes, setor != null ? setor.getNome() : "setor");
        }

        Atendente atendente = atendentes.get(opcao - 1);
        String nomeAtendente = userRepository.findById(atendente.getUser().getId())
                .map(u -> u.getNome())
                .orElse("Atendente");

        Setor setor = setorRepository.findById(setorId).orElse(null);
        String nomeSetor = setor != null ? setor.getNome() : "";

        // Aplica de fato a escolha no Ticket/Conversa (antes só existia na resposta do bot)
        boolean transferido = transferirParaAtendente(jid, setor, atendente);

        if (!transferido) {
            log.warn("⚠️ Não foi possível localizar conversa/ticket ativo para transferir: jid={}", jid);
        }

        // Marca atendimento finalizado (bot para de responder)
        redisTemplate.opsForValue().set(PREFIX_FINALIZADO + jid, "true", TTL_FINALIZADO);
        resetar(jid);

        log.info("✅ Bot encaminhou {} para atendente {} / setor {}", jid, nomeAtendente, nomeSetor);

        return "✅ Você será atendido por *" + nomeAtendente + "* do setor *" + nomeSetor + "*.\n\n"
                + "⏳ Aguarde, o atendente irá responder em breve.\n\n"
                + "Digite *MENU* a qualquer momento para reiniciar.";
    }

    // =========================================================================
    // TRANSFERÊNCIA REAL (Ticket + Conversa) — corrige o gap entre bot e banco
    // =========================================================================

    /**
     * Atualiza a Conversa e o Ticket ativos do cliente para refletir a escolha
     * feita no fluxo do bot: setor correto, atendente atribuído diretamente,
     * e status já como EM_ATENDIMENTO (pula a fila, pois o cliente escolheu
     * explicitamente o atendente).
     */
    @Transactional
    private boolean transferirParaAtendente(String jid, Setor setor, Atendente atendente) {
        if (setor == null) return false;

        Conversa conversa = conversaRepository
                .findByClienteTelAndStatusNot(jid, StatusConversa.ENCERRADA)
                .orElse(null);

        if (conversa == null) return false;

        conversa.setSetor(setor);
        conversa.setAtendente(atendente);
        conversa.setStatus(StatusConversa.EM_ATENDIMENTO);
        conversaRepository.save(conversa);

        ticketRepository.findByConversaId(conversa.getId()).ifPresent(ticket -> {
            ticket.setSetor(setor);
            ticket.setAtendente(atendente);
            ticket.setStatus(StatusTicket.EM_ATENDIMENTO);
            ticket.setAssumidoAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            notificarTransferencia(ticket, conversa, atendente, setor);
        });

        return true;
    }

    private void notificarTransferencia(Ticket ticket, Conversa conversa, Atendente atendente, Setor setor) {
        try {
            messagingTemplate.convertAndSend("/topic/fila", Map.of(
                    "id",             ticket.getId(),
                    "protocolo",      conversa.getProtocolo(),
                    "clienteNome",    conversa.getClienteNome(),
                    "clienteTel",     conversa.getClienteTel(),
                    "setor",          setor.getNome(),
                    "atendenteId",    atendente.getId(),
                    "status",         "EM_ATENDIMENTO",
                    "timestamp",      System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.warn("Erro ao notificar frontend sobre transferência: {}", e.getMessage());
        }
    }

    // =========================================================================
    // MENSAGENS DO BOT
    // =========================================================================

    private String menuPrincipal(String nome) {
        List<Setor> setores = setorRepository.findByAtivoTrue();
        StringBuilder sb = new StringBuilder();
        sb.append("👋 Olá, *").append(nome).append("!* Bem-vindo ao atendimento.\n\n");
        sb.append("Escolha uma opção:\n\n");
        for (int i = 0; i < setores.size(); i++) {
            sb.append("*").append(i + 1).append("* - ").append(setores.get(i).getNome()).append("\n");
        }
        sb.append("\n📌 Digite o *número* da opção desejada.\n");
        sb.append("🔁 Digite *MENU* para reiniciar.");
        return sb.toString();
    }

    private String listaAtendentes(List<Atendente> atendentes, String nomeSetor) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Atendentes disponíveis - ").append(nomeSetor).append("*\n\n");
        for (int i = 0; i < atendentes.size(); i++) {
            String nomeAt = userRepository.findById(atendentes.get(i).getUser().getId())
                    .map(u -> u.getNome()).orElse("Atendente");
            sb.append("*").append(i + 1).append("* - ").append(nomeAt).append("\n");
        }
        sb.append("\n📌 Digite o *número* do atendente.\n");
        sb.append("🔁 Digite *MENU* para voltar.");
        return sb.toString();
    }

    // =========================================================================
    // HELPERS REDIS
    // =========================================================================

    private int getEtapa(String jid) {
        String val = redisTemplate.opsForValue().get(PREFIX_ETAPA + jid);
        if (val == null) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    private void setEtapa(String jid, int etapa) {
        redisTemplate.opsForValue().set(PREFIX_ETAPA + jid, String.valueOf(etapa), TTL_SESSAO);
    }

    private Long getSetorEscolhido(String jid) {
        String val = redisTemplate.opsForValue().get(PREFIX_SETOR + jid);
        if (val == null) return null;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return null; }
    }

    private void setSetorEscolhido(String jid, Long setorId) {
        redisTemplate.opsForValue().set(PREFIX_SETOR + jid, String.valueOf(setorId), TTL_SESSAO);
    }

    private void resetar(String jid) {
        redisTemplate.delete(List.of(
                PREFIX_ETAPA      + jid,
                PREFIX_SETOR      + jid,
                PREFIX_ULTIMA_MSG + jid
        ));
    }

    /**
     * Mapeia a opção digitada (ex: "2") para o ID do item na posição correspondente.
     * Retorna null se fora do range ou não numérico.
     */
    private Long mapearOpcao(String opcao, int totalItens) {
        try {
            int num = Integer.parseInt(opcao);
            if (num < 1 || num > totalItens) return null;
            List<Setor> setores = setorRepository.findByAtivoTrue();
            if (num > setores.size()) return null;
            return setores.get(num - 1).getId();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =========================================================================
    // API PÚBLICA — permite que outros serviços reativem o bot (ex: ao encerrar
    // conversa pelo painel, o atendente pode "liberar" o cliente para o bot)
    // =========================================================================

    /** Reativa o bot para o cliente (ex: atendente encerrou o atendimento) */
    public void reativarBot(String jid) {
        redisTemplate.delete(PREFIX_FINALIZADO + jid);
        resetar(jid);
        log.info("🔁 Bot reativado para: {}", jid);
    }

    /** Verifica se o bot está ativo para o cliente */
    public boolean isBotAtivo(String jid) {
        return !Boolean.parseBoolean(redisTemplate.opsForValue().get(PREFIX_FINALIZADO + jid));
    }
}
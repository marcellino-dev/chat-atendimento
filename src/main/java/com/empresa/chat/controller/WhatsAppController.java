package com.empresa.chat.controller;

import com.empresa.chat.service.bot.BotService;
import com.empresa.chat.service.whatsapp.WhatsAppMessageService;
import com.empresa.chat.service.whatsapp.WhatsAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp", description = "Webhook e integração API oficial (Meta Cloud API)")
public class WhatsAppController {

    private final BotService botService;
    private final WhatsAppService whatsAppService;
    private final WhatsAppMessageService whatsAppMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.whatsapp.verify-token}")
    private String verifyToken;

    private static final String PREFIX_MSG = "wpp:msg:"; // dedup mensagens TTL 5min

    // -------------------------------------------------------------------------
    // GET /webhook  (verificação exigida pela Meta ao cadastrar o webhook)
    // -------------------------------------------------------------------------
    @GetMapping("/webhook")
    public ResponseEntity<String> webhookGet(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("✅ Webhook verificado com sucesso pela Meta");
            return ResponseEntity.ok(challenge);
        }
        log.warn("⚠️ Falha na verificação do webhook: token inválido");
        return ResponseEntity.status(403).body("Forbidden");
    }

    @GetMapping
    public ResponseEntity<String> base() {
        return ok();
    }

    // -------------------------------------------------------------------------
    // POST /webhook  (mensagens recebidas da Meta)
    // -------------------------------------------------------------------------
    @PostMapping("/webhook")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries == null) return ok();

            for (Map<String, Object> entry : entries) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    if (messages == null) continue; // pode ser evento de "statuses" (entregue/lido) — ignora

                    List<Map<String, Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
                    String nomeCliente = extrairNome(contacts);

                    for (Map<String, Object> message : messages) {
                        processarMensagemRecebida(message, nomeCliente);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Erro ao processar webhook: {}", e.getMessage(), e);
        }
        return ok();
    }

    private void processarMensagemRecebida(Map<String, Object> message, String nomeCliente) {
        String messageId = (String) message.get("id");
        if (messageId != null && !marcarProcessada(messageId)) {
            log.debug("Mensagem duplicada ignorada: {}", messageId);
            return;
        }

        String telefone = (String) message.get("from");
        if (telefone == null) return;

        String texto = extrairTexto(message);
        if (texto == null || texto.isBlank()) return;

        log.info("📨 {} | telefone={} | msg={}", nomeCliente, telefone, texto);
        processarMensagemCompleta(telefone, nomeCliente, texto);
    }

    private void processarMensagemCompleta(String telefone, String nomeCliente, String texto) {
        whatsAppMessageService.processarMensagem(telefone, nomeCliente, texto, "CLIENTE");
        enviarParaFrontend(telefone, nomeCliente, texto, "CLIENTE");

        String resposta = botService.processarMensagem(telefone, nomeCliente, texto);

        if (resposta != null && !"em_atendimento".equals(resposta)) {
            whatsAppService.enviarMensagem(telefone, resposta);
            whatsAppMessageService.processarMensagem(telefone, "Bot", resposta, "BOT");
            enviarParaFrontend(telefone, "Bot", resposta, "BOT");
        }
    }

    // -------------------------------------------------------------------------
    // POST /responder
    // -------------------------------------------------------------------------
    @PostMapping("/responder")
    public ResponseEntity<String> responderCliente(@RequestBody Map<String, String> req) {
        String telefone      = req.get("telefone");
        String mensagem      = req.get("mensagem");
        String atendenteNome = req.get("atendenteNome");

        if (telefone == null || mensagem == null) {
            return ResponseEntity.badRequest().body("telefone e mensagem são obrigatórios");
        }

        log.info("💬 Atendente {} → {}", atendenteNome, telefone);
        whatsAppService.enviarMensagem(telefone, mensagem);
        whatsAppMessageService.processarMensagem(telefone, atendenteNome, mensagem, "ATENDENTE");
        enviarParaFrontend(telefone, atendenteNome, mensagem, "ATENDENTE");
        return ok();
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(whatsAppService.verificarStatus());
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // =========================================================================
    // DEDUPLICAÇÃO VIA REDIS
    // =========================================================================

    private boolean marcarProcessada(String messageId) {
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(PREFIX_MSG + messageId, "1", Duration.ofMinutes(5));
        return Boolean.TRUE.equals(isNew);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    @SuppressWarnings("unchecked")
    private String extrairNome(List<Map<String, Object>> contacts) {
        if (contacts == null || contacts.isEmpty()) return "Cliente";
        Map<String, Object> profile = (Map<String, Object>) contacts.get(0).get("profile");
        if (profile == null) return "Cliente";
        String nome = (String) profile.get("name");
        return (nome != null && !nome.isBlank()) ? nome : "Cliente";
    }

    @SuppressWarnings("unchecked")
    private String extrairTexto(Map<String, Object> message) {
        String tipo = (String) message.get("type");
        if (tipo == null) return null;

        if ("text".equals(tipo)) {
            Map<String, Object> text = (Map<String, Object>) message.get("text");
            return text != null ? (String) text.get("body") : null;
        }

        for (String midiaTipo : List.of("image", "video", "document", "audio")) {
            if (midiaTipo.equals(tipo)) {
                Map<String, Object> media = (Map<String, Object>) message.get(midiaTipo);
                String caption = media != null ? (String) media.get("caption") : null;
                if (caption != null && !caption.isBlank()) return caption;
                return "[" + midiaTipo.toUpperCase() + "]";
            }
        }

        return null;
    }

    private void enviarParaFrontend(String telefone, String nome, String mensagem, String remetente) {
        try {
            messagingTemplate.convertAndSend("/topic/whatsapp-mensagens", Map.of(
                    "jid",       telefone,
                    "nome",      nome,
                    "mensagem",  mensagem,
                    "remetente", remetente,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.warn("Erro WebSocket frontend: {}", e.getMessage());
        }
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok("OK");
    }
}
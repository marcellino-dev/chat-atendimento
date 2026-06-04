package com.empresa.chat.controller;

import com.empresa.chat.service.bot.BotService;
import com.empresa.chat.service.whatsapp.WhatsAppService;
import com.empresa.chat.service.whatsapp.WhatsAppMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp", description = "Webhook e integração Evolution API")
public class WhatsAppController {

    private final BotService botService;
    private final WhatsAppService whatsAppService;
    private final WhatsAppMessageService whatsAppMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    private final String NUMERO_EMPRESA = "554789299801";
    private final String NUMERO_CLIENTE_TESTE = "5591991552191";
    private final Map<String, Long> ultimaMensagemTempo = new ConcurrentHashMap<>();
    private final Map<String, String> ultimoMessageId = new ConcurrentHashMap<>();

    @GetMapping("/webhook")
    public ResponseEntity<String> webhookGet(
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        log.info("Webhook GET chamado");
        if (challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
        log.info("=== WEBHOOK POST RECEBIDO ===");
        log.info("Payload: {}", payload);

        try {
            String event = (String) payload.get("event");
            if (event == null || !event.equals("messages.upsert")) {
                return ResponseEntity.ok("OK");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null) return ResponseEntity.ok("OK");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) data.get("message");
            @SuppressWarnings("unchecked")
            Map<String, Object> key = (Map<String, Object>) data.get("key");

            if (message == null || key == null) return ResponseEntity.ok("OK");

            Boolean fromMe = (Boolean) key.get("fromMe");
            if (Boolean.TRUE.equals(fromMe)) {
                log.info("Mensagem enviada por nós mesmos, ignorando");
                return ResponseEntity.ok("OK");
            }

            // EXTRAI NÚMERO DO CLIENTE COM FALLBACK
            String telefoneCliente = extrairNumeroCliente(payload, key);

            // SE NÃO CONSEGUIU EXTRAIR, USA NÚMERO DE TESTE
            if (telefoneCliente == null || telefoneCliente.equals(NUMERO_EMPRESA)) {
                log.warn("Usando número de teste fallback: {}", NUMERO_CLIENTE_TESTE);
                telefoneCliente = NUMERO_CLIENTE_TESTE;
            }

            // Evita duplicação
            String messageId = (String) key.get("id");
            if (messageId != null) {
                String lastId = ultimoMessageId.get(telefoneCliente);
                if (messageId.equals(lastId)) {
                    log.info("Mensagem duplicada ignorada");
                    return ResponseEntity.ok("OK");
                }
                ultimoMessageId.put(telefoneCliente, messageId);
            }

            long agora = System.currentTimeMillis();
            Long ultimoTempo = ultimaMensagemTempo.get(telefoneCliente);
            if (ultimoTempo != null && (agora - ultimoTempo) < 3000) {
                log.info("Mensagem muito rápida, ignorando");
                return ResponseEntity.ok("OK");
            }
            ultimaMensagemTempo.put(telefoneCliente, agora);

            String pushName = (String) data.get("pushName");
            String nomeCliente = (pushName != null && !pushName.equals(".") && !pushName.isEmpty())
                    ? pushName : telefoneCliente;

            String texto = extrairTexto(message);
            if (texto == null || texto.isBlank()) {
                log.info("Mensagem sem texto, ignorando");
                return ResponseEntity.ok("OK");
            }

            log.info("Cliente: {} ({}) - Msg: {}", telefoneCliente, nomeCliente, texto);

            whatsAppMessageService.processarMensagem(telefoneCliente, nomeCliente, texto, "CLIENTE");
            enviarParaFrontend(telefoneCliente, nomeCliente, texto, "CLIENTE");

            String resposta = botService.processarMensagem(telefoneCliente, nomeCliente, texto);

            if (resposta != null && !"em_atendimento".equals(resposta)) {
                whatsAppService.enviarMensagem(telefoneCliente, resposta);
                whatsAppMessageService.processarMensagem(telefoneCliente, "Bot", resposta, "BOT");
                enviarParaFrontend(telefoneCliente, "Bot", resposta, "BOT");
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK");
        }
    }

    private String extrairNumeroCliente(Map<String, Object> payload, Map<String, Object> key) {
        // Tenta do sender
        String sender = (String) payload.get("sender");
        if (sender != null && sender.contains("@s.whatsapp.net")) {
            String numero = sender.replace("@s.whatsapp.net", "");
            if (!numero.equals(NUMERO_EMPRESA) && numero.length() >= 10) {
                log.info("Número extraído do sender: {}", numero);
                return numero;
            }
        }

        // Tenta do remoteJid
        String remoteJid = (String) key.get("remoteJid");
        if (remoteJid != null && remoteJid.contains("@s.whatsapp.net")) {
            String numero = remoteJid.replace("@s.whatsapp.net", "");
            if (!numero.equals(NUMERO_EMPRESA) && numero.length() >= 10) {
                log.info("Número extraído do remoteJid: {}", numero);
                return numero;
            }
        }

        return null;
    }

    private String extrairTexto(Map<String, Object> message) {
        if (message.containsKey("conversation")) {
            return (String) message.get("conversation");
        }
        if (message.containsKey("extendedTextMessage")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ext = (Map<String, Object>) message.get("extendedTextMessage");
            return (String) ext.get("text");
        }
        return null;
    }

    private void enviarParaFrontend(String telefone, String nome, String mensagem, String remetente) {
        try {
            Map<String, Object> wsMessage = Map.of(
                    "telefone", telefone,
                    "nome", nome,
                    "mensagem", mensagem,
                    "remetente", remetente,
                    "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/whatsapp-mensagens", wsMessage);
        } catch (Exception e) {
            log.error("Erro ao enviar para frontend: {}", e.getMessage());
        }
    }

    @PostMapping("/responder")
    public ResponseEntity<String> responderCliente(@RequestBody Map<String, String> req) {
        String telefone = req.get("telefone");
        String mensagem = req.get("mensagem");
        String atendenteNome = req.get("atendenteNome");

        log.info("Atendente {} respondendo para: {}", atendenteNome, telefone);
        whatsAppService.enviarMensagem(telefone, mensagem);
        whatsAppMessageService.processarMensagem(telefone, atendenteNome, mensagem, "ATENDENTE");
        enviarParaFrontend(telefone, atendenteNome, mensagem, "ATENDENTE");

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/qrcode")
    public ResponseEntity<String> qrCode() {
        return ResponseEntity.ok(whatsAppService.gerarQrCode());
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviar(@RequestBody Map<String, String> req) {
        String telefone = req.get("telefone");
        String mensagem = req.get("mensagem");
        log.info("Enviando mensagem para: {}", telefone);
        whatsAppService.enviarMensagem(telefone, mensagem);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(whatsAppService.verificarStatus());
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
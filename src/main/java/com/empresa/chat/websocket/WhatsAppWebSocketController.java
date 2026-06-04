package com.empresa.chat.websocket;

import com.empresa.chat.service.whatsapp.WhatsAppService;
import com.empresa.chat.service.whatsapp.WhatsAppMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebSocketController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppMessageService whatsAppMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/whatsapp.responder")
    public void responderCliente(@Payload Map<String, String> payload) {
        String telefone = payload.get("telefone");
        String mensagem = payload.get("mensagem");
        String atendenteNome = payload.get("atendenteNome");

        log.info("Atendente {} respondendo para: {}", atendenteNome, telefone);

        whatsAppService.enviarMensagem(telefone, mensagem);
        whatsAppMessageService.processarMensagem(telefone, atendenteNome, mensagem, "ATENDENTE");

        Map<String, Object> wsMessage = Map.of(
                "telefone", telefone,
                "nome", atendenteNome,
                "mensagem", mensagem,
                "remetente", "ATENDENTE",
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/whatsapp-mensagens", wsMessage);
    }
}
package com.empresa.chat.service.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    @Value("${app.evolution.base-url:http://localhost:8081}")
    private String evolutionUrl;

    @Value("${app.evolution.api-key:tx6cg14quocf1giq0badws}")
    private String apiKey;

    @Value("${app.evolution.instance:chat}")
    private String instance;

    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarMensagem(String numeroOuJid, String mensagem) {
        try {
            String numero = numeroOuJid;
            if (numero.contains("@")) {
                numero = numero.substring(0, numero.indexOf("@"));
            }
            numero = formatarTelefone(numero);

            String url = evolutionUrl + "/message/sendText/" + instance;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            Map<String, Object> body = Map.of(
                    "number", numero,
                    "textMessage", Map.of("text", mensagem),
                    "options", Map.of("delay", 500)
            );

            log.info("Enviando para: {} via instância: {}", numero, instance);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            log.info("Resposta: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Erro ao enviar para {}: {}", numeroOuJid, e.getMessage());
        }
    }

    public String gerarQrCode() {
        try {
            String url = evolutionUrl + "/instance/connect/" + instance;
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", apiKey);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao gerar QR Code: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String verificarStatus() {
        try {
            String url = evolutionUrl + "/instance/fetchInstances";
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", apiKey);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao verificar status: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String formatarTelefone(String telefone) {
        String numero = telefone.replaceAll("[^0-9]", "");
        if (!numero.startsWith("55")) {
            numero = "55" + numero;
        }
        return numero;
    }
}
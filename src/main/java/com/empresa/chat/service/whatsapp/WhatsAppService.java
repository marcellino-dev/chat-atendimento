package com.empresa.chat.service.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    @Value("${app.whatsapp.graph-api-url:https://graph.facebook.com}")
    private String graphApiUrl;

    @Value("${app.whatsapp.api-version:v21.0}")
    private String apiVersion;

    @Value("${app.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================================================================
    // ENVIO
    // =========================================================================

    public void enviarMensagem(String telefone, String mensagem) {
        try {
            String url     = graphApiUrl + "/" + apiVersion + "/" + phoneNumberId + "/messages";
            String destino = normalizarDestino(telefone);

            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to",   destino,
                    "type", "text",
                    "text", Map.of("body", mensagem)
            );

            log.info("📤 Enviando para {} (original: {})", destino, telefone);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headersJson()),
                    String.class
            );
            log.info("✅ Mensagem enviada | status={}", resp.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("❌ Erro cliente ao enviar para {}: {} | body: {}",
                    telefone, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("❌ Erro servidor Meta ao enviar para {}: {} | body: {}",
                    telefone, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao enviar para {}: {}", telefone, e.getMessage());
        }
    }

    // =========================================================================
    // STATUS (consulta informações do número via Graph API)
    // =========================================================================

    public String verificarStatus() {
        try {
            String url = graphApiUrl + "/" + apiVersion + "/" + phoneNumberId
                    + "?fields=verified_name,display_phone_number,quality_rating";
            return restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headersGet()), String.class).getBody();
        } catch (Exception e) {
            log.error("Erro ao verificar status: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /**
     * A API oficial da Meta espera o número em formato E.164 sem "+" e sem sufixo
     * (ex: "5511999999999"), diferente do JID usado pela Evolution API.
     *
     * Números de celular brasileiros recebidos via webhook às vezes vêm SEM o
     * "nono dígito" (formato antigo, ex: 5547989299801 -> 554789299801), mesmo
     * quando o cadastro na Meta/contato usa o formato novo. Aqui reinserimos
     * esse dígito quando necessário, para bater com o destino esperado.
     */
    private String normalizarDestino(String telefone) {
        if (telefone == null) return "";
        String numero = telefone.replaceAll("[^0-9]", "");

        if (!numero.startsWith("55")) {
            numero = "55" + numero;
        }

        // Formato esperado: 55 + DDD(2 dígitos) + assinante(8 ou 9 dígitos)
        if (numero.length() < 12) {
            // Número curto demais para aplicar a regra com segurança — retorna como está
            return numero;
        }

        String ddd = numero.substring(2, 4);
        String assinante = numero.substring(4);

        // Celular sem o nono dígito: 8 dígitos, começando em 6-9 (padrão de celular no Brasil)
        if (assinante.length() == 8 && assinante.matches("^[6-9].*")) {
            assinante = "9" + assinante;
        }

        return "55" + ddd + assinante;
    }

    private HttpHeaders headersJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(accessToken);
        return h;
    }

    private HttpHeaders headersGet() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        return h;
    }
}
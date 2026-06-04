package com.empresa.chat.dto.response;

import com.empresa.chat.domain.model.Ticket;
import java.time.LocalDateTime;

public record TicketResponse(
    Long id,
    Long conversaId,
    String protocolo,
    String clienteNome,
    String clienteTel,
    String setor,
    Long setorId,
    String atendente,
    String prioridade,
    String status,
    String canal,
    LocalDateTime createdAt,
    LocalDateTime assumidoAt,
    LocalDateTime slaLimite
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
            t.getId(),
            t.getConversa().getId(),
            t.getConversa().getProtocolo(),
            t.getConversa().getClienteNome(),
            t.getConversa().getClienteTel(),
            t.getSetor().getNome(),
            t.getSetor().getId(),
            t.getAtendente() != null ? t.getAtendente().getUser().getNome() : null,
            t.getPrioridade().name(),
            t.getStatus().name(),
            t.getConversa().getCanal().name(),
            t.getCreatedAt(),
            t.getAssumidoAt(),
            t.getSlaLimite()
        );
    }
}

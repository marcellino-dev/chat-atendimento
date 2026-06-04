package com.empresa.chat.dto.response;

import com.empresa.chat.domain.model.Mensagem;
import java.time.LocalDateTime;

public record MensagemResponse(
    Long id,
    Long conversaId,
    String remetente,
    String conteudo,
    String tipo,
    Boolean lida,
    LocalDateTime createdAt
) {
    public static MensagemResponse from(Mensagem m) {
        return new MensagemResponse(
            m.getId(), m.getConversa().getId(),
            m.getRemetente().name(), m.getConteudo(),
            m.getTipo().name(), m.getLida(), m.getCreatedAt()
        );
    }
}

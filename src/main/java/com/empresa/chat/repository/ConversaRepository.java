package com.empresa.chat.repository;

import com.empresa.chat.domain.enums.StatusConversa;
import com.empresa.chat.domain.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    Optional<Conversa> findByProtocolo(String protocolo);

    List<Conversa> findByAtendenteIdAndStatus(Long atendenteId, StatusConversa status);

    List<Conversa> findByStatus(StatusConversa status);

    long countByStatus(StatusConversa status);

    // ── NOVO: busca direta por JID/telefone — evita findAll() em memória ──
    Optional<Conversa> findByClienteTelAndStatusNot(String clienteTel, StatusConversa status);

    // Busca todas as conversas ativas de um JID (para migração de LID→JID real)
    List<Conversa> findByClienteTelAndStatus(String clienteTel, StatusConversa status);
}
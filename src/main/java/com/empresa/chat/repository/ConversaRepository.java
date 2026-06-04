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
}

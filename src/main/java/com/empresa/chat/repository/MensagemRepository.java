package com.empresa.chat.repository;

import com.empresa.chat.domain.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConversaIdOrderByCreatedAtAsc(Long conversaId);

    long countByConversaIdAndLidaFalse(Long conversaId);

    @Modifying
    @Query("UPDATE Mensagem m SET m.lida = true WHERE m.conversa.id = :conversaId AND m.lida = false")
    int marcarTodasComoLidas(Long conversaId);
}

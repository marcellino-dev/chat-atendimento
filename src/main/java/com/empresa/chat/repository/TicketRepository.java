package com.empresa.chat.repository;

import com.empresa.chat.domain.enums.StatusTicket;
import com.empresa.chat.domain.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findBySetorIdAndStatusOrderByCreatedAtAsc(Long setorId, StatusTicket status);

    List<Ticket> findByAtendenteIdAndStatus(Long atendenteId, StatusTicket status);

    long countByStatus(StatusTicket status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.atendente.id = :atendenteId AND t.status = 'EM_ATENDIMENTO'")
    long countAtivosDoAtendente(Long atendenteId);
}

package com.empresa.chat.repository;

import com.empresa.chat.domain.enums.StatusAtendente;
import com.empresa.chat.domain.model.Atendente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtendenteRepository extends JpaRepository<Atendente, Long> {

    Optional<Atendente> findByUserId(Long userId);

    List<Atendente> findBySetorIdAndStatus(Long setorId, StatusAtendente status);

    List<Atendente> findByStatus(StatusAtendente status);

    @Query("SELECT a FROM Atendente a WHERE a.setor.id = :setorId AND a.status = 'ONLINE' " +
            "ORDER BY (SELECT COUNT(t) FROM Ticket t WHERE t.atendente = a AND t.status = 'EM_ATENDIMENTO') ASC")
    List<Atendente> findDisponiveisPorSetor(Long setorId);
}
package com.empresa.chat.repository;

import com.empresa.chat.domain.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SetorRepository extends JpaRepository<Setor, Long> {
    List<Setor> findByAtivoTrue();
    Optional<Setor> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
}

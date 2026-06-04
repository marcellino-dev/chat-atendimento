package com.empresa.chat.domain.model;

import com.empresa.chat.domain.enums.StatusTicket;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    public enum Prioridade { BAIXA, NORMAL, ALTA, URGENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false, unique = true)
    private Conversa conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Atendente atendente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Prioridade prioridade = Prioridade.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusTicket status = StatusTicket.ABERTO;

    @Column(name = "sla_limite")
    private LocalDateTime slaLimite;

    @Column(name = "assumido_at")
    private LocalDateTime assumidoAt;

    @Column(name = "fechado_at")
    private LocalDateTime fechadoAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

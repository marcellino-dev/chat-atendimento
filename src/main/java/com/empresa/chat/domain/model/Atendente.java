package com.empresa.chat.domain.model;

import com.empresa.chat.domain.enums.StatusAtendente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "atendentes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAtendente status = StatusAtendente.OFFLINE;

    @Column(name = "max_simultaneous", nullable = false)
    @Builder.Default
    private Integer maxSimultaneous = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

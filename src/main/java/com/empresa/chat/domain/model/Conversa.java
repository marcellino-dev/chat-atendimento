package com.empresa.chat.domain.model;

import com.empresa.chat.domain.enums.CanalOrigem;
import com.empresa.chat.domain.enums.StatusConversa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @Builder.Default
    private String protocolo = "CHAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CanalOrigem canal = CanalOrigem.WEB;

    @Column(name = "cliente_nome", length = 120)
    private String clienteNome;

    @Column(name = "cliente_tel", length = 30)
    private String clienteTel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Atendente atendente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusConversa status = StatusConversa.AGUARDANDO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "encerrada_at")
    private LocalDateTime encerradaAt;

    @OneToMany(mappedBy = "conversa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Mensagem> mensagens;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

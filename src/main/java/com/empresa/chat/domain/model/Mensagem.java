package com.empresa.chat.domain.model;

import com.empresa.chat.domain.enums.RemetenteTipo;
import com.empresa.chat.domain.enums.TipoMensagem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens", indexes = {
    @Index(name = "idx_conversa_created", columnList = "conversa_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    @ToString.Exclude
    private Conversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemetenteTipo remetente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TipoMensagem tipo = TipoMensagem.TEXTO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

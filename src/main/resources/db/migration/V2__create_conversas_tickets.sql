-- ================================================================
-- V2 — Conversas, Mensagens, Tickets e Filas (Fase 2)
-- ================================================================
CREATE TABLE conversas (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo    VARCHAR(20) UNIQUE NOT NULL,
    canal        ENUM('WHATSAPP','WEB') NOT NULL DEFAULT 'WEB',
    cliente_nome VARCHAR(120),
    cliente_tel  VARCHAR(30),
    atendente_id BIGINT,
    setor_id     BIGINT,
    status       ENUM('AGUARDANDO','EM_ATENDIMENTO','ENCERRADA') NOT NULL DEFAULT 'AGUARDANDO',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    encerrada_at DATETIME,
    FOREIGN KEY (atendente_id) REFERENCES atendentes(id) ON DELETE SET NULL,
    FOREIGN KEY (setor_id)     REFERENCES setores(id)    ON DELETE SET NULL
);

CREATE TABLE mensagens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversa_id  BIGINT       NOT NULL,
    remetente    ENUM('CLIENTE','ATENDENTE','BOT') NOT NULL,
    conteudo     TEXT         NOT NULL,
    tipo         ENUM('TEXTO','IMAGEM','AUDIO','ARQUIVO') NOT NULL DEFAULT 'TEXTO',
    lida         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversa_id) REFERENCES conversas(id) ON DELETE CASCADE,
    INDEX idx_conversa_created (conversa_id, created_at)
);

CREATE TABLE tickets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversa_id  BIGINT UNIQUE NOT NULL,
    setor_id     BIGINT        NOT NULL,
    atendente_id BIGINT,
    prioridade   ENUM('BAIXA','NORMAL','ALTA','URGENTE') NOT NULL DEFAULT 'NORMAL',
    status       ENUM('ABERTO','EM_ATENDIMENTO','TRANSFERIDO','FECHADO') NOT NULL DEFAULT 'ABERTO',
    sla_limite   DATETIME,
    assumido_at  DATETIME,
    fechado_at   DATETIME,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversa_id)  REFERENCES conversas(id)  ON DELETE CASCADE,
    FOREIGN KEY (setor_id)     REFERENCES setores(id)    ON DELETE RESTRICT,
    FOREIGN KEY (atendente_id) REFERENCES atendentes(id) ON DELETE SET NULL
);

-- ================================================================
-- V1 — Usuários e Atendentes (Fase 1)
-- ================================================================
CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(120)        NOT NULL,
    email      VARCHAR(180) UNIQUE NOT NULL,
    senha      VARCHAR(255)        NOT NULL,
    role       ENUM('ADMIN','ATENDENTE','BOT') NOT NULL DEFAULT 'ATENDENTE',
    ativo      BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE setores (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(100) UNIQUE NOT NULL,
    descricao  TEXT,
    ativo      BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE atendentes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT              NOT NULL UNIQUE,
    setor_id   BIGINT,
    status     ENUM('ONLINE','OFFLINE','AUSENTE') NOT NULL DEFAULT 'OFFLINE',
    max_simultaneous INT           NOT NULL DEFAULT 5,
    created_at DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (setor_id) REFERENCES setores(id) ON DELETE SET NULL
);

-- Seed: admin padrão (senha: admin123 — troque em produção!)
INSERT INTO users (nome, email, senha, role) VALUES
('Administrador', 'admin@empresa.com', '$2a$12$placeholder_bcrypt_hash_aqui', 'ADMIN');

INSERT INTO setores (nome, descricao) VALUES
('Financeiro',     'Cobranças, pagamentos e notas fiscais'),
('Suporte Técnico','Problemas técnicos e bugs'),
('Comercial',      'Vendas e propostas'),
('Outros',         'Demais assuntos');

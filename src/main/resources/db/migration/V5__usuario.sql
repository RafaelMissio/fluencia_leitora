CREATE TABLE usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil ENUM('COORDENADOR', 'PROFESSOR') NOT NULL,
    professor_id BIGINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    tentativas_falhas INT NOT NULL DEFAULT 0,
    bloqueado_ate DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_professor
        FOREIGN KEY (professor_id)
        REFERENCES professor(id),

    CONSTRAINT uk_usuario_email UNIQUE (email)
);

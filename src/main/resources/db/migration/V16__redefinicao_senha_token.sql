CREATE TABLE redefinicao_senha_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    usado_em DATETIME(6) NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_redefinicao_senha_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_redefinicao_senha_token_hash UNIQUE (token_hash)
);

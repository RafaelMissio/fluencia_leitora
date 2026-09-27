CREATE TABLE professor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE turma (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    nome_normalizado VARCHAR(100) GENERATED ALWAYS AS (LOWER(nome)) STORED,
    serie TINYINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    professor_id BIGINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_turma_ano_letivo
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT fk_turma_professor
        FOREIGN KEY (professor_id)
        REFERENCES professor(id),

    UNIQUE KEY uk_turma_ano_nome (ano_letivo_id, nome_normalizado)
);

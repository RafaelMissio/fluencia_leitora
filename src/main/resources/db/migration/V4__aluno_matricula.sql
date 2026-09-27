CREATE TABLE aluno (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_aluno_nome (nome)
);

CREATE TABLE matricula (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aluno_id BIGINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    turma_id BIGINT NOT NULL,
    professor_id BIGINT NULL,
    serie TINYINT NOT NULL,
    ano_finalizado BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_matricula_aluno
        FOREIGN KEY (aluno_id)
        REFERENCES aluno(id),

    CONSTRAINT fk_matricula_ano_letivo
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT fk_matricula_turma
        FOREIGN KEY (turma_id)
        REFERENCES turma(id),

    CONSTRAINT fk_matricula_professor
        FOREIGN KEY (professor_id)
        REFERENCES professor(id),

    UNIQUE KEY uk_matricula_aluno_ano (aluno_id, ano_letivo_id)
);

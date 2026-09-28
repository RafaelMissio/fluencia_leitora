CREATE TABLE avaliacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aluno_id BIGINT NOT NULL,
    professor_id BIGINT NULL,
    professor_nome VARCHAR(150) NULL,
    turma_id BIGINT NOT NULL,
    turma_nome VARCHAR(100) NOT NULL,
    serie TINYINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    ciclo_id BIGINT NOT NULL,
    tipo_leitura ENUM('PALAVRA','PSEUDOPALAVRA','TEXTO_CURTO') NOT NULL,
    data_avaliacao DATE NOT NULL,
    tempo_configurado_segundos INT NOT NULL,
    tempo_acumulado_segundos INT NOT NULL DEFAULT 0,
    iniciado_em TIMESTAMP(6) NULL,
    ultima_atividade_em TIMESTAMP(6) NOT NULL,
    finalizado_em TIMESTAMP(6) NULL,
    tempo_utilizado_segundos INT NULL,
    status ENUM('CRIADA','EM_ANDAMENTO','PAUSADA','FINALIZADA','CANCELADA') NOT NULL DEFAULT 'CRIADA',
    quantidade_total INT NOT NULL,
    quantidade_corretas INT NOT NULL DEFAULT 0,
    quantidade_incorretas INT NOT NULL DEFAULT 0,
    quantidade_nao_lidas INT NOT NULL DEFAULT 0,
    percentual_acerto DECIMAL(5,2) NULL,
    fase ENUM('PRE_LEITOR','LEITOR_INICIANTE','LEITOR_FLUENTE') NULL,
    nivel TINYINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avaliacao_aluno
        FOREIGN KEY (aluno_id) REFERENCES aluno(id),

    CONSTRAINT fk_avaliacao_professor
        FOREIGN KEY (professor_id) REFERENCES professor(id),

    CONSTRAINT fk_avaliacao_turma
        FOREIGN KEY (turma_id) REFERENCES turma(id),

    CONSTRAINT fk_avaliacao_ano_letivo
        FOREIGN KEY (ano_letivo_id) REFERENCES ano_letivo(id),

    CONSTRAINT fk_avaliacao_ciclo
        FOREIGN KEY (ciclo_id) REFERENCES ciclo(id),

    CONSTRAINT ck_avaliacao_serie CHECK (serie BETWEEN 1 AND 5),
    CONSTRAINT ck_avaliacao_tempo_configurado CHECK (tempo_configurado_segundos BETWEEN 10 AND 600),
    CONSTRAINT ck_avaliacao_quantidade_total CHECK (quantidade_total >= 0),
    CONSTRAINT ck_avaliacao_nivel
        CHECK ((fase = 'PRE_LEITOR' AND nivel BETWEEN 1 AND 4)
            OR (fase IS NOT NULL AND fase <> 'PRE_LEITOR' AND nivel IS NULL)
            OR (fase IS NULL AND nivel IS NULL))
);

CREATE INDEX idx_avaliacao_aluno ON avaliacao (aluno_id);
CREATE INDEX idx_avaliacao_status ON avaliacao (status);

CREATE TABLE avaliacao_palavra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    palavra VARCHAR(60) NOT NULL,
    tipo_palavra ENUM('CANONICA','NAO_CANONICA') NULL,
    status ENUM('PENDENTE','CORRETA','INCORRETA','NAO_LIDA') NOT NULL DEFAULT 'PENDENTE',

    CONSTRAINT fk_avaliacao_palavra_avaliacao
        FOREIGN KEY (avaliacao_id) REFERENCES avaliacao(id),

    CONSTRAINT uk_avaliacao_palavra_ordem UNIQUE (avaliacao_id, ordem)
);

CREATE TABLE avaliacao_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    data_hora TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    acao ENUM('MARCACAO_PALAVRA','CANCELAMENTO') NOT NULL,
    valor_anterior VARCHAR(500) NOT NULL,
    valor_novo VARCHAR(500) NOT NULL,
    justificativa VARCHAR(500) NULL,

    CONSTRAINT fk_avaliacao_auditoria_avaliacao
        FOREIGN KEY (avaliacao_id) REFERENCES avaliacao(id),

    CONSTRAINT fk_avaliacao_auditoria_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_avaliacao_auditoria_avaliacao ON avaliacao_auditoria (avaliacao_id, data_hora);

CREATE TABLE avaliacao_audio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    referencia_armazenamento VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avaliacao_audio_avaliacao
        FOREIGN KEY (avaliacao_id) REFERENCES avaliacao(id),

    CONSTRAINT uk_avaliacao_audio_avaliacao UNIQUE (avaliacao_id)
);

CREATE TABLE ano_letivo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ano INT NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    situacao ENUM('PLANEJADO', 'ATIVO', 'ENCERRADO') NOT NULL DEFAULT 'PLANEJADO',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_ano_letivo UNIQUE (ano)
);

CREATE TABLE configuracao_avaliacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ano_letivo_id BIGINT NOT NULL,
    serie TINYINT NOT NULL,
    quantidade_minima INT NOT NULL,
    quantidade_maxima INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_configuracao_ano
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT chk_config_quantidade
        CHECK (quantidade_minima <= quantidade_maxima),

    UNIQUE KEY uk_config_ano_serie (ano_letivo_id, serie)
);

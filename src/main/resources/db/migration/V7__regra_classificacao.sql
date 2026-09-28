CREATE TABLE regra_classificacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    serie TINYINT NOT NULL,
    quantidade_minima_acertos INT NOT NULL,
    quantidade_maxima_acertos INT NULL,
    fase ENUM('PRE_LEITOR','LEITOR_INICIANTE','LEITOR_FLUENTE') NOT NULL,
    nivel TINYINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    alterado_por BIGINT NULL,
    alterado_em TIMESTAMP NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_regra_classificacao_serie CHECK (serie BETWEEN 1 AND 5),
    CONSTRAINT ck_regra_classificacao_minima CHECK (quantidade_minima_acertos >= 0),
    CONSTRAINT ck_regra_classificacao_intervalo
        CHECK (quantidade_maxima_acertos IS NULL OR quantidade_maxima_acertos >= quantidade_minima_acertos),
    CONSTRAINT ck_regra_classificacao_nivel
        CHECK ((fase = 'PRE_LEITOR' AND nivel BETWEEN 1 AND 4)
            OR (fase <> 'PRE_LEITOR' AND nivel IS NULL)),
    CONSTRAINT fk_regra_classificacao_usuario
        FOREIGN KEY (alterado_por) REFERENCES usuario(id)
);

CREATE INDEX idx_regra_classificacao_filtro ON regra_classificacao (serie, ativo);

-- Seed (spec.md, Assumptions - faixas contíguas aprovadas pelo usuário)

-- Série 1
INSERT INTO regra_classificacao (serie, quantidade_minima_acertos, quantidade_maxima_acertos, fase, nivel) VALUES
(1, 0, 3, 'PRE_LEITOR', 1),
(1, 4, 5, 'PRE_LEITOR', 2),
(1, 6, 6, 'PRE_LEITOR', 3),
(1, 7, 7, 'PRE_LEITOR', 4),
(1, 8, 11, 'LEITOR_INICIANTE', NULL),
(1, 12, NULL, 'LEITOR_FLUENTE', NULL);

-- Série 2
INSERT INTO regra_classificacao (serie, quantidade_minima_acertos, quantidade_maxima_acertos, fase, nivel) VALUES
(2, 0, 4, 'PRE_LEITOR', 1),
(2, 5, 7, 'PRE_LEITOR', 2),
(2, 8, 9, 'PRE_LEITOR', 3),
(2, 10, 11, 'PRE_LEITOR', 4),
(2, 12, 30, 'LEITOR_INICIANTE', NULL),
(2, 31, NULL, 'LEITOR_FLUENTE', NULL);

-- Série 3
INSERT INTO regra_classificacao (serie, quantidade_minima_acertos, quantidade_maxima_acertos, fase, nivel) VALUES
(3, 0, 4, 'PRE_LEITOR', 1),
(3, 5, 7, 'PRE_LEITOR', 2),
(3, 8, 9, 'PRE_LEITOR', 3),
(3, 10, 11, 'PRE_LEITOR', 4),
(3, 12, 30, 'LEITOR_INICIANTE', NULL),
(3, 31, NULL, 'LEITOR_FLUENTE', NULL);

-- Série 4
INSERT INTO regra_classificacao (serie, quantidade_minima_acertos, quantidade_maxima_acertos, fase, nivel) VALUES
(4, 0, 4, 'PRE_LEITOR', 1),
(4, 5, 7, 'PRE_LEITOR', 2),
(4, 8, 9, 'PRE_LEITOR', 3),
(4, 10, 11, 'PRE_LEITOR', 4),
(4, 12, 30, 'LEITOR_INICIANTE', NULL),
(4, 31, NULL, 'LEITOR_FLUENTE', NULL);

-- Série 5
INSERT INTO regra_classificacao (serie, quantidade_minima_acertos, quantidade_maxima_acertos, fase, nivel) VALUES
(5, 0, 4, 'PRE_LEITOR', 1),
(5, 5, 7, 'PRE_LEITOR', 2),
(5, 8, 9, 'PRE_LEITOR', 3),
(5, 10, 11, 'PRE_LEITOR', 4),
(5, 12, 30, 'LEITOR_INICIANTE', NULL),
(5, 31, NULL, 'LEITOR_FLUENTE', NULL);

CREATE TABLE lista_palavras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    serie TINYINT NOT NULL,
    tipo_leitura ENUM('PALAVRA','PSEUDOPALAVRA','TEXTO_CURTO') NOT NULL,
    tipo_palavra ENUM('CANONICA','NAO_CANONICA') NULL,
    texto VARCHAR(2000) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT ck_lista_palavras_serie CHECK (serie BETWEEN 1 AND 5)
);

CREATE INDEX idx_lista_palavras_filtro ON lista_palavras (serie, tipo_leitura, ativo);

CREATE TABLE item_lista_palavras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lista_palavras_id BIGINT NOT NULL,
    palavra VARCHAR(60) NOT NULL,
    tipo_palavra ENUM('CANONICA','NAO_CANONICA') NOT NULL,
    ordem INT NOT NULL,

    CONSTRAINT fk_item_lista_palavras_lista
        FOREIGN KEY (lista_palavras_id) REFERENCES lista_palavras(id),

    CONSTRAINT uk_item_lista_palavras_ordem UNIQUE (lista_palavras_id, ordem)
);

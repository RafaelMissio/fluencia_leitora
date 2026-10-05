-- Avaliação configurada pelo coordenador para uma série do ano letivo; aparece
-- como pendente para cada aluno da série até ser aplicada (avaliacao.programada_id).
CREATE TABLE avaliacao_programada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    serie TINYINT NOT NULL,
    ciclo_id BIGINT NOT NULL,
    tipo_leitura ENUM('PALAVRA','PSEUDOPALAVRA','TEXTO_CURTO') NOT NULL,
    tempo_segundos INT NOT NULL,
    lista_palavras_id BIGINT NULL,
    palavras TEXT NULL,
    texto TEXT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_programada_ano_letivo FOREIGN KEY (ano_letivo_id) REFERENCES ano_letivo(id),
    CONSTRAINT fk_programada_ciclo FOREIGN KEY (ciclo_id) REFERENCES ciclo(id),
    CONSTRAINT fk_programada_lista FOREIGN KEY (lista_palavras_id) REFERENCES lista_palavras(id),
    CONSTRAINT ck_programada_serie CHECK (serie BETWEEN 1 AND 5),
    CONSTRAINT ck_programada_tempo CHECK (tempo_segundos BETWEEN 10 AND 600)
);

CREATE INDEX idx_programada_ano_serie ON avaliacao_programada (ano_letivo_id, serie, ativa);

ALTER TABLE avaliacao ADD COLUMN programada_id BIGINT NULL;
ALTER TABLE avaliacao ADD CONSTRAINT fk_avaliacao_programada
    FOREIGN KEY (programada_id) REFERENCES avaliacao_programada(id);
CREATE INDEX idx_avaliacao_programada ON avaliacao (programada_id, aluno_id);

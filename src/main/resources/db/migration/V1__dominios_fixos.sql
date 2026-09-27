CREATE TABLE ciclo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL
);

INSERT INTO ciclo (codigo, descricao)
VALUES
('ENTRADA', 'Entrada'),
('ACOMPANHAMENTO', 'Acompanhamento'),
('SAIDA', 'Saída');

CREATE TABLE tipo_leitura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL
);

INSERT INTO tipo_leitura (codigo, descricao)
VALUES
('PALAVRA', 'Leitura de Palavras'),
('PSEUDOPALAVRA', 'Leitura de Pseudopalavras'),
('TEXTO_CURTO', 'Leitura de Texto Curto');

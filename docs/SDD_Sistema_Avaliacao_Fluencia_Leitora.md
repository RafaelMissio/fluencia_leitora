# SDD — Sistema de Avaliação de Fluência Leitora

## 1. Visão Geral

### 1.1 Objetivo

O sistema tem como objetivo realizar e acompanhar avaliações de leitura dos alunos, permitindo identificar:

- Quantidade de palavras lidas;
- Quantidade de palavras lidas corretamente;
- Quantidade de palavras lidas incorretamente;
- Classificação do aluno;
- Evolução entre os ciclos de avaliação;
- Evolução entre anos letivos;
- Histórico das avaliações;
- Áudio da leitura realizada pelo aluno.

O sistema deverá permitir que professores realizem avaliações utilizando palavras, pseudopalavras ou textos curtos.

---

## 2. Tipos de Leitura

O sistema possuirá três tipos de avaliação de leitura:

| Código | Tipo |
|---|---|
| 1 | Leitura de Palavras |
| 2 | Leitura de Pseudopalavras |
| 3 | Leitura de Texto Curto |

As regras de classificação deverão ser aplicáveis aos três tipos de leitura.

---

## 3. Fases de Leitura

O aluno poderá ser classificado em uma das seguintes fases:

1. Pré-Leitor
2. Leitor Iniciante
3. Leitor Fluente

### 3.1 Níveis

A fase **Pré-Leitor** será dividida em quatro níveis:

- Pré-Leitor — Nível 1
- Pré-Leitor — Nível 2
- Pré-Leitor — Nível 3
- Pré-Leitor — Nível 4

As fases **Leitor Iniciante** e **Leitor Fluente** não possuem subdivisão de nível inicialmente.

---

## 4. Cadastros

O sistema deverá possuir os seguintes cadastros:

- Ano Letivo
- Turma
- Aluno
- Professor
- Ciclo
- Tipos de Leitura
- Palavras / Conteúdo da Avaliação
- Configuração das regras de classificação

---

### 4.1 Cadastro de Ano Letivo

O Ano Letivo deverá permitir definir:

- Ano;
- Data de início;
- Data de término;
- Situação;
- Quantidade mínima de palavras;
- Quantidade máxima de palavras.

A quantidade de palavras utilizada nas avaliações deverá ser configurável pelo coordenador.

---

### 4.2 Cadastro de Turma

A turma deverá possuir:

- Nome da turma;
- Ano/Série;
- Ano letivo;
- Professor responsável;
- Situação.

---

### 4.3 Cadastro de Aluno

O cadastro do aluno deverá possuir:

- Nome do aluno;
- Turma;
- Professor;
- Ano letivo;
- Ano/Série;
- Situação;
- Indicador de ano finalizado.

#### Regras

- Após o início das avaliações, os dados de identificação do aluno não deverão ser alterados diretamente.
- O professor responsável poderá ser alterado.
- Alterações deverão manter o histórico das avaliações já realizadas.

---

### 4.4 Cadastro de Professor

O cadastro do professor deverá possuir:

- Nome do professor;
- Turma(s);
- Situação.

Um professor poderá estar associado a uma ou mais turmas.

---

## 5. Ciclos de Avaliação

Cada ano letivo possuirá três ciclos:

| Código | Ciclo |
|---|---|
| 1 | Entrada |
| 2 | Acompanhamento |
| 3 | Saída |

### 5.1 Objetivo dos Ciclos

Os ciclos permitirão acompanhar a evolução do aluno durante o ano letivo.

Exemplo:

```text
Entrada          → 6 palavras corretas
Acompanhamento   → 11 palavras corretas
Saída            → 18 palavras corretas
```

O sistema deverá apresentar a evolução:

```text
Entrada → Acompanhamento → Saída
```

---

## 6. Avaliação

### 6.1 Busca do Aluno

A tela de avaliação deverá possuir um campo para busca do aluno.

Após selecionar o aluno, deverão ser apresentados:

- Nome;
- Turma;
- Professor;
- Ano letivo;
- Ano/Série;
- Ciclo atual;
- Histórico de avaliações;
- Última classificação;
- Evolução no ano letivo.

---

### 6.2 Configuração da Avaliação

Antes de iniciar uma avaliação, o professor deverá informar ou selecionar:

- Aluno;
- Tipo de leitura;
- Ciclo;
- Data da avaliação;
- Tempo da avaliação em segundos;
- Palavras ou conteúdo da avaliação.

---

### 6.3 Campo de Palavras

O professor poderá cadastrar ou selecionar as palavras que serão utilizadas na avaliação.

Exemplo:

```text
casa
bola
menino
escola
janela
carro
gato
livro
mesa
cadeira
```

O sistema deverá respeitar a quantidade mínima e máxima configurada para o ano/série do aluno.

---

## 7. Cronômetro

A avaliação possuirá um cronômetro configurável em segundos.

Exemplo:

```text
Tempo da avaliação: 60 segundos
```

O cronômetro deverá possuir as seguintes ações:

- Iniciar;
- Pausar;
- Continuar;
- Resetar;
- Finalizar.

### 7.1 Início

Ao clicar em **Iniciar Avaliação**:

1. O cronômetro deverá iniciar;
2. O microfone deverá ser habilitado;
3. A gravação do áudio deverá iniciar;
4. A avaliação deverá mudar para o status `EM_ANDAMENTO`.

---

### 7.2 Pausar

Ao clicar em **Pausar**:

- O cronômetro deverá ser pausado;
- O sistema deverá manter o estado atual da avaliação.

---

### 7.3 Resetar

Ao clicar em **Resetar**:

- O cronômetro deverá voltar ao tempo inicial;
- O estado da avaliação deverá ser reiniciado conforme regra definida para a aplicação.

---

### 7.4 Finalização Automática

Quando o tempo chegar a zero:

1. A avaliação deverá ser encerrada;
2. A gravação deverá ser finalizada;
3. Os resultados deverão ser calculados;
4. A classificação deverá ser determinada;
5. O áudio deverá ficar disponível.

---

## 8. Gravação de Áudio

O sistema deverá utilizar o microfone do computador para gravar a leitura do aluno.

Ao clicar em **Iniciar Avaliação**, o sistema deverá solicitar permissão de acesso ao microfone, quando necessário.

Durante a avaliação:

```text
Aluno → Microfone → Gravação → Avaliação
```

O áudio deverá estar associado à avaliação realizada.

Ao finalizar a avaliação, o professor deverá possuir opção para:

- Reproduzir o áudio;
- Baixar o áudio.

---

## 9. Identificação das Palavras

Durante ou após a avaliação, cada palavra deverá possuir um status.

Possíveis status:

```text
CORRETA
INCORRETA
NAO_LIDA
```

### 9.1 Representação Visual

As palavras deverão ser apresentadas visualmente:

- 🟢 Verde — palavra correta;
- 🔴 Vermelho — palavra incorreta;
- ⚪ Neutro — palavra não lida.

Exemplo:

```text
casa      → CORRETA
bola      → CORRETA
menino    → INCORRETA
escola    → CORRETA
janela    → NÃO LIDA
```

---

## 10. Tipos de Palavra

### 10.1 Primeiro Ano

No primeiro ano serão utilizadas inicialmente palavras:

- Canônicas.

A regra será aplicada para:

- Leitura de Palavras;
- Leitura de Pseudopalavras;
- Leitura de Texto Curto.

---

### 10.2 Segundo ao Quinto Ano

Do segundo ao quinto ano poderão ser utilizadas palavras:

- Canônicas;
- Não canônicas.

A regra será aplicada para:

- Leitura de Palavras;
- Leitura de Pseudopalavras;
- Leitura de Texto Curto.

---

## 11. Regras de Classificação

### 11.1 Primeiro Ano

Quantidade permitida:

```text
Mínimo: 15 palavras
Máximo: 20 palavras
```

Classificação informada:

| Palavras corretas | Classificação |
|---:|---|
| 1 a 3 | Pré-Leitor — Nível 1 |
| 5 | Pré-Leitor — Nível 2 |
| 6 | Pré-Leitor — Nível 3 |
| 7 | Pré-Leitor — Nível 4 |
| 8 a 11 | Leitor Iniciante |
| Acima de 11 | Leitor Fluente |

#### Ponto a validar

Não foi definida classificação para:

```text
0 palavras corretas
4 palavras corretas
```

Essas faixas precisam ser definidas antes da implementação definitiva da regra.

---

### 11.2 Segundo ao Quinto Ano

Quantidade permitida:

```text
Mínimo: 20 palavras
Máximo: 60 palavras
```

Classificação informada:

| Palavras corretas | Classificação |
|---:|---|
| 4 | Pré-Leitor — Nível 1 |
| 5 a 7 | Pré-Leitor — Nível 2 |
| 8 a 10 | Pré-Leitor — Nível 3 |
| 10 a 12 | Pré-Leitor — Nível 4 |
| 12 a 30 | Leitor Iniciante |
| Acima de 30 | Leitor Fluente |

#### Pontos a validar

Existem sobreposições:

```text
10 → Pré-Leitor Nível 3 ou Nível 4
12 → Pré-Leitor Nível 4 ou Leitor Iniciante
```

Também não foi definida classificação para resultados entre `0` e `3`.

As faixas deverão ser ajustadas para que não existam valores duplicados ou sem classificação.

---

## 12. Configuração das Regras

Recomenda-se que as regras de classificação **não sejam fixadas diretamente no código-fonte**.

As faixas deverão ser configuráveis no banco de dados.

Exemplo:

| Ano/Série | Mínimo | Máximo | Fase | Nível |
|---|---:|---:|---|---:|
| 1º Ano | 1 | 3 | Pré-Leitor | 1 |
| 1º Ano | 5 | 5 | Pré-Leitor | 2 |
| 1º Ano | 6 | 6 | Pré-Leitor | 3 |
| 1º Ano | 7 | 7 | Pré-Leitor | 4 |
| 1º Ano | 8 | 11 | Leitor Iniciante | - |
| 1º Ano | 12 | 20 | Leitor Fluente | - |

Isso permitirá que o coordenador altere as regras sem necessidade de uma nova versão da aplicação.

---

## 13. Resultado da Avaliação

Ao finalizar uma avaliação, o sistema deverá calcular:

```text
Total de palavras
Total de palavras lidas
Total de palavras corretas
Total de palavras incorretas
Total de palavras não lidas
Percentual de acerto
Tempo utilizado
Classificação
Nível
```

#### Exemplo

```text
Aluno: João Silva
Ano: 1º Ano
Ciclo: Entrada
Tipo: Leitura de Palavras

Total: 20
Corretas: 9
Incorretas: 4
Não lidas: 7

Percentual de acerto: 45%

Classificação:
Leitor Iniciante
```

---

## 14. Acompanhamento da Evolução

O sistema deverá permitir acompanhar o desempenho do aluno entre os três ciclos.

Exemplo:

| Ciclo | Corretas | Classificação |
|---|---:|---|
| Entrada | 6 | Pré-Leitor Nível 3 |
| Acompanhamento | 10 | Leitor Iniciante |
| Saída | 15 | Leitor Fluente |

O sistema deverá demonstrar a evolução entre os ciclos.

---

## 15. Comparação entre Anos Letivos

Além da evolução dentro do mesmo ano, o sistema deverá permitir comparar o desempenho com anos anteriores.

Exemplo:

| Ano Letivo | Ano/Série | Entrada | Acompanhamento | Saída |
|---|---|---:|---:|---:|
| 2026 | 2º Ano | 12 | 22 | 32 |
| 2027 | 3º Ano | 25 | 36 | 48 |

O sistema poderá apresentar:

```text
Evolução absoluta = resultado atual - resultado anterior
```

e:

```text
Evolução percentual =
((resultado atual - resultado anterior) / resultado anterior) * 100
```

Quando o resultado anterior for zero, o percentual deverá ser tratado separadamente para evitar divisão por zero.

---

## 16. Histórico do Aluno

Cada aluno deverá possuir um histórico contendo:

- Ano letivo;
- Ano/Série;
- Turma;
- Professor;
- Ciclo;
- Tipo de leitura;
- Data da avaliação;
- Quantidade de palavras;
- Palavras corretas;
- Palavras incorretas;
- Palavras não lidas;
- Percentual;
- Classificação;
- Nível;
- Tempo;
- Áudio.

---

## 17. Perfis de Usuário

Sugestão de perfis:

### 17.1 Coordenador

Poderá:

- Cadastrar ano letivo;
- Configurar quantidade mínima/máxima de palavras;
- Configurar regras de classificação;
- Cadastrar turmas;
- Cadastrar professores;
- Consultar alunos;
- Consultar avaliações;
- Visualizar relatórios;
- Comparar anos letivos.

### 17.2 Professor

Poderá:

- Consultar seus alunos;
- Realizar avaliações;
- Inserir palavras;
- Iniciar/parar/resetar cronômetro;
- Gravar avaliação;
- Classificar palavras;
- Consultar histórico;
- Reproduzir áudio;
- Baixar áudio.

---

## 18. Modelo de Dados

### 18.1 Principais Entidades

```text
ANO_LETIVO
    |
    +--- TURMA
            |
            +--- ALUNO
            |
            +--- PROFESSOR

ALUNO
    |
    +--- AVALIACAO
            |
            +--- AVALIACAO_PALAVRA
            |
            +--- AUDIO
            |
            +--- RESULTADO
            |
            +--- CICLO
            |
            +--- TIPO_LEITURA

REGRA_CLASSIFICACAO
```

---

## 19. DDL — MySQL

```sql
CREATE DATABASE IF NOT EXISTS avaliacao_leitura
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE avaliacao_leitura;

CREATE TABLE ano_letivo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ano INT NOT NULL,
    data_inicio DATE,
    data_fim DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_ano_letivo (ano)
);

CREATE TABLE professor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE turma (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    serie TINYINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    professor_id BIGINT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_turma_ano
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT fk_turma_professor
        FOREIGN KEY (professor_id)
        REFERENCES professor(id)
);

CREATE TABLE aluno (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    turma_id BIGINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    ano_finalizado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_aluno_turma
        FOREIGN KEY (turma_id)
        REFERENCES turma(id),

    CONSTRAINT fk_aluno_ano
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id)
);

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

CREATE TABLE configuracao_avaliacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ano_letivo_id BIGINT NOT NULL,
    serie TINYINT NOT NULL,
    quantidade_minima INT NOT NULL,
    quantidade_maxima INT NOT NULL,

    CONSTRAINT fk_configuracao_ano
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT chk_config_quantidade
        CHECK (quantidade_minima <= quantidade_maxima),

    UNIQUE KEY uk_config_ano_serie (ano_letivo_id, serie)
);

CREATE TABLE regra_classificacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    serie_inicial TINYINT NOT NULL,
    serie_final TINYINT NOT NULL,
    quantidade_minima_acertos INT NOT NULL,
    quantidade_maxima_acertos INT NOT NULL,
    fase ENUM(
        'PRE_LEITOR',
        'LEITOR_INICIANTE',
        'LEITOR_FLUENTE'
    ) NOT NULL,
    nivel TINYINT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_regra_intervalo
        CHECK (quantidade_minima_acertos <= quantidade_maxima_acertos)
);

CREATE TABLE avaliacao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aluno_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    ano_letivo_id BIGINT NOT NULL,
    ciclo_id BIGINT NOT NULL,
    tipo_leitura_id BIGINT NOT NULL,

    data_avaliacao DATE NOT NULL,
    tempo_configurado_segundos INT NOT NULL,
    tempo_utilizado_segundos INT,

    quantidade_total INT DEFAULT 0,
    quantidade_corretas INT DEFAULT 0,
    quantidade_incorretas INT DEFAULT 0,
    quantidade_nao_lidas INT DEFAULT 0,

    percentual_acerto DECIMAL(5,2),

    fase ENUM(
        'PRE_LEITOR',
        'LEITOR_INICIANTE',
        'LEITOR_FLUENTE'
    ),

    nivel TINYINT,

    status ENUM(
        'CRIADA',
        'EM_ANDAMENTO',
        'PAUSADA',
        'FINALIZADA',
        'CANCELADA'
    ) NOT NULL DEFAULT 'CRIADA',

    iniciado_em DATETIME,
    finalizado_em DATETIME,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avaliacao_aluno
        FOREIGN KEY (aluno_id)
        REFERENCES aluno(id),

    CONSTRAINT fk_avaliacao_professor
        FOREIGN KEY (professor_id)
        REFERENCES professor(id),

    CONSTRAINT fk_avaliacao_ano
        FOREIGN KEY (ano_letivo_id)
        REFERENCES ano_letivo(id),

    CONSTRAINT fk_avaliacao_ciclo
        FOREIGN KEY (ciclo_id)
        REFERENCES ciclo(id),

    CONSTRAINT fk_avaliacao_tipo
        FOREIGN KEY (tipo_leitura_id)
        REFERENCES tipo_leitura(id)
);

CREATE TABLE avaliacao_palavra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    palavra VARCHAR(255) NOT NULL,

    tipo_palavra ENUM(
        'CANONICA',
        'NAO_CANONICA',
        'PSEUDOPALAVRA'
    ),

    status ENUM(
        'PENDENTE',
        'CORRETA',
        'INCORRETA',
        'NAO_LIDA'
    ) NOT NULL DEFAULT 'PENDENTE',

    palavra_reconhecida VARCHAR(255),

    CONSTRAINT fk_palavra_avaliacao
        FOREIGN KEY (avaliacao_id)
        REFERENCES avaliacao(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_avaliacao_ordem (avaliacao_id, ordem)
);

CREATE TABLE avaliacao_audio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255),
    caminho_arquivo VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    tamanho_bytes BIGINT,
    duracao_segundos INT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audio_avaliacao
        FOREIGN KEY (avaliacao_id)
        REFERENCES avaliacao(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_aluno_nome
    ON aluno(nome);

CREATE INDEX idx_avaliacao_aluno
    ON avaliacao(aluno_id);

CREATE INDEX idx_avaliacao_data
    ON avaliacao(data_avaliacao);

CREATE INDEX idx_avaliacao_aluno_ano_ciclo
    ON avaliacao(aluno_id, ano_letivo_id, ciclo_id);
```

---

## 20. Fluxo Principal da Avaliação

```text
Professor
    |
    v
Busca aluno
    |
    v
Sistema carrega dados
    |
    v
Seleciona ciclo
    |
    v
Seleciona tipo de leitura
    |
    v
Seleciona/insere palavras
    |
    v
Define tempo
    |
    v
Iniciar avaliação
    |
    +-------------------+
    |                   |
    v                   v
Cronômetro          Microfone
    |                   |
    v                   v
Contagem            Gravação
    |                   |
    +---------+---------+
              |
              v
       Leitura do aluno
              |
              v
      Classificar palavras
              |
              v
         Tempo acabou?
              |
              v
      Finalizar avaliação
              |
              v
      Calcular resultado
              |
              v
       Classificar aluno
              |
              v
        Salvar histórico
              |
              v
      Disponibilizar áudio
```

---

## 21. Requisitos Funcionais

### RF001 — Cadastro de aluno

O sistema deverá permitir cadastrar alunos associados a uma turma e ano letivo.

### RF002 — Cadastro de professor

O sistema deverá permitir cadastrar professores.

### RF003 — Cadastro de turma

O sistema deverá permitir cadastrar turmas associadas a um ano letivo.

### RF004 — Ciclos

O sistema deverá suportar os ciclos Entrada, Acompanhamento e Saída.

### RF005 — Busca do aluno

O professor deverá conseguir localizar um aluno antes de iniciar a avaliação.

### RF006 — Configuração das palavras

O professor deverá conseguir informar ou selecionar o conteúdo da avaliação.

### RF007 — Cronômetro

O sistema deverá possuir cronômetro configurável em segundos.

### RF008 — Gravação

O sistema deverá gravar o áudio da leitura do aluno.

### RF009 — Classificação das palavras

Cada palavra deverá ser classificada como correta, incorreta ou não lida.

### RF010 — Resultado

O sistema deverá calcular automaticamente o resultado da avaliação.

### RF011 — Classificação

O sistema deverá determinar automaticamente a fase e o nível do aluno conforme as regras configuradas.

### RF012 — Histórico

O sistema deverá manter o histórico de avaliações do aluno.

### RF013 — Evolução por ciclo

O sistema deverá comparar Entrada, Acompanhamento e Saída.

### RF014 — Evolução anual

O sistema deverá permitir comparar o desempenho atual com anos letivos anteriores.

### RF015 — Download do áudio

Após finalizar a avaliação, deverá ser possível reproduzir e baixar o áudio.

---

## 22. Requisitos Não Funcionais

### RNF001 — Banco de Dados

O banco de dados deverá utilizar MySQL.

### RNF002 — Segurança

O sistema deverá possuir autenticação e autorização baseada no perfil do usuário.

### RNF003 — Áudio

O sistema deverá solicitar autorização do navegador para utilização do microfone.

### RNF004 — Auditoria

Avaliações finalizadas não deverão ser alteradas sem que exista rastreabilidade da alteração.

### RNF005 — Histórico

Mudanças de professor ou turma não deverão modificar os dados históricos das avaliações anteriores.

### RNF006 — Integridade

O sistema deverá impedir a exclusão acidental de informações necessárias para o histórico do aluno.

---

## 23. Arquitetura Sugerida

Para uma implementação utilizando Java e Spring Boot:

```text
Frontend
   |
   | HTTPS / REST
   v
Spring Boot API
   |
   +--- Autenticação / Autorização
   |
   +--- Alunos
   |
   +--- Professores
   |
   +--- Turmas
   |
   +--- Avaliações
   |
   +--- Classificação
   |
   +--- Evolução
   |
   +--- Relatórios
   |
   v
MySQL

Frontend
   |
   +--- Microfone / MediaRecorder API
   |
   v
Armazenamento de Áudio
```

Stack sugerida:

```text
Backend:
Java 21
Spring Boot
Spring Web
Spring Data JPA
Spring Security
Bean Validation
Flyway
MySQL

Frontend:
React ou Angular
MediaRecorder API

Testes:
JUnit 5
Mockito
Testcontainers
RestAssured

Documentação:
OpenAPI / Swagger

```

---

## 24. Pontos Importantes para Evolução do Projeto

Antes da implementação, deverão ser validadas as seguintes regras:

1. Definir classificação para `0` e `4` acertos no primeiro ano.
2. Corrigir sobreposição das faixas `10` e `12` do segundo ao quinto ano.
3. Definir classificação para `0–3` acertos do segundo ao quinto ano.
4. Definir se as palavras serão cadastradas previamente ou informadas pelo professor em cada avaliação.
5. Definir se o reconhecimento de palavra correta/incorreta será manual, automático por reconhecimento de voz ou híbrido.
6. Definir política de armazenamento e retenção dos áudios.
7. Definir se cada aluno poderá realizar mais de uma avaliação do mesmo tipo no mesmo ciclo.
8. Definir se a comparação anual utilizará quantidade absoluta, percentual de acerto ou ambos.
9. Definir se a classificação varia conforme o tipo de leitura.
10. Definir como textos curtos serão avaliados: palavra por palavra, quantidade total correta ou outro critério.

---

## 25. Evoluções Futuras

O sistema poderá futuramente possuir:

- Dashboard de evolução;
- Gráficos por aluno;
- Gráficos por turma;
- Comparação entre turmas;
- Comparação entre anos letivos;
- Relatórios por professor;
- Relatórios por ciclo;
- Exportação para PDF/Excel;
- Reconhecimento automático de fala;
- Transcrição do áudio;
- Identificação automática de palavras corretas/incorretas;
- Indicadores de evolução percentual;
- Dashboard para coordenadores;
- Histórico longitudinal do aluno.

---

## 26. Resumo

O sistema deverá fornecer uma visão longitudinal do desenvolvimento da leitura:

```text
Aluno
  |
  +-- Ano Letivo
        |
        +-- Entrada
        |
        +-- Acompanhamento
        |
        +-- Saída
        |
        +-- Resultado anual
                |
                v
          Próximo Ano Letivo
                |
                v
       Comparação / Evolução
```

Dessa forma será possível acompanhar tanto a evolução **dentro do ano letivo** quanto a evolução **entre diferentes anos letivos**.

---

## 27. Estratégia de Testes e Cobertura de Código

O projeto deverá possuir:

- Testes unitários;
- Testes de integração;
- Cobertura mínima de **85% do código** como critério de aprovação.

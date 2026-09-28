# Banco de Palavras Specification

> Origem: SDD §4 ("Palavras / Conteúdo da Avaliação"), §6.3, §10, §24 itens 4 e 10, RF006.

## Problem Statement

O professor precisa de listas de palavras, pseudopalavras e textos curtos adequados à série, para não digitar tudo a cada avaliação. As listas precisam respeitar as regras de tipo de palavra (1º ano só usa canônicas) e não podem mudar avaliações já criadas.

## Goals

- [ ] O coordenador mantém listas reutilizáveis por série e tipo de leitura.
- [ ] O professor encontra listas compatíveis filtrando por série e tipo em uma chamada.
- [ ] Nenhuma lista do 1º ano contém palavra marcada como não canônica.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Palavras digitadas pelo professor na hora | Ficam na feature `avaliacao` (entrada ad hoc) |
| Detecção automática de estrutura silábica (canônica ou não) | Precisaria de análise linguística; o tipo é informado manualmente |
| Listas criadas por professores | O SDD §17 dá ao professor "inserir palavras" na avaliação, não o cadastro de listas |
| Importação de listas de arquivo | Não pedida |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Origem das palavras (SDD §24 item 4) | Banco de listas mantido pelo coordenador + digitação pelo professor na avaliação; as palavras são copiadas para a avaliação | Opção escolhida pelo usuário | y |
| Significado de `tipo_palavra` | Estrutura silábica: `CANONICA` ou `NAO_CANONICA`, válida para os 3 tipos de leitura; o valor `PSEUDOPALAVRA` do DDL não é usado, porque o tipo de leitura já indica isso | O SDD §10 aplica canônica/não canônica também a pseudopalavras | n |
| Textos curtos (SDD §24 item 10) | A lista TEXTO_CURTO guarda um texto corrido (até 2000 caracteres), que vira palavras individuais (tokens) na avaliação e é avaliado palavra por palavra | É o mesmo modelo de `avaliacao_palavra`, com resultado comparável | n |
| Tipo das palavras de um texto curto | Informado uma vez para o texto inteiro (`tipoPalavra` da lista) | Marcar palavra a palavra num texto é inviável na prática | n |
| Palavras duplicadas | Recusadas em listas de PALAVRA e PSEUDOPALAVRA (sem diferenciar maiúsculas); permitidas em TEXTO_CURTO | Um texto repete palavras naturalmente | n |
| Tamanho da lista | De 1 a 200 itens; o mín./máx. por série é validado ao criar a avaliação, e não na lista | Os limites mudam por ano letivo; a lista é reaproveitada entre anos | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Cadastrar listas de palavras ⭐ MVP

**User Story**: Como coordenador, quero cadastrar listas de palavras e pseudopalavras por série, para que os professores usem conteúdos padronizados.

**Why P1**: É a fonte principal de conteúdo das avaliações.

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/listas-palavras` com `nome` (3–100), `serie` (1–5), `tipoLeitura` (PALAVRA ou PSEUDOPALAVRA) e `itens` (cada um com `palavra` e `tipoPalavra`) THEN o sistema SHALL criar a lista ativa guardando a ordem dos itens (1..n) e retornar 201.
2. IF `serie` = 1 e algum item tiver `tipoPalavra = NAO_CANONICA` THEN o sistema SHALL retornar 422 com código `NAO_CANONICA_PROIBIDA_1_ANO` e as posições dos itens inválidos.
3. IF algum item tiver `palavra` vazia, com mais de 60 caracteres ou com caracteres diferentes de letras (acentuadas inclusive) e hífen THEN o sistema SHALL retornar 422 com a posição do item.
4. IF a lista tiver palavras repetidas (sem diferenciar maiúsculas, depois do trim) THEN o sistema SHALL retornar 422 com código `PALAVRA_DUPLICADA`.
5. IF a lista tiver 0 ou mais de 200 itens THEN o sistema SHALL retornar 422.
6. WHEN o coordenador altera uma lista THEN o sistema SHALL NOT alterar as palavras de avaliações já criadas a partir dela.

**Independent Test**: Criar uma lista do 1º ano com 15 palavras canônicas (201); tentar incluir uma não canônica (422).

---

### P1: Cadastrar textos curtos ⭐ MVP

**User Story**: Como coordenador, quero cadastrar textos curtos por série, para aplicar a avaliação de leitura de texto.

**Why P1**: TEXTO_CURTO é um dos três tipos obrigatórios (SDD §2).

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/listas-palavras` com `tipoLeitura = TEXTO_CURTO`, `texto` (1–2000 caracteres), `serie` e `tipoPalavra` THEN o sistema SHALL criar a lista, gerar as palavras com a regra de tokenização e retornar 201 com `quantidadePalavras`.
2. The system SHALL tokenizar o texto separando por espaços em branco, retirar a pontuação do início e do fim de cada palavra e descartar as palavras que ficarem vazias.
3. WHEN o texto "O gato, a bola." é tokenizado THEN o sistema SHALL gerar exatamente [O, gato, a, bola] com as ordens 1 a 4.
4. IF `serie` = 1 e `tipoPalavra = NAO_CANONICA` THEN o sistema SHALL retornar 422 com código `NAO_CANONICA_PROIBIDA_1_ANO`.

**Independent Test**: Cadastrar um texto e conferir o número de palavras e a ordem.

---

### P1: Consultar listas ⭐ MVP

**User Story**: Como professor, quero ver as listas compatíveis com a série do aluno e o tipo de leitura, para escolher uma na hora de configurar a avaliação.

**Why P1**: O professor precisa disso para usar o banco.

**Acceptance Criteria**:
1. WHEN um usuário envia `GET /api/v1/listas-palavras?serie={s}&tipoLeitura={t}` THEN o sistema SHALL retornar só as listas ativas daquela série e daquele tipo, com `id`, `nome` e `quantidadePalavras`.
2. WHEN um usuário envia `GET /api/v1/listas-palavras/{id}` THEN o sistema SHALL retornar a lista com os itens na ordem e, se for TEXTO_CURTO, também o texto original.
3. WHEN o coordenador inativa uma lista THEN o sistema SHALL retirá-la da consulta filtrada e SHALL mantê-la acessível por id.

**Independent Test**: Filtrar série 2 + PSEUDOPALAVRA e receber só as listas correspondentes.

---

## Edge Cases

- IF `tipoLeitura` = TEXTO_CURTO e forem enviados `itens` em vez de `texto` (ou o contrário para os outros tipos) THEN o sistema SHALL retornar 422 com código `CONTEUDO_INCOMPATIVEL_COM_TIPO`.
- IF o texto gerar mais de 200 palavras THEN o sistema SHALL retornar 422.
- WHEN a palavra tem espaços no início ou no fim THEN o sistema SHALL removê-los antes de validar e gravar.
- The system SHALL guardar as palavras com a grafia original (maiúsculas e acentos preservados).

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | PAL-02..PAL-05, PAL-08, PAL-12 |
| Failure / partial-failure | A lista e os itens são gravados numa única transação |
| Idempotency / duplicates | PAL-04 |
| Auth boundaries | Só o COORDENADOR escreve; PROFESSOR e COORDENADOR leem |
| Concurrency / ordering | Lock otimista na lista (409 `CONFLITO_DE_VERSAO`); a ordem dos itens é explícita |
| Data lifecycle | Listas são inativadas, nunca excluídas; as avaliações guardam cópia das palavras (PAL-06) |
| Observability | N/A because é CRUD simples |
| External-dependency failure | N/A because não há dependência externa |
| State-transition integrity | ativo ↔ inativo, sem efeito sobre as avaliações |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| PAL-01 | P1: Criar lista de palavras e pseudopalavras | T10, T13 | ✅ Verified |
| PAL-02 | P1: 1º ano só com canônicas | T10, T13 | ✅ Verified |
| PAL-03 | P1: Validação do formato da palavra | T8, T13 | ✅ Verified |
| PAL-04 | P1: Sem duplicatas | T10, T13 | ✅ Verified |
| PAL-05 | P1: Tamanho de 1 a 200 | T8, T13 | ✅ Verified |
| PAL-06 | P1: Alteração da lista não afeta avaliações | T11 | ⏸ Deferred - só é observável quando a feature `avaliacao` existir (guarda cópia das palavras); AC correspondente a adicionar no spec de `avaliacao` quando essa feature chegar ao Design |
| PAL-07 | P1: Criar texto curto | T10, T13 | ✅ Verified |
| PAL-08 | P1: Tokenização do texto | T2 | ✅ Verified |
| PAL-09 | P1: Texto do 1º ano canônico | T10, T13 | ✅ Verified |
| PAL-10 | P1: Consulta filtrada | T7, T15 | ✅ Verified |
| PAL-11 | P1: Consulta por id / inativação | T12, T15 | ✅ Verified |
| PAL-12 | P1: Conteúdo compatível com o tipo | T10, T13 | ✅ Verified |

**Coverage:** 12 total, 11 verified, 1 deferred (PAL-06, waiting on `avaliacao`) - `.specs/features/banco-palavras/validation.md` (Verifier, iteração 2/3, 2026-09-28).

**Nota (spec-precision, PAL-03):** o formato da "posição do item" não foi fixado. A validação de formato (`palavra` vazia/>60/caractere inválido) usa Bean Validation e retorna o caminho do campo 0-based (`itens[0].palavra`); a validação de série×canônica (PAL-02/PAL-09) é uma regra de negócio própria e retorna posições 1-based em `details.posicoes` (AD-008). São dois mecanismos de erro diferentes (validação declarativa de formato vs. regra de negócio), então o formato diferente é aceito como está - não é uma inconsistência a corrigir.

---

## Success Criteria

- [ ] A tokenização é uma função pura reutilizada pela feature `avaliacao` (entrada ad hoc de texto).
- [ ] Todos os ACs cobertos por testes; os de validação ficam em testes unitários.

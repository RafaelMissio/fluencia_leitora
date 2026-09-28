# Regras de Classificação Specification

> Origem: SDD §3, §11, §12, §24 itens 1–3 e 9, RF011. Decisões: AD-001.

## Problem Statement

O aluno é classificado em Pré-Leitor (níveis 1–4), Leitor Iniciante ou Leitor Fluente conforme a série e o número de palavras corretas. As faixas do SDD tinham lacunas (0 e 4 no 1º ano; 0–3 no 2º–5º) e sobreposições (10 e 12 no 2º–5º). O coordenador precisa alterar as faixas sem nova versão da aplicação.

## Goals

- [ ] Para qualquer série de 1 a 5 e qualquer número de acertos ≥ 0, a classificação retorna exatamente uma fase (e um nível, quando for Pré-Leitor).
- [ ] O coordenador altera as faixas pela API e a mudança vale para as próximas finalizações, sem deploy.
- [ ] O sistema recusa qualquer conjunto de faixas com lacuna ou sobreposição.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Regras diferentes por tipo de leitura | O SDD §2 define as mesmas regras para os três tipos (AD-001) |
| Regras diferentes por ano letivo | O DDL não prevê; a mudança vale a partir do momento em que é salva |
| Reclassificação em massa de avaliações antigas | Violaria o RNF005 (o histórico fica congelado) |
| Classificação por percentual ou por palavras por minuto | O SDD classifica por número absoluto de palavras corretas |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Faixas do 1º ano (lacunas 0 e 4) | 0–3 PRE_LEITOR N1; 4–5 N2; 6 N3; 7 N4; 8–11 LEITOR_INICIANTE; 12+ LEITOR_FLUENTE | Opção "faixas contíguas" escolhida pelo usuário | y |
| Faixas do 2º ao 5º ano (sobreposições 10 e 12, lacuna 0–3) | 0–4 PRE_LEITOR N1; 5–7 N2; 8–9 N3; 10–11 N4; 12–30 LEITOR_INICIANTE; 31+ LEITOR_FLUENTE | Opção "faixas contíguas" escolhida pelo usuário | y |
| Classificação por tipo de leitura (SDD §24 item 9) | É a mesma para os três tipos | SDD §2 | y |
| Limite superior da última faixa | `quantidadeMaximaAcertos = null` significa sem limite | Evita lacuna quando o coordenador aumenta o máximo de palavras | n |
| Granularidade da edição | O coordenador substitui de uma vez o conjunto inteiro de faixas de uma série (operação atômica) | Validar contiguidade faixa a faixa deixaria estados intermediários inválidos | n |
| Modelo da série | Uma linha por série (`serieInicial = serieFinal`) no seed; a API trabalha por série | Simplifica a validação; o modelo continua aceitando intervalos | n |
| Histórico de regras | A substituição inativa as linhas anteriores (`ativo=false`) e grava `alteradoPor` e `alteradoEm` | Rastreabilidade da mudança de regra | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Classificar por série e acertos ⭐ MVP

**User Story**: Como sistema de avaliação, preciso de uma função de classificação que, dadas a série e as palavras corretas, retorne fase e nível (RF011).

**Why P1**: A finalização da avaliação depende dela.

**Acceptance Criteria**:
1. WHEN a classificação é pedida para a série 1 com 0, 3, 4, 5, 6, 7, 8, 11, 12 e 20 acertos THEN o sistema SHALL retornar, com o seed, respectivamente PRE_LEITOR/1, PRE_LEITOR/1, PRE_LEITOR/2, PRE_LEITOR/2, PRE_LEITOR/3, PRE_LEITOR/4, LEITOR_INICIANTE/null, LEITOR_INICIANTE/null, LEITOR_FLUENTE/null e LEITOR_FLUENTE/null.
2. WHEN a classificação é pedida para qualquer série de 2 a 5 com 0, 4, 5, 7, 8, 9, 10, 11, 12, 30, 31 e 60 acertos THEN o sistema SHALL retornar, com o seed, respectivamente PRE_LEITOR/1, PRE_LEITOR/1, PRE_LEITOR/2, PRE_LEITOR/2, PRE_LEITOR/3, PRE_LEITOR/3, PRE_LEITOR/4, PRE_LEITOR/4, LEITOR_INICIANTE/null, LEITOR_INICIANTE/null, LEITOR_FLUENTE/null e LEITOR_FLUENTE/null.
3. The system SHALL usar apenas as faixas com `ativo=true` na classificação.
4. The system SHALL retornar o mesmo resultado para os tipos PALAVRA, PSEUDOPALAVRA e TEXTO_CURTO.
5. IF nenhuma faixa ativa cobrir a série e os acertos THEN o sistema SHALL retornar "sem classificação" (fase e nível nulos) em vez de lançar um erro.

**Independent Test**: Testes unitários parametrizados com as tabelas dos ACs 1 e 2.

---

### P1: Configurar faixas ⭐ MVP

**User Story**: Como coordenador, quero consultar e substituir as faixas de uma série, para ajustar os critérios pedagógicos sem depender da equipe técnica (SDD §12).

**Why P1**: É uma exigência explícita do SDD §12 e resolve os "pontos a validar".

**Acceptance Criteria**:
1. WHEN um usuário autenticado envia `GET /api/v1/regras-classificacao?serie={1-5}` THEN o sistema SHALL retornar as faixas ativas da série ordenadas por `quantidadeMinimaAcertos`.
2. WHEN o coordenador envia `PUT /api/v1/regras-classificacao/series/{serie}` com uma lista de faixas válidas THEN o sistema SHALL inativar as faixas anteriores, gravar as novas na mesma transação e retornar 200 com as novas faixas.
3. IF a primeira faixa não começar em 0 THEN o sistema SHALL retornar 422 com código `FAIXA_NAO_INICIA_EM_ZERO`.
4. IF duas faixas consecutivas deixarem um valor sem cobertura THEN o sistema SHALL retornar 422 com código `FAIXA_COM_LACUNA` e o primeiro valor descoberto.
5. IF duas faixas cobrirem o mesmo valor THEN o sistema SHALL retornar 422 com código `FAIXA_SOBREPOSTA` e o primeiro valor duplicado.
6. IF a última faixa tiver `quantidadeMaximaAcertos` diferente de null THEN o sistema SHALL retornar 422 com código `FAIXA_FINAL_LIMITADA`.
7. IF uma faixa PRE_LEITOR não tiver `nivel` entre 1 e 4, ou uma faixa LEITOR_INICIANTE ou LEITOR_FLUENTE tiver `nivel` preenchido THEN o sistema SHALL retornar 422.
8. IF alguma validação falhar THEN o sistema SHALL manter as faixas anteriores ativas e sem alteração.
9. WHEN as faixas são substituídas THEN o sistema SHALL gravar `alteradoPor` (id do usuário) e `alteradoEm` nas linhas inativadas.

**Independent Test**: Enviar uma faixa com lacuna em 4 e receber 422 `FAIXA_COM_LACUNA` com valor 4; enviar um conjunto válido e ver a classificação mudar na próxima chamada.

---

### P2: Consultar histórico de regras

**User Story**: Como coordenador, quero ver as versões anteriores das faixas, para entender por que avaliações antigas têm outra classificação.

**Why P2**: Ajuda na auditoria, mas não bloqueia o fluxo principal.

**Acceptance Criteria**:
1. WHEN o coordenador envia `GET /api/v1/regras-classificacao/historico?serie={serie}` THEN o sistema SHALL retornar todas as faixas (ativas e inativas) da série, agrupadas por `alteradoEm` em ordem decrescente.

**Independent Test**: Substituir as faixas duas vezes e ver 3 grupos no histórico.

---

## Edge Cases

- IF `quantidadeMinimaAcertos` for negativo ou maior que `quantidadeMaximaAcertos` THEN o sistema SHALL retornar 422.
- IF a lista enviada estiver vazia THEN o sistema SHALL retornar 422 com código `FAIXA_NAO_INICIA_EM_ZERO`.
- WHEN as faixas de uma série mudam THEN o sistema SHALL NOT alterar fase e nível já gravados em avaliações FINALIZADAS.
- IF a série no path estiver fora de 1–5 THEN o sistema SHALL retornar 422.

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | REG-07..REG-11 |
| Failure / partial-failure | A substituição é atômica; se falhar, as faixas anteriores continuam valendo (REG-12) |
| Idempotency / duplicates | Reenviar o mesmo PUT gera um novo registro histórico com o mesmo conteúdo e sem efeito na classificação |
| Auth boundaries | Só o COORDENADOR escreve; todos os perfis leem |
| Concurrency / ordering | Dois PUT simultâneos na mesma série são serializados por lock pessimista na série; o último a confirmar vence |
| Data lifecycle | Faixas antigas nunca são apagadas, apenas inativadas |
| Observability | Log INFO com a série, o usuário e as faixas novas a cada substituição |
| External-dependency failure | N/A because não há dependência externa |
| State-transition integrity | ativo → inativo, nunca o contrário |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| REG-01 | P1: Seed das faixas do 1º ano | T3 | Implementing |
| REG-02 | P1: Seed das faixas do 2º ao 5º ano | T3 | Implementing |
| REG-03 | P1: Classificação usa só faixas ativas | T6 | Implementing |
| REG-04 | P1: Mesma regra para os 3 tipos | - | Pending |
| REG-05 | P1: Sem classificação quando não há cobertura | - | Pending |
| REG-06 | P1: Consulta de faixas | T6 | Implementing |
| REG-07 | P1: Substituição atômica | - | Pending |
| REG-08 | P1: Validação - início em zero | - | Pending |
| REG-09 | P1: Validação - lacuna | - | Pending |
| REG-10 | P1: Validação - sobreposição | - | Pending |
| REG-11 | P1: Validação - última faixa sem limite; nível coerente | - | Pending |
| REG-12 | P1: Falha preserva faixas anteriores | - | Pending |
| REG-13 | P1: Rastreabilidade de quem alterou | - | Pending |
| REG-14 | P1: Avaliações finalizadas não são reclassificadas | - | Pending |
| REG-15 | P2: Histórico de regras | T6 | Implementing |

**Coverage:** 15 total, 0 mapped to tasks, 15 unmapped ⚠️

---

## Success Criteria

- [ ] Um teste de propriedade confirma que, para cada série de 1 a 5 e acertos de 0 a 200, existe exatamente uma faixa ativa.
- [ ] Alterar uma faixa muda a classificação da próxima avaliação sem reiniciar a aplicação.

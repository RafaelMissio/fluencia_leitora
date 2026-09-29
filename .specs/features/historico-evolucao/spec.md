# Histórico e Evolução Specification

## Problem Statement

O sistema já grava cada avaliação finalizada (`avaliacao`, feature `avaliacao`), mas não existe forma de consultar o histórico de um aluno nem de acompanhar sua evolução entre ciclos ou entre anos letivos (SDD §14, §15, §16; RF012, RF013, RF014). Sem isso, professor e coordenador não conseguem ver o progresso do aluno ao longo do tempo - só o resultado de uma avaliação isolada.

## Goals

- [ ] Professor e coordenador consultam o histórico de avaliações finalizadas de um aluno, paginado e filtrável.
- [ ] Coordenador consulta a evolução do aluno entre os três ciclos (Entrada, Acompanhamento, Saída) de um ano letivo.
- [ ] Coordenador compara o desempenho do aluno entre anos letivos, ciclo a ciclo, com evolução absoluta e percentual.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Reconhecimento automático de fala / recálculo de resultado | Já resolvido em `avaliacao`; esta feature só lê o que já foi calculado |
| Comparação agregada por turma/série entre anos | Decisão do usuário: RF014 só por aluno individual nesta versão |
| Tela/UI de histórico e evolução | Feature `frontend-web`, decisão do usuário de ordem (2026-09-27); esta feature só expõe a API |
| Exportação (PDF/planilha) de histórico ou relatórios | Não mencionado no SDD para esta feature; fica para evolução futura se pedido |
| Alterar/excluir avaliações do histórico | RNF006 exige preservar o histórico; esta feature é somente leitura |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | --------------- | --------- | ---------- |
| Escopo da comparação anual (RF014) | Só por aluno individual, sem agregação por turma/série | Decisão do usuário; bate com o exemplo do SDD §15 (uma linha por ano do mesmo aluno) | y |
| Métrica usada nas fórmulas de evolução absoluta/percentual (§14, §15) | `quantidade_corretas` da avaliação | Decisão do usuário; bate literalmente com as tabelas de exemplo do SDD, que usam "Corretas" como coluna | y |
| Alinhamento da evolução anual entre ciclos | Mesmo ciclo, ano a ano (Entrada vs Entrada, Acompanhamento vs Acompanhamento, Saída vs Saída) | Decisão do usuário; alinhado com as colunas fixas da tabela do SDD §15 | y |
| Quem acessa a evolução por ciclo (RF013) | Só Coordenador | Decisão do usuário - RF013 tratado como relatório gerencial, mesmo padrão de acesso do RF014, apesar do SDD não atribuir RF013 a um perfil específico (§17.1/§17.2 só citam RF012 e RF014 explicitamente) | y |
| Quem acessa o histórico (RF012) | Professor (só seus alunos, via matrícula ativa) + Coordenador (todos) | SDD §17.2 lista "consultar histórico" para o Professor; §17.1 não restringe o Coordenador; segue o padrão de `AUTH-09` já usado em `avaliacao`/`cadastros-base` | y |
| Percentual quando o resultado anterior é zero | Se atual também for zero → percentual = 0; se atual > 0 → percentual = `null` (indefinido), evolução absoluta continua calculada | SDD §15 exige "tratamento separado" para divisão por zero mas não define o valor; escolha evita inventar uma métrica (ex.: 100%) sem base no SDD | y |
| Avaliações incluídas no histórico e nas evoluções | Só `FINALIZADA` | `CANCELADA`/`CRIADA`/`EM_ANDAMENTO`/`PAUSADA` não têm resultado consolidado (SDD §16 lista percentual/classificação/nível como campos do histórico); `AD-004` já usa "a mais recente `FINALIZADA`" como base de evolução | y |
| Escolha entre múltiplas `FINALIZADA` do mesmo grupo (aluno, ano, tipo, ciclo) | A mais recente por `finalizado_em` | Reaproveita `AD-004`, já implementado em `avaliacao` | y |
| Filtro `tipoLeitura` na evolução por ciclo e na comparação anual | Obrigatório | Os três tipos de leitura (SDD §2) não são comparáveis entre si numa mesma linha/série temporal; sem o filtro a evolução misturaria métricas de naturezas diferentes | y |
| Ano letivo na evolução por ciclo | Parâmetro opcional `anoLetivoId`; default = ano letivo com `situacao = ATIVO` | Consistente com o resto do sistema, que sempre tem um único ano ATIVO por vez (`cadastros-base`) | y |
| Paginação do histórico | `page`/tamanho fixo de 20, mesmo padrão de `AlunoController` | Reaproveita convenção já usada no projeto | y |
| Ownership do Professor no histórico | Verificado pela matrícula **ativa** do aluno (mesmo padrão de `AlunoController.buscarPorId`), não pelo professor snapshot de cada avaliação individual | Uma avaliação antiga pode ter um `professorNome`/`professor` snapshot de quem não é mais o professor atual (AD-005); o acesso ao histórico deve refletir quem é responsável pelo aluno **hoje** | y |

**Open questions:** none - todas resolvidas ou registradas acima.

---

## User Stories

### P1: Consultar histórico do aluno ⭐ MVP

**User Story**: Como professor ou coordenador, eu quero consultar o histórico de avaliações finalizadas de um aluno, para acompanhar o desempenho dele ao longo do tempo.

**Why P1**: É o RF012, pré-requisito para qualquer análise de evolução; sem ele não há como visualizar o que já foi avaliado.

**Acceptance Criteria**:

1. WHEN o professor ou coordenador consulta `GET /api/v1/alunos/{alunoId}/historico-avaliacoes` THEN o sistema SHALL retornar uma página (tamanho 20) das avaliações `FINALIZADA` do aluno, ordenadas por `dataAvaliacao` decrescente.
2. The system SHALL incluir em cada item do histórico: ano letivo, ano/série, turma, professor, ciclo, tipo de leitura, data da avaliação, quantidade de palavras, corretas, incorretas, não lidas, percentual de acerto, classificação (fase + nível), tempo utilizado e indicação de áudio disponível (SDD §16).
3. WHERE os parâmetros opcionais `anoLetivoId`, `tipoLeitura` e/ou `cicloId` forem informados THEN o sistema SHALL filtrar o histórico por esses valores.
4. IF o aluno não existir THEN o sistema SHALL responder 404 `ALUNO_NAO_ENCONTRADO`.
5. IF o usuário autenticado for PROFESSOR e não for o professor da matrícula ativa do aluno THEN o sistema SHALL responder 404 (mesmo comportamento de `AUTH-09`, sem revelar que o aluno existe).
6. IF o aluno não tiver nenhuma avaliação `FINALIZADA` (com ou sem os filtros aplicados) THEN o sistema SHALL retornar uma página vazia com status 200.

**Independent Test**: Finalizar avaliações de um aluno em ciclos/anos diferentes e conferir que `GET /historico-avaliacoes` devolve exatamente essas avaliações, paginadas e com os campos do SDD §16; conferir 404 para professor de outra turma.

---

### P1: Consultar evolução por ciclo ⭐ MVP

**User Story**: Como coordenador, eu quero ver a evolução de um aluno entre os ciclos Entrada, Acompanhamento e Saída de um ano letivo, para saber se o aluno está progredindo dentro do ano.

**Why P1**: É o RF013 / SDD §14, a visão mínima de progresso que justifica os três ciclos de avaliação.

**Acceptance Criteria**:

1. WHEN o coordenador consulta `GET /api/v1/alunos/{alunoId}/evolucao-ciclos?tipoLeitura={tipo}` THEN o sistema SHALL retornar, para o ano letivo informado (ou o ano ATIVO por padrão), o resultado (corretas, percentual de acerto, classificação) da avaliação `FINALIZADA` mais recente de cada um dos três ciclos, para o `tipoLeitura` informado.
2. IF o aluno não tiver avaliação `FINALIZADA` num determinado ciclo (para o ano/tipo pedidos) THEN o sistema SHALL retornar esse ciclo como ausente (`null`), sem erro.
3. IF o parâmetro `tipoLeitura` não for informado THEN o sistema SHALL responder 400.
4. IF o usuário autenticado não tiver papel COORDENADOR THEN o sistema SHALL responder 403.
5. IF o aluno não existir THEN o sistema SHALL responder 404 `ALUNO_NAO_ENCONTRADO`.

**Independent Test**: Finalizar avaliações de Entrada e Saída (sem Acompanhamento) de um aluno e verificar que a resposta traz os dois ciclos preenchidos e Acompanhamento `null`.

---

### P1: Comparar desempenho entre anos letivos ⭐ MVP

**User Story**: Como coordenador, eu quero comparar o desempenho de um aluno em anos letivos diferentes, ciclo a ciclo, para avaliar a evolução de longo prazo.

**Why P1**: É o RF014 / SDD §15, o requisito explícito de comparação anual do coordenador.

**Acceptance Criteria**:

1. WHEN o coordenador consulta `GET /api/v1/alunos/{alunoId}/evolucao-anos?tipoLeitura={tipo}` THEN o sistema SHALL retornar uma linha por ano letivo em que o aluno tiver ao menos uma avaliação `FINALIZADA` do `tipoLeitura` informado, com ano letivo, série (snapshot da avaliação) e o resultado mais recente de cada um dos três ciclos.
2. THE linhas SHALL vir ordenadas por ano letivo crescente.
3. WHEN houver dois anos letivos consecutivos com resultado no mesmo ciclo THEN o sistema SHALL calcular, por ciclo, a evolução absoluta (`atual - anterior`) e percentual (`(atual - anterior) / anterior * 100`) usando `quantidade_corretas`.
4. IF o resultado anterior de um ciclo for zero e o atual também for zero THEN o sistema SHALL retornar percentual 0.
5. IF o resultado anterior de um ciclo for zero e o atual for maior que zero THEN o sistema SHALL retornar percentual `null` e manter a evolução absoluta calculada.
6. IF o parâmetro `tipoLeitura` não for informado THEN o sistema SHALL responder 400.
7. IF o usuário autenticado não tiver papel COORDENADOR THEN o sistema SHALL responder 403.
8. IF o aluno não existir THEN o sistema SHALL responder 404 `ALUNO_NAO_ENCONTRADO`.
9. IF o aluno não tiver nenhuma avaliação `FINALIZADA` do `tipoLeitura` informado, em nenhum ano THEN o sistema SHALL retornar uma lista vazia com status 200.

**Independent Test**: Finalizar avaliações de Saída em 2026 (2º ano) e de Entrada/Saída em 2027 (3º ano) e verificar que a resposta traz as duas linhas com série correta e a evolução do ciclo Saída calculada entre os dois anos; Entrada de 2027 não tem "anterior" e não gera evolução.

---

## Edge Cases

- IF o aluno existir mas nunca tiver sido avaliado THEN todos os três endpoints SHALL retornar coleção vazia (200), nunca 404.
- IF houver mais de uma avaliação `FINALIZADA` no mesmo (aluno, ano letivo, tipo de leitura, ciclo) THEN o sistema SHALL usar a de maior `finalizadoEm` (AD-004).
- IF `anoLetivoId` informado na evolução por ciclo não existir THEN o sistema SHALL responder 404 `ANO_LETIVO_NAO_ENCONTRADO`.
- IF `cicloId` ou `tipoLeitura` informados no histórico não corresponderem a valores válidos do domínio fixo THEN o sistema SHALL responder 400.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --------------- | ----- | ----- | ------ |
| HIST-01 | P1: Histórico | Design | Pending |
| HIST-02 | P1: Histórico | Design | Pending |
| HIST-03 | P1: Histórico | Design | Pending |
| HIST-04 | P1: Histórico | Design | Pending |
| HIST-05 | P1: Histórico | Design | Pending |
| HIST-06 | P1: Histórico | Design | Pending |
| HIST-07 | P1: Evolução por ciclo | Design | Pending |
| HIST-08 | P1: Evolução por ciclo | Design | Pending |
| HIST-09 | P1: Evolução por ciclo | Design | Pending |
| HIST-10 | P1: Evolução por ciclo | Design | Pending |
| HIST-11 | P1: Evolução por ciclo | Design | Pending |
| HIST-12 | P1: Comparação anual | Design | Pending |
| HIST-13 | P1: Comparação anual | Design | Pending |
| HIST-14 | P1: Comparação anual | Design | Pending |
| HIST-15 | P1: Comparação anual | Design | Pending |
| HIST-16 | P1: Comparação anual | Design | Pending |
| HIST-17 | P1: Comparação anual | Design | Pending |
| HIST-18 | P1: Comparação anual | Design | Pending |
| HIST-19 | P1: Comparação anual | Design | Pending |
| HIST-20 | Edge cases | Design | Pending |
| HIST-21 | Edge cases | Design | Pending |
| HIST-22 | Edge cases | Design | Pending |

**Mapping**: HIST-01..06 = ACs 1-6 da história "Histórico"; HIST-07..11 = ACs 1-5 da história "Evolução por ciclo"; HIST-12..19 = ACs 1-8 da história "Comparação anual" (AC 9 é redundante com o edge case HIST-20 e não recebe ID próprio); HIST-20..22 = Edge Cases 2-4 (Edge Case 1 já coberto por HIST-06/HIST-09/HIST-19).

**ID format:** `HIST-[NUMBER]`.

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 22 total, 0 mapped to tasks, 22 unmapped ⚠️ (aguardando Design/Tasks)

---

## Success Criteria

- [ ] Professor consegue ver o histórico completo dos próprios alunos e recebe 404 para alunos de outro professor.
- [ ] Coordenador consegue comparar Entrada/Acompanhamento/Saída de um aluno dentro do mesmo ano e entre anos letivos diferentes, com evolução absoluta e percentual corretas (incluindo o caso de divisão por zero).
- [ ] Cobertura de linhas ≥ 85% (AD-007), sem regressão nos testes existentes de `avaliacao`/`cadastros-base`.

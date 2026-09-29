# Histórico e Evolução Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/historico-evolucao/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** (JaCoCo ≥85% de linhas; testes de integração contra MySQL real via Testcontainers). Amostra de testes existentes (`avaliacao`, já implementada e com Verifier PASS): `AvaliacaoServiceTest.java` (unit, Mockito puro, `MockitoExtension`), `AvaliacaoControllerIT.java`/`AvaliacaoRepositoryIT.java` (integration via `support.IntegrationTestBase` - container MySQL singleton, `bearerCoordenador()` pronto na base, `bearerProfessor()` emitido inline por `JwtService` em cada `*ControllerIT`). `AvaliacaoAudioRepository` hoje não tem um `*RepositoryIT` próprio (seus métodos são exercitados indiretamente por `AvaliacaoServiceTest`/`AvaliacaoControllerIT`) - esta feature cria o primeiro, só para o método novo que ela adiciona.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Repositório com query customizada (`AvaliacaoRepository`, 3 métodos novos) | integration | Cada filtro (isolado e combinado), exclusão de não-`FINALIZADA`, ordenação (`dataAvaliacao desc` no histórico; `ciclo, finalizadoEm desc` nas duas queries de evolução) | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepositoryIT.java` (estende o arquivo existente) | `./mvnw verify` |
| Repositório com query customizada (`AvaliacaoAudioRepository`, 1 método novo) | integration | `findAvaliacaoIdByAvaliacaoIdIn` retorna só os ids com áudio gravado, mesmo aviso outros ids | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAudioRepositoryIT.java` (novo arquivo) | `./mvnw verify` |
| Serviço de domínio (`HistoricoEvolucaoService`) | unit | Todos os branches; 1:1 com HIST-01..HIST-22 (spec.md); agrupamento "mais recente por grupo", divisão por zero e ownership (AUTH-09) cobertos explicitamente | `src/test/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoServiceTest.java` | `./mvnw test` |
| Controller (REST) (`HistoricoEvolucaoController`) + DTOs de resposta | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (400/401/403/404) do spec | `src/test/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoControllerIT.java` | `./mvnw verify` |
| DTOs de resposta (`records` em `historicoevolucao/dto/`) | none | - (build gate only; exercitados indiretamente pelas tasks de controller) | - | `./mvnw compile` |

## Gate Check Commands

> Reaproveita a configuração já existente do projeto (`maven-failsafe-plugin` para `*IT.java`, `jacoco-maven-plugin` com 85% de linhas na fase `verify` - nada novo a configurar).

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que só adicionam `*ServiceTest`/`*Test` (unit, sem Docker) | `./mvnw test` |
| Full | Após tasks que adicionam/alteram `*RepositoryIT`/`*ControllerIT` (integration, precisa Docker) | `./mvnw verify` |
| Build | Fechamento de fase | `./mvnw verify` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Repositório (leitura)

```
T1
T2
```

(T1 e T2 são independentes entre si - ambos rodam nesta fase, sem ordem obrigatória entre eles.)

### Phase 2: Serviço (domínio)

```
T1 -> T3
T2 -> T3
T3 -> T4
T1 -> T4
T4 -> T5
T1 -> T5
```

### Phase 3: Controller (API)

```
T3 -> T6
T4 -> T7
T6 -> T7
T5 -> T8
T7 -> T8
```

---

## Task Breakdown

### T1: Adicionar 3 métodos de leitura a `AvaliacaoRepository` ✅ Done

**What**: Adicionar `Page<Avaliacao> buscarHistorico(Long alunoId, StatusAvaliacao status, Long anoLetivoId, TipoLeituraCodigo tipoLeitura, Long cicloId, Pageable pageable)` (JPQL, filtros opcionais via `:param is null or ...`, `order by dataAvaliacao desc, finalizadoEm desc`), `List<Avaliacao> buscarFinalizadasPorAnoETipo(Long alunoId, StatusAvaliacao status, Long anoLetivoId, TipoLeituraCodigo tipoLeitura)` (`order by ciclo.id, finalizadoEm desc`) e `List<Avaliacao> buscarFinalizadasPorTipo(Long alunoId, StatusAvaliacao status, TipoLeituraCodigo tipoLeitura)` (`order by anoLetivo.ano, ciclo.id, finalizadoEm desc`), exatamente como especificado em design.md (Data Models).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepository.java` (modify)
**Depends on**: None
**Reuses**: Estilo de `@Query` com filtro opcional (`cadastros/aluno/AlunoRepository.java`); `StatusAvaliacao`/`TipoLeituraCodigo` já existentes
**Requirement**: HIST-01, HIST-03, HIST-04, HIST-05, HIST-06 (infra também para HIST-07..HIST-22, usada pelas tasks de serviço)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `buscarHistorico` filtra por `alunoId`+`status` sempre, e por `anoLetivoId`/`tipoLeitura`/`cicloId` só quando informados; ordena por `dataAvaliacao desc, finalizadoEm desc`; paginado
- [x] `buscarFinalizadasPorAnoETipo` filtra por `alunoId`+`status`+`anoLetivoId`+`tipoLeitura`; ordena por `ciclo.id, finalizadoEm desc`
- [x] `buscarFinalizadasPorTipo` filtra por `alunoId`+`status`+`tipoLeitura`; ordena por `anoLetivo.ano, ciclo.id, finalizadoEm desc`
- [x] Novos testes de integração em `AvaliacaoRepositoryIT`: cada filtro isolado, filtros combinados, `CANCELADA`/`EM_ANDAMENTO` nunca retornam, ordenação correta (inclui caso com 2 `FINALIZADA` no mesmo grupo para provar que a mais recente vem primeiro)
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 8 testes novos (9 novos)

**Tests**: integration
**Gate**: full

**Commit**: `feat(historico-evolucao): add avaliacao history and evolution queries to AvaliacaoRepository`

---

### T2: Adicionar `AvaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn` ✅ Done

**What**: Método derivado `List<Long> findAvaliacaoIdByAvaliacaoIdIn(List<Long> avaliacaoIds)`, para marcar `temAudio` numa página de histórico sem N+1 (design.md, Tech Decisions).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAudioRepository.java` (modify)
**Depends on**: None
**Reuses**: Padrão de método derivado já usado no mesmo arquivo (`existsByAvaliacaoId`)
**Requirement**: HIST-02

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Método derivado adicionado, sem alterar os métodos existentes (SPEC_DEVIATION: implementado com `@Query` explícito, não derivado puro - ver javadoc de `AvaliacaoAudioRepository`)
- [x] Novo arquivo `AvaliacaoAudioRepositoryIT` (primeiro da classe): ids com áudio voltam na lista; ids sem áudio não voltam; lista vazia de entrada retorna lista vazia
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 3 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(historico-evolucao): add findAvaliacaoIdByAvaliacaoIdIn to AvaliacaoAudioRepository`

---

### T3: Criar `HistoricoEvolucaoService.historico` + `comAudio` ✅ Done

**What**: Novo serviço `HistoricoEvolucaoService` com `Page<Avaliacao> historico(Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura, Long cicloId, Pageable pageable)` (resolve o aluno via `AlunoService.buscarPorId`, checa `PertencimentoProfessorGuard`, delega a `AvaliacaoRepository.buscarHistorico` com `status = FINALIZADA`) e `Set<Long> comAudio(List<Long> avaliacaoIds)` (delega a `AvaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn`).
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoService.java` (new)
**Depends on**: T1, T2
**Reuses**: `AlunoService.buscarPorId` (404 `ALUNO_NAO_ENCONTRADO`), `PertencimentoProfessorGuard.verificar` (404 AUTH-09), `AvaliacaoRepository.buscarHistorico`/`AvaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn` (T1, T2)
**Requirement**: HIST-01, HIST-03, HIST-04, HIST-05, HIST-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `historico` devolve a página tal como o repositório retorna, sempre com `status = FINALIZADA`
- [x] Aluno inexistente → `BusinessException(404, ALUNO_NAO_ENCONTRADO)`
- [x] Professor que não é o professor da matrícula ativa → 404 (guard); professor dono e coordenador passam
- [x] `comAudio` devolve o `Set<Long>` do repositório
- [x] Novos testes unitários em `HistoricoEvolucaoServiceTest`: paginação básica, cada filtro passado ao repositório, aluno inexistente, professor não-dono, professor dono, coordenador, `comAudio` com e sem ids
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 8 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(historico-evolucao): add HistoricoEvolucaoService.historico and comAudio`

---

### T4: Adicionar `HistoricoEvolucaoService.evolucaoPorCiclo` ✅ Done

**What**: Método `EvolucaoCiclos evolucaoPorCiclo(Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura)` (tipo de retorno interno do service, mapeado a DTO na T7): resolve `anoLetivoId` (informado → `AnoLetivoRepository.findById` ou 404 `ANO_LETIVO_NAO_ENCONTRADO`; omitido → `findBySituacao(ATIVO)`), busca `AvaliacaoRepository.buscarFinalizadasPorAnoETipo`, agrupa em memória por `ciclo.id` pegando o primeiro de cada grupo (já vem ordenado por `finalizadoEm desc` dentro do grupo - HIST-20), monta os três slots (`ENTRADA`/`ACOMPANHAMENTO`/`SAIDA`), `null` quando o ciclo não tem avaliação.
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoService.java` (modify)
**Depends on**: T1, T3
**Reuses**: `AnoLetivoRepository.findBySituacao`/`findById`, `AvaliacaoRepository.buscarFinalizadasPorAnoETipo` (T1), `AlunoService.buscarPorId` (mesmo aluno já resolvido em T3, reaproveitado)
**Requirement**: HIST-07, HIST-08, HIST-09, HIST-10, HIST-11, HIST-20, HIST-21

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Os 3 ciclos vêm preenchidos quando há `FINALIZADA` para cada um
- [x] Ciclo sem `FINALIZADA` vem `null`, sem erro
- [x] Duas `FINALIZADA` no mesmo ciclo → usa a de maior `finalizadoEm` (HIST-20)
- [x] `anoLetivoId` omitido usa o ano `ATIVO`; informado e existente usa esse ano; informado e inexistente → 404 `ANO_LETIVO_NAO_ENCONTRADO`
- [x] Aluno inexistente → 404 `ALUNO_NAO_ENCONTRADO` (reaproveita a resolução de T3)
- [x] Novos testes unitários em `HistoricoEvolucaoServiceTest`: 3 ciclos preenchidos, ciclo ausente, ciclo com 2 `FINALIZADA` (mais recente vence), ano default `ATIVO`, ano informado existente, ano informado inexistente, aluno inexistente
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 7 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(historico-evolucao): add HistoricoEvolucaoService.evolucaoPorCiclo`

---

### T5: Adicionar `HistoricoEvolucaoService.evolucaoAnual` ✅ Done

**What**: Método `List<EvolucaoAnualLinha> evolucaoAnual(Long alunoId, TipoLeituraCodigo tipoLeitura)` (tipo interno do service): busca `AvaliacaoRepository.buscarFinalizadasPorTipo`, agrupa em memória por `(anoLetivo.id, ciclo.id)` pegando o mais recente de cada subgrupo (HIST-20 reaproveitado), agrupa por ano (ordem já vem de `anoLetivo.ano` crescente da query - HIST-13), e para cada ciclo presente em 2 anos consecutivos calcula `EvolucaoValor` usando `quantidadeCorretas` (HIST-14, HIST-17): `absoluta = atual - anterior`; `percentual = null` se `anterior == 0 && atual > 0` (HIST-19), `0` se `anterior == 0 && atual == 0` (HIST-18), senão `(atual - anterior) / anterior * 100` arredondado 2 casas HALF_UP.
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoService.java` (modify)
**Depends on**: T1, T4
**Reuses**: `AvaliacaoRepository.buscarFinalizadasPorTipo` (T1); mesma lógica de "grupo → mais recente" de `evolucaoPorCiclo` (T4), extraída para um método privado compartilhado
**Requirement**: HIST-12, HIST-13, HIST-14, HIST-15, HIST-16, HIST-17, HIST-18, HIST-19, HIST-20

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Uma linha por ano letivo com pelo menos 1 `FINALIZADA` do tipo pedido, série da avaliação (snapshot), ordenadas por ano crescente
- [x] Ciclo repetido no mesmo ano/tipo → usa o de maior `finalizadoEm`
- [x] Ciclo presente em 2 anos consecutivos → `absoluta`/`percentual` calculados sobre `quantidadeCorretas`
- [x] `anterior == 0 && atual == 0` → `percentual = 0`; `anterior == 0 && atual > 0` → `percentual = null` e `absoluta` calculada
- [x] Ciclo sem ano anterior com esse ciclo (ex.: 1º ano com dado, ou ciclo que só apareceu esse ano) → `evolucao` com `absoluta`/`percentual` ambos `null`
- [x] Aluno sem nenhuma `FINALIZADA` do tipo pedido → lista vazia
- [x] Novos testes unitários em `HistoricoEvolucaoServiceTest`: linha por ano, ordenação, cálculo normal, os 2 casos de divisão por zero, ciclo sem anterior, ciclo repetido no grupo, lista vazia, métrica é `quantidadeCorretas` (não `percentualAcerto`) + aluno inexistente (spec.md AC8, não listado aqui mas exigido pela história)
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 9 testes novos (10 novos)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(historico-evolucao): add HistoricoEvolucaoService.evolucaoAnual`

---

### T6: Criar `HistoricoEvolucaoController` - `GET /historico-avaliacoes` ✅ Done

**What**: Novo controller com `GET /api/v1/alunos/{alunoId}/historico-avaliacoes?anoLetivoId=&tipoLeitura=&cicloId=&page=` (`hasAnyRole('PROFESSOR','COORDENADOR')`), delegando a `HistoricoEvolucaoService.historico` + `comAudio`, com os DTOs `HistoricoAvaliacaoItemResponse` (todos os campos do SDD §16, `temAudio` calculado a partir de `comAudio`).
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoController.java` (new)
**Depends on**: T3
**Reuses**: Estilo fino de `AlunoController`/`AvaliacaoController` (paginação `PageRequest.of(page, 20)`, `.map(DTO::from)`); DTOs novos em `historicoevolucao/dto/`
**Requirement**: HIST-01, HIST-02, HIST-03, HIST-04, HIST-05, HIST-06, HIST-22

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Rota registrada com os 2 papéis; resposta traz todos os campos do SDD §16 (ano letivo, série, turma, professor, ciclo, tipo de leitura, data, quantidades, percentual, classificação, tempo, `temAudio`)
- [x] Filtros `anoLetivoId`/`tipoLeitura`/`cicloId` opcionais funcionam isolados e combinados
- [x] `tipoLeitura`/`cicloId` com valor inválido → 400 (conversão do Spring, sem handler customizado)
- [x] Professor dono → 200; professor não-dono → 404; coordenador → 200 para qualquer aluno; aluno inexistente → 404; sem token → 401
- [x] Aluno sem `FINALIZADA` → 200 com página vazia
- [x] Novos testes de integração em `HistoricoEvolucaoControllerIT` cobrindo os pontos acima
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 8 testes novos (13 novos)

**Tests**: integration
**Gate**: full

**Commit**: `feat(historico-evolucao): add GET /alunos/{alunoId}/historico-avaliacoes endpoint`

---

### T7: Adicionar `GET /evolucao-ciclos` a `HistoricoEvolucaoController` ✅ Done

**What**: `GET /api/v1/alunos/{alunoId}/evolucao-ciclos?anoLetivoId=&tipoLeitura=` (`hasRole('COORDENADOR')`, `tipoLeitura` obrigatório), delegando a `HistoricoEvolucaoService.evolucaoPorCiclo`, com os DTOs `ResultadoCicloResponse`/`EvolucaoCiclosResponse`.
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoController.java` (modify)
**Depends on**: T4, T6
**Reuses**: `HistoricoEvolucaoService.evolucaoPorCiclo` (T4); mesmo padrão de mapeamento DTO de T6
**Requirement**: HIST-07, HIST-08, HIST-09, HIST-10, HIST-11, HIST-21

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Resposta traz os 3 ciclos (ou `null` quando ausente) para o ano/tipo pedidos
- [x] `tipoLeitura` ausente → 400; `anoLetivoId` inexistente → 404 `ANO_LETIVO_NAO_ENCONTRADO`; `anoLetivoId` omitido usa o ano `ATIVO`
- [x] Professor → 403; coordenador → 200; aluno inexistente → 404; sem token → 401
- [x] Novos testes de integração em `HistoricoEvolucaoControllerIT` cobrindo os pontos acima
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 7 testes novos (8 novos)

**Tests**: integration
**Gate**: full

**Commit**: `feat(historico-evolucao): add GET /alunos/{alunoId}/evolucao-ciclos endpoint`

---

### T8: Adicionar `GET /evolucao-anos` a `HistoricoEvolucaoController` ✅ Done

**What**: `GET /api/v1/alunos/{alunoId}/evolucao-anos?tipoLeitura=` (`hasRole('COORDENADOR')`, `tipoLeitura` obrigatório), delegando a `HistoricoEvolucaoService.evolucaoAnual`, com os DTOs `EvolucaoValor`/`CicloAnualResponse`/`EvolucaoAnualLinhaResponse`/`EvolucaoAnualResponse`.
**Where**: `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoController.java` (modify)
**Depends on**: T5, T7
**Reuses**: `HistoricoEvolucaoService.evolucaoAnual` (T5); mesmo padrão de mapeamento DTO de T6/T7
**Requirement**: HIST-12, HIST-13, HIST-14, HIST-15, HIST-16, HIST-17, HIST-18, HIST-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Resposta traz 1 linha por ano com `FINALIZADA`, ordenadas por ano crescente, com evolução absoluta/percentual por ciclo
- [x] Os 2 casos de divisão por zero (HIST-18/HIST-19) aparecem corretos na resposta HTTP
- [x] `tipoLeitura` ausente → 400; professor → 403; coordenador → 200; aluno inexistente → 404; aluno sem `FINALIZADA` do tipo → 200 lista vazia; sem token → 401
- [x] Novos testes de integração em `HistoricoEvolucaoControllerIT` cobrindo os pontos acima
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 8 testes novos (8 novos)

**Tests**: integration
**Gate**: full

**Commit**: `feat(historico-evolucao): add GET /alunos/{alunoId}/evolucao-anos endpoint`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1        T2
Phase 2:  T3 ------→ T4 ------→ T5
Phase 3:  T6 ------→ T7 ------→ T8
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

**Batching**: 8 tasks total → fits a single task-budgeted batch (≤ ~8 tasks). Execute runs inline, no sub-agent offer needed.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: 3 métodos novos em `AvaliacaoRepository` | 1 arquivo, mesma finalidade imediata (dar ao service o que ele precisa ler) | ✅ Granular (2-3 coisas relacionadas no mesmo arquivo) |
| T2: 1 método novo em `AvaliacaoAudioRepository` | 1 arquivo, 1 método | ✅ Granular |
| T3: `historico` + `comAudio` em `HistoricoEvolucaoService` | 1 arquivo (novo), 2 métodos da mesma história (RF012) | ✅ Granular |
| T4: `evolucaoPorCiclo` em `HistoricoEvolucaoService` | 1 arquivo, 1 método | ✅ Granular |
| T5: `evolucaoAnual` em `HistoricoEvolucaoService` | 1 arquivo, 1 método | ✅ Granular |
| T6: `GET /historico-avaliacoes` | 1 arquivo (novo controller) + DTOs da mesma rota | ✅ Granular (1 endpoint) |
| T7: `GET /evolucao-ciclos` | 1 arquivo (mesmo controller, novo método) + DTOs da mesma rota | ✅ Granular (1 endpoint) |
| T8: `GET /evolucao-anos` | 1 arquivo (mesmo controller, novo método) + DTOs da mesma rota | ✅ Granular (1 endpoint) |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Nenhuma seta de entrada (Phase 1) | ✅ Match |
| T2 | None | Nenhuma seta de entrada (Phase 1) | ✅ Match |
| T3 | T1, T2 | T1→T3, T2→T3 | ✅ Match |
| T4 | T1, T3 | T1→T4, T3→T4 | ✅ Match |
| T5 | T1, T4 | T1→T5, T4→T5 | ✅ Match |
| T6 | T3 | T3→T6 | ✅ Match |
| T7 | T4, T6 | T4→T7, T6→T7 | ✅ Match |
| T8 | T5, T7 | T5→T8, T7→T8 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: `AvaliacaoRepository` queries | Repositório | integration | integration | ✅ OK |
| T2: `AvaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn` | Repositório | integration | integration | ✅ OK |
| T3: `HistoricoEvolucaoService.historico`/`comAudio` | Serviço | unit | unit | ✅ OK |
| T4: `HistoricoEvolucaoService.evolucaoPorCiclo` | Serviço | unit | unit | ✅ OK |
| T5: `HistoricoEvolucaoService.evolucaoAnual` | Serviço | unit | unit | ✅ OK |
| T6: `GET /historico-avaliacoes` + DTOs | Controller + DTO | integration (DTO = none, controller manda) | integration | ✅ OK |
| T7: `GET /evolucao-ciclos` + DTOs | Controller + DTO | integration | integration | ✅ OK |
| T8: `GET /evolucao-anos` + DTOs | Controller + DTO | integration | integration | ✅ OK |

Nenhuma violação - todas as tasks testam no mesmo commit em que o código é criado, no tipo exigido pela matriz.

# Cadastros Base Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/cadastros-base/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** ("O build falha se a cobertura de linhas ficar abaixo de 85% [JaCoCo]... Os testes de integração usam MySQL real via Testcontainers"). Nenhum `AGENTS.md`/`CONTRIBUTING.md` no repositório. O único teste existente é `FluenciaLeitoraApplicationTests` (placeholder de context-load, sem código de domínio) - usado só para confirmar framework (JUnit 5 + `spring-boot-starter-*-test`), não como teto de profundidade.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Serviço de domínio (`*Service.java`) | unit | Todos os branches; 1:1 com os ACs do spec (CAD-01..CAD-17); todo edge case listado tem um teste | `src/test/java/com/missio/fluencia_leitora/cadastros/**/*ServiceTest.java` | `./mvnw test` |
| Infra comum (`common.error`, `common.security`) | unit | Cada tipo de exceção mapeada para o `ProblemDetail`/código certo; adapter de contexto lê os headers certos | `src/test/java/com/missio/fluencia_leitora/common/**/*Test.java` | `./mvnw test` |
| Repositório com query customizada (além do CRUD herdado do `JpaRepository`) | integration | Toda query customizada + toda constraint única/FK exercitada | `src/test/java/com/missio/fluencia_leitora/cadastros/**/*RepositoryIT.java` | `./mvnw verify` |
| Controller (REST) | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (422/404/409) do spec | `src/test/java/com/missio/fluencia_leitora/cadastros/**/*ControllerIT.java` | `./mvnw verify` |
| Entidade / migração Flyway / repositório CRUD puro (sem query customizada, ex.: `Professor`, `Ciclo`, `TipoLeitura`) | none | - (build gate only; coberto indiretamente pelos testes de controller da mesma fase) | - | `./mvnw compile` |

## Gate Check Commands

> Generated from codebase - confirm before Execute. `pom.xml` ainda não tem `maven-failsafe-plugin`, `jacoco-maven-plugin` nem o driver MySQL/Testcontainers - a T1 adiciona tudo isso. Convenção adotada: `*Test.java` roda no Surefire (fase `test`, sem Docker); `*IT.java` roda no Failsafe (fases `integration-test`/`verify`, precisa de Docker para os Testcontainers); `jacoco:check` (85% de linhas, AD-007) fica amarrado à fase `verify`.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que só adicionam `*ServiceTest`/`*Test` (unit, sem Docker) | `./mvnw test` |
| Full | Após tasks que adicionam `*RepositoryIT`/`*ControllerIT` (integration, precisa Docker rodando) | `./mvnw verify` |
| Build | Fechamento de fase, ou tasks que só criam migração Flyway/entidade/repositório CRUD puro sem teste novo | `./mvnw compile` (e `./mvnw verify` no fechamento de cada fase, para confirmar o gate de cobertura da AD-007 sobre tudo que a fase acumulou) |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Fundação (dependências, erro, segurança provisória, domínios fixos)

```
T1 -> T2
T1 -> T3
T1 -> T4
T4 -> T5
```

### Phase 2: Ano Letivo e Configuração de Palavras

```
T6 -> T7
T7 -> T8
T8 -> T9
T8 -> T10
T9 -> T11
T10 -> T11
```

### Phase 3: Professor e Turma

```
T12 -> T13
T13 -> T14
T14 -> T15
T14 -> T16
T15 -> T17
T16 -> T18
```

### Phase 4: Aluno e Matrícula - domínio

```
T19 -> T20
T20 -> T21
T21 -> T23
T23 -> T24
T22 -> T25
T23 -> T25
T21 -> T26
T26 -> T27
```

`T22` (`HistoricoAvaliacaoPort`+stub) não depende de nenhuma outra task desta fase - sua única dependência (`T1`) é de uma fase anterior.

### Phase 5: Aluno e Matrícula - API

```
T28
T29
```

`T28` e `T29` não dependem uma da outra - cada uma só depende de tasks de fases anteriores (T2, T24, T25 para T28; T2, T26, T27 para T29). Executam em sequência (T28 antes de T29) só pela ordem de listagem, não por dependência.

---

## Task Breakdown

### T1: Adicionar dependências e configuração de build (MySQL, Testcontainers, Failsafe, JaCoCo)

**What**: Adicionar ao `pom.xml` o driver `mysql-connector-j`, `testcontainers-mysql` + `testcontainers-junit-jupiter` (scope test), `maven-failsafe-plugin` (roda `**/*IT.java` nas fases `integration-test`/`verify`) e `jacoco-maven-plugin` com `check` amarrado à fase `verify` exigindo `LINE` `COVEREDRATIO >= 0.85` (AD-007); configurar `src/main/resources/application.yaml` com `spring.datasource.*` via variáveis de ambiente e `spring.flyway.locations=classpath:db/migration`.
**Where**: `pom.xml`, `src/main/resources/application.yaml`
**Depends on**: None
**Reuses**: N/A (primeira task do projeto)
**Requirement**: N/A (infraestrutura)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `pom.xml` compila com as novas dependências e plugins
- [x] `./mvnw compile` passa
- [x] `jacoco-maven-plugin` está configurado com `check` na fase `verify` (85% linhas)
- [x] `maven-failsafe-plugin` está configurado para `**/*IT.java`

**Tests**: none
**Gate**: build

---

### T2: Criar `BusinessException` e `GlobalExceptionHandler` (RFC 7807)

**What**: Criar `BusinessException(HttpStatus status, String code, String message)` e `GlobalExceptionHandler` (`@RestControllerAdvice extends ResponseEntityExceptionHandler`) que: (a) mapeia `BusinessException` para `ProblemDetail` com a extensão `code`; (b) sobrescreve `handleMethodArgumentNotValid` para devolver 422 com os campos inválidos; (c) mapeia `ObjectOptimisticLockingFailureException` para 409 com `code=CONFLITO_DE_VERSAO`.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/error/BusinessException.java`, `.../common/error/GlobalExceptionHandler.java`
**Depends on**: T1
**Reuses**: N/A
**Requirement**: CAD-20 (parte genérica; o caso concreto de lock otimista é testado na T11)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `BusinessException` carrega status HTTP + código + mensagem
- [x] `GlobalExceptionHandler` devolve 422 em erro de validação (`@Valid`), 409 em `BusinessException`/lock otimista, com `code` no `ProblemDetail`
- [x] `./mvnw test` passa
- [x] 3 testes (um por tipo de exceção mapeada) usando `MockMvcBuilders.standaloneSetup(...)` com um `@RestController` de teste que dispara cada exceção

**Tests**: unit
**Gate**: quick

---

### T3: Criar `ContextoUsuarioPort` e adapter provisório por headers HTTP

**What**: Criar `Perfil` (`COORDENADOR`, `PROFESSOR`), interface `ContextoUsuarioPort { Perfil perfilAtual(); Long professorIdAtual(); }` e `ContextoUsuarioHeaderAdapter implements ContextoUsuarioPort` (`@Component`, request-scoped) que lê `X-Perfil` e `X-Professor-Id` do `HttpServletRequest`, com javadoc explicando que é provisório até a feature `autenticacao-perfis` substituir o bean.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/Perfil.java`, `.../common/security/ContextoUsuarioPort.java`, `.../common/security/ContextoUsuarioHeaderAdapter.java`
**Depends on**: T1
**Reuses**: N/A
**Requirement**: CAD-16 (habilita o escopo por professor, usado na T24/T28)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `ContextoUsuarioHeaderAdapter` lê `X-Perfil`/`X-Professor-Id` e retorna os valores certos (ou `null`/`COORDENADOR` por padrão quando ausentes)
- [x] `./mvnw test` passa
- [x] 3 testes: perfil COORDENADOR sem header de professor, perfil PROFESSOR com `professorId`, ausência de headers (default)

**Tests**: unit
**Gate**: quick

---

### T4: Migração Flyway - domínios fixos (`ciclo`, `tipo_leitura`)

**What**: Criar `V1__dominios_fixos.sql` com as tabelas `ciclo` e `tipo_leitura` e o seed exato do SDD §19 (`ENTRADA`/`ACOMPANHAMENTO`/`SAIDA`; `PALAVRA`/`PSEUDOPALAVRA`/`TEXTO_CURTO`).
**Where**: `src/main/resources/db/migration/V1__dominios_fixos.sql`
**Depends on**: T1
**Reuses**: DDL do SDD §19 (`ciclo`, `tipo_leitura`)
**Requirement**: CAD-18

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Migração roda sem erro (`./mvnw compile` + Flyway validate na próxima task que sobe o contexto)
- [x] Seed tem exatamente os 3 ciclos e os 3 tipos de leitura, na ordem do SDD

**Tests**: none
**Gate**: build

---

### T5: Entidades, repositórios e controller de domínios fixos (`Ciclo`, `TipoLeitura`)

**What**: Criar as entidades `Ciclo`/`TipoLeitura` (somente leitura), `CicloRepository`/`TipoLeituraRepository` (`JpaRepository`, sem query customizada) e `DominioFixoController` com `GET /api/v1/ciclos` e `GET /api/v1/tipos-leitura`. Sem service (é passthrough, não há regra de negócio) e sem `@PostMapping`/`@PutMapping`/`@DeleteMapping` nesses paths.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/dominio/` (`Ciclo.java`, `TipoLeitura.java`, `CicloRepository.java`, `TipoLeituraRepository.java`, `DominioFixoController.java`, `dto/CicloResponse.java`, `dto/TipoLeituraResponse.java`)
**Depends on**: T4
**Reuses**: `common.error` (não se aplica aqui, mas o padrão de DTO já fica definido para as próximas features)
**Requirement**: CAD-18

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `GET /api/v1/ciclos` retorna os 3 ciclos na ordem do seed
- [x] `GET /api/v1/tipos-leitura` retorna os 3 tipos na ordem do seed
- [x] `POST`/`PUT`/`DELETE` nesses paths retornam 405
- [x] `./mvnw verify` passa (Testcontainers MySQL sobe e aplica `V1__dominios_fixos.sql`)
- [x] 3 testes: `GET /ciclos`, `GET /tipos-leitura`, `POST /ciclos` → 405

**Tests**: integration
**Gate**: full

---

### T6: Migração Flyway - `ano_letivo` e `configuracao_avaliacao`

**What**: Criar `V2__ano_letivo.sql` com `ano_letivo` (`situacao ENUM('PLANEJADO','ATIVO','ENCERRADO')`, `ativo BOOLEAN NOT NULL DEFAULT TRUE`, `version BIGINT NOT NULL DEFAULT 0`, `uk_ano_letivo(ano)`) e `configuracao_avaliacao` (FK para `ano_letivo`, `uk_config_ano_serie(ano_letivo_id, serie)`, `chk_config_quantidade`, `version`).
**Where**: `src/main/resources/db/migration/V2__ano_letivo.sql`
**Depends on**: T4
**Reuses**: DDL do SDD §19 (`ano_letivo`, `configuracao_avaliacao`), adaptado por AD (design.md, seção Data Models)
**Requirement**: CAD-01, CAD-04, CAD-05, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Migração cria as duas tabelas com as constraints acima
- [x] `chk_config_quantidade` (`quantidade_minima <= quantidade_maxima`) presente

**Tests**: none
**Gate**: build

---

### T7: Entidade e repositório `AnoLetivo`

**What**: Criar `SituacaoAnoLetivo` (enum) e a entidade `AnoLetivo` (`@Version` em `version`) e `AnoLetivoRepository` com `existsByAno(int ano)` e `findBySituacao(SituacaoAnoLetivo situacao)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/SituacaoAnoLetivo.java`, `.../cadastros/anoletivo/AnoLetivo.java`, `.../cadastros/anoletivo/AnoLetivoRepository.java`
**Depends on**: T6
**Reuses**: N/A
**Requirement**: CAD-01, CAD-02, CAD-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `existsByAno` e `findBySituacao` retornam o esperado contra MySQL real
- [x] `./mvnw verify` passa
- [x] 3 testes: `existsByAno` true/false, `findBySituacao(ATIVO)` com 0 e com 1 resultado

**Tests**: integration
**Gate**: full

---

### T8: Entidade e repositório `ConfiguracaoAvaliacao`

**What**: Criar a entidade `ConfiguracaoAvaliacao` (`@Version`) e `ConfiguracaoAvaliacaoRepository` com `findByAnoLetivoIdAndSerie(Long anoLetivoId, int serie)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/ConfiguracaoAvaliacao.java`, `.../cadastros/anoletivo/ConfiguracaoAvaliacaoRepository.java`
**Depends on**: T7
**Reuses**: N/A
**Requirement**: CAD-05, CAD-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `findByAnoLetivoIdAndSerie` retorna o esperado
- [x] Inserir duas configurações com o mesmo `(ano_letivo_id, serie)` viola `uk_config_ano_serie`
- [x] `./mvnw verify` passa
- [x] 2 testes: busca encontrada/vazia, violação da constraint única

**Tests**: integration
**Gate**: full

---

### T9: `AnoLetivoService` (criar com seed, ativar)

**What**: Implementar `AnoLetivoService.criar(CriarAnoLetivoRequest)` (transacional: cria o `AnoLetivo` `PLANEJADO` + as 5 `ConfiguracaoAvaliacao` do seed - série 1 = 15-20, séries 2-5 = 20-60 -, lança `BusinessException` 409 `ANO_LETIVO_DUPLICADO` se `existsByAno`) e `AnoLetivoService.ativar(Long id)` (transacional: se houver um `AnoLetivo` `ATIVO`, muda para `ENCERRADO`; ativa o novo).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/AnoLetivoService.java`
**Depends on**: T8
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-01, CAD-02, CAD-04, CAD-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `criar` grava o ano + as 5 configurações numa única transação e lança 409 se duplicado
- [x] `ativar` encerra o `ATIVO` anterior (se houver) e ativa o novo
- [x] `./mvnw test` passa
- [x] 4 testes: criar com sucesso (5 configs certas), criar duplicado → 409, ativar sem `ATIVO` anterior, ativar com `ATIVO` anterior (encerra + ativa)

**Tests**: unit
**Gate**: quick

---

### T10: `ConfiguracaoAvaliacaoService.atualizar`

**What**: Implementar `ConfiguracaoAvaliacaoService.atualizar(Long anoLetivoId, int serie, AtualizarConfiguracaoRequest)`: valida `serie` em 1-5, `quantidadeMinima >= 1`, `quantidadeMaxima <= 200`, `quantidadeMinima <= quantidadeMaxima` (lança `BusinessException` 422 se violado), grava e retorna 200.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/ConfiguracaoAvaliacaoService.java`
**Depends on**: T8
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Atualização válida grava os novos limites
- [x] `quantidadeMinima > quantidadeMaxima`, `quantidadeMinima < 1`, `quantidadeMaxima > 200` e `serie` fora de 1-5 retornam 422 sem alterar o registro
- [x] `./mvnw test` passa
- [x] 5 testes (1 sucesso + 4 violações acima)

**Tests**: unit
**Gate**: quick

---

### T11: `AnoLetivoController` (CRUD + ativação + configuração)

**What**: Implementar `AnoLetivoController` com `POST /api/v1/anos-letivos`, `POST /api/v1/anos-letivos/{id}/ativar`, `PUT /api/v1/anos-letivos/{id}/configuracoes/{serie}`, `DELETE /api/v1/anos-letivos/{id}` (seta `ativo=false`), e os DTOs de request/response com Bean Validation (`ano` 2000-2100, `dataFim` posterior a `dataInicio` via validador cross-field).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/AnoLetivoController.java`, `.../cadastros/anoletivo/dto/*`
**Depends on**: T9, T10, T2
**Reuses**: `common.error.BusinessException`, `common.error.GlobalExceptionHandler`
**Requirement**: CAD-01, CAD-02, CAD-03, CAD-04, CAD-06, CAD-19, CAD-20

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `POST /anos-letivos` cria com sucesso (201 + 5 configs), duplicado (409), datas/ano inválidos (422)
- [x] `POST /{id}/ativar` encerra o `ATIVO` anterior
- [x] `PUT /{id}/configuracoes/{serie}` atualiza (200) e rejeita inválido (422)
- [x] `DELETE /{id}` retorna 204 e a linha continua no banco com `ativo=false` (nunca `DELETE FROM`)
- [x] Duas requisições `PUT` concorrentes na mesma configuração: a segunda recebe 409 `CONFLITO_DE_VERSAO`
- [x] `./mvnw verify` passa
- [x] 8 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

**Commit**: `feat(cadastros-anoletivo): CRUD de ano letivo, ativação e configuração de palavras por série`

---

### T12: Migração Flyway - `professor` e `turma`

**What**: Criar `V3__professor_turma.sql` com `professor` (`ativo`, `version`) e `turma` (FK para `ano_letivo` e `professor`, `version`, índice único case-insensitive em `(ano_letivo_id, nome)` via coluna gerada ou `UNIQUE KEY` sobre `LOWER(nome)`/collation `_ai_ci`).
**Where**: `src/main/resources/db/migration/V3__professor_turma.sql`
**Depends on**: T6
**Reuses**: DDL do SDD §19 (`professor`, `turma`)
**Requirement**: CAD-07, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Migração cria as duas tabelas com as FKs e a unicidade case-insensitive de `turma.nome` por `ano_letivo_id`

**Tests**: none
**Gate**: build

---

### T13: Entidade e repositório `Professor`

**What**: Criar a entidade `Professor` (`@Version`) e `ProfessorRepository` (`JpaRepository`, sem query customizada - CRUD puro).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/professor/Professor.java`, `.../cadastros/professor/ProfessorRepository.java`
**Depends on**: T12
**Reuses**: N/A
**Requirement**: CAD-07

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Entidade mapeia todas as colunas da migração T12
- [x] `./mvnw compile` passa (cobertura de persistência vem da T17, que exercita o repositório via Testcontainers)

**Tests**: none
**Gate**: build

---

### T14: Entidade e repositório `Turma`

**What**: Criar a entidade `Turma` (`@Version`) e `TurmaRepository` com `existsByNomeIgnoreCaseAndAnoLetivoId(String nome, Long anoLetivoId)` e `findByProfessorIdAndAtivoTrue(Long professorId)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/turma/Turma.java`, `.../cadastros/turma/TurmaRepository.java`
**Depends on**: T13, T7
**Reuses**: N/A
**Requirement**: CAD-07, CAD-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `existsByNomeIgnoreCaseAndAnoLetivoId` ignora maiúsculas/minúsculas de fato contra MySQL real
- [x] `findByProfessorIdAndAtivoTrue` retorna só as turmas ativas
- [x] `./mvnw verify` passa
- [x] 3 testes: duplicidade case-insensitive, busca por professor com 0/N turmas ativas

**Tests**: integration
**Gate**: full

---

### T15: `ProfessorService`

**What**: Implementar `ProfessorService.criar(CriarProfessorRequest)`, `ProfessorService.buscarComTurmas(Long id)` (retorna professor + turmas ativas via `TurmaRepository`) e `ProfessorService.inativar(Long id)` (lança `BusinessException` 409 `PROFESSOR_COM_TURMA_ATIVA` se `findByProfessorIdAndAtivoTrue` não vazio).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/professor/ProfessorService.java`
**Depends on**: T14
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-07, CAD-08, CAD-09

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `criar` valida `nome` 3-150 e grava
- [x] `buscarComTurmas` traz a lista de turmas ativas (0..n)
- [x] `inativar` bloqueia com 409 quando há turma ativa vinculada
- [x] `./mvnw test` passa
- [x] 6 testes cobrindo os pontos acima (criar sucesso/422, buscarComTurmas 0/n, inativar bloqueado/gravado - a dupla cobertura de cada guarda evita um mutante "sempre lança"/"nunca lança")

**Tests**: unit
**Gate**: quick

---

### T16: `TurmaService`

**What**: Implementar `TurmaService.criar(CriarTurmaRequest)` (valida `professorId`/`anoLetivoId` existentes e ativos, lança 409 `TURMA_DUPLICADA` se `existsByNomeIgnoreCaseAndAnoLetivoId`, 422 se referência inválida) e `TurmaService.atualizarProfessor(Long turmaId, Long novoProfessorId)` (troca o professor responsável sem tocar em avaliações).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/turma/TurmaService.java`
**Depends on**: T14
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-07, CAD-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `criar` grava com sucesso; rejeita duplicidade (409) e referência inválida/inativa (422)
- [x] `atualizarProfessor` grava o novo professor
- [x] `./mvnw test` passa
- [x] 6 testes cobrindo os pontos acima (criar com/sem professor, duplicidade, ano inativo, professor inativo, atualizarProfessor)

**Deviation:** adicionado `TurmaService.inativar(Long id)` (soft-delete, seta `ativo=false`), fora do texto original da task mas exigido pelo design.md (seção `cadastros.turma`) e pela T18 (`DELETE /turmas/{id}`). Mesmo padrão de `AnoLetivoService.inativar` (T11); sem teste unitário dedicado, consistente com o precedente (exercitado via `*ControllerIT` na T18).

**Tests**: unit
**Gate**: quick

---

### T17: `ProfessorController`

**What**: Implementar `ProfessorController` com `POST /api/v1/professores`, `GET /api/v1/professores/{id}` (com turmas ativas) e `DELETE /api/v1/professores/{id}` (soft-delete, bloqueando com 409 se turma ativa).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/professor/ProfessorController.java`, `.../cadastros/professor/dto/*`
**Depends on**: T15, T2
**Reuses**: `common.error`
**Requirement**: CAD-07, CAD-08, CAD-09, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `POST /professores` cria (201)
- [x] `GET /professores/{id}` retorna as turmas ativas associadas
- [x] `DELETE /professores/{id}` retorna 204 (linha permanece com `ativo=false`) ou 409 se houver turma ativa
- [x] `./mvnw verify` passa
- [x] 4 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

**Commit**: `feat(cadastros-professor): CRUD de professor com consulta de turmas ativas`

---

### T18: `TurmaController`

**What**: Implementar `TurmaController` com `POST /api/v1/turmas`, `PUT /api/v1/turmas/{id}` (troca de professor responsável) e `DELETE /api/v1/turmas/{id}` (soft-delete).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/turma/TurmaController.java`, `.../cadastros/turma/dto/*`
**Depends on**: T16, T2
**Reuses**: `common.error`
**Requirement**: CAD-07, CAD-10, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `POST /turmas` cria (201), rejeita duplicidade (409) e referência inválida (422)
- [x] `PUT /{id}` troca o professor e não altera avaliações (não há avaliações nesta feature; o teste confirma que só a turma é alterada)
- [x] `DELETE /{id}` retorna 204 sem excluir a linha
- [x] `./mvnw verify` passa
- [x] 5 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

**Commit**: `feat(cadastros-turma): CRUD de turma com vínculo a professor e ano letivo`

---

### T19: Migração Flyway - `aluno` e `matricula`

**What**: Criar `V4__aluno_matricula.sql` com `aluno` (`nome VARCHAR(150) COLLATE utf8mb4_0900_ai_ci`, índice em `nome`, `ativo`, `version`) e `matricula` (FKs para `aluno`, `ano_letivo`, `turma`, `professor`; `serie` e `ano_finalizado`; `version`; `UNIQUE KEY uk_matricula_aluno_ano (aluno_id, ano_letivo_id)`).
**Where**: `src/main/resources/db/migration/V4__aluno_matricula.sql`
**Depends on**: T6, T12
**Reuses**: DDL do SDD §19 (`aluno`), adaptado por AD-005 (split `aluno`/`matricula`) e pela decisão de collation do design.md (Risks & Concerns)
**Requirement**: CAD-11, CAD-13, CAD-16, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Migração cria as duas tabelas com as FKs, a coluna `nome` com collation `utf8mb4_0900_ai_ci` e a constraint única de matrícula por aluno+ano

**Tests**: none
**Gate**: build

---

### T20: Entidade e repositório `Aluno` (busca por nome)

**What**: Criar a entidade `Aluno` (`@Version`) e `AlunoRepository` com uma query customizada de busca por nome (contém, sem diferenciar maiúsculas/acentos, paginada, ordenada por nome), com uma variante que também filtra por `professorId` da matrícula ativa.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/Aluno.java`, `.../cadastros/aluno/AlunoRepository.java`
**Depends on**: T19
**Reuses**: N/A
**Requirement**: CAD-16

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Buscar "joao" encontra "João Silva" mas não "Joana" incorretamente incluído/excluído por acento/maiúscula (confirma contra MySQL real que a collation `utf8mb4_0900_ai_ci` resolve o caso do design.md - risco de collation)
- [x] Paginação de 20 itens por página e ordenação por nome funcionam
- [x] Filtro por `professorId` retorna só os alunos daquele professor
- [x] `./mvnw verify` passa
- [x] 4 testes cobrindo os pontos acima

**Confirmado**: risco de collation do design.md resolvido - `buscarPorNome("joao")` contra MySQL real (Testcontainers) encontra "João Silva" e não inclui "Joana" (`AlunoRepositoryIT.buscarPorNomeEncontraComAcentoECaseInsensitiveSemIncluirNomeDiferente`). O filtro por professor (`buscarPorNomeEProfessor`) usa `nativeQuery` com JOIN direto nas tabelas `matricula`/`ano_letivo` (a entidade `Matricula` só existe a partir da T21) e já reproduz CAD-16 por completo (professorId + ano letivo `ATIVO`).

**Tests**: integration
**Gate**: full

---

### T21: Entidade e repositório `Matricula`

**What**: Criar a entidade `Matricula` (`@Version`) e `MatriculaRepository` com `existsByAlunoIdAndAnoLetivoId(Long alunoId, Long anoLetivoId)` e `findByAlunoId(Long alunoId)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/Matricula.java`, `.../cadastros/aluno/MatriculaRepository.java`
**Depends on**: T20, T14, T7
**Reuses**: N/A
**Requirement**: CAD-12, CAD-13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `existsByAlunoIdAndAnoLetivoId` detecta duplicidade
- [x] Inserir duas matrículas do mesmo aluno no mesmo ano letivo viola `uk_matricula_aluno_ano`
- [x] `./mvnw verify` passa
- [x] 2 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

---

### T22: `HistoricoAvaliacaoPort` e stub

**What**: Criar a interface `HistoricoAvaliacaoPort { boolean existeAvaliacaoNaoCancelada(Long alunoId); }` e `HistoricoAvaliacaoPortStub implements HistoricoAvaliacaoPort` (`@Component`, sempre `false`), com javadoc explicando que a feature `avaliacao` deve fornecer um bean `@Primary` real quando existir.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/HistoricoAvaliacaoPort.java`, `.../cadastros/aluno/HistoricoAvaliacaoPortStub.java`
**Depends on**: T1
**Reuses**: N/A
**Requirement**: CAD-15

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `HistoricoAvaliacaoPortStub.existeAvaliacaoNaoCancelada` retorna `false` para qualquer `alunoId`
- [x] `./mvnw test` passa
- [x] 1 teste confirmando o contrato do stub

**Tests**: unit
**Gate**: quick

---

### T23: `AlunoService.criarComMatricula`

**What**: Implementar `AlunoService.criarComMatricula(CriarAlunoRequest)`: transação única que cria `Aluno` + `Matricula` (série/professor copiados da turma), valida turma ativa e ano letivo não `ENCERRADO` (422 se violado), reverte tudo se qualquer passo falhar.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoService.java`
**Depends on**: T21
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-11

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Sucesso cria aluno + matrícula com `serie`/`professorId` copiados da turma
- [x] Turma inativa ou ano letivo `ENCERRADO` → 422, nada é persistido
- [x] `./mvnw test` passa
- [x] 3 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T24: `AlunoService.buscar`

**What**: Implementar `AlunoService.buscar(String termo, Pageable, ContextoUsuarioPort)`: valida `termo` com pelo menos 2 caracteres (422 se violado), e quando `ContextoUsuarioPort.perfilAtual() == PROFESSOR`, restringe aos alunos com matrícula no ano letivo `ATIVO` cujo `professorId` é o do contexto (CAD-16).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoService.java` (mesmo arquivo da T23, método novo)
**Depends on**: T23, T3
**Reuses**: `common.security.ContextoUsuarioPort`
**Requirement**: CAD-16

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Termo com menos de 2 caracteres → 422
- [x] Perfil `COORDENADOR` não tem filtro por professor
- [x] Perfil `PROFESSOR` só recebe os próprios alunos (mock do port com dois `professorId` diferentes)
- [x] `./mvnw test` passa
- [x] 3 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T25: `AlunoService.atualizarNome` e `AlunoService.inativar`

**What**: Implementar `AlunoService.atualizarNome(Long id, String novoNome)` (lança `BusinessException` 409 `ALUNO_COM_AVALIACAO` quando `HistoricoAvaliacaoPort.existeAvaliacaoNaoCancelada(id)` for `true`) e `AlunoService.inativar(Long id)` (seta `ativo=false`, soft-delete).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoService.java` (mesmo arquivo, métodos novos)
**Depends on**: T22, T23
**Reuses**: `cadastros.aluno.HistoricoAvaliacaoPort`
**Requirement**: CAD-15, CAD-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `atualizarNome` grava quando o port (mock) retorna `false`
- [x] `atualizarNome` retorna 409 quando o port (mock) retorna `true`, sem alterar o nome
- [x] `inativar` seta `ativo=false` sem excluir a linha
- [x] `./mvnw test` passa
- [x] 3 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T26: `MatriculaService.matricular`

**What**: Implementar `MatriculaService.matricular(Long alunoId, NovaMatriculaRequest)`: cria nova `Matricula` para o mesmo `alunoId` em outro ano letivo, valida turma ativa/ano não `ENCERRADO` (422), lança 409 `MATRICULA_DUPLICADA` se `existsByAlunoIdAndAnoLetivoId`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaService.java`
**Depends on**: T21
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-12, CAD-13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Nova matrícula em outro ano letivo cria com sucesso, ligada ao mesmo `alunoId`
- [x] Matrícula duplicada no mesmo ano letivo → 409
- [x] Turma inativa/ano `ENCERRADO` → 422
- [x] `./mvnw test` passa
- [x] 3 testes cobrindo os pontos acima

**Deviation:** implementados 4 testes em vez de 3 - a validação de turma inválida (`TURMA_INVALIDA`) e a de ano letivo `ENCERRADO` (`ANO_LETIVO_ENCERRADO`) são branches independentes de `AlunoService.buscarTurmaValidaParaMatricula` (reutilizado aqui) e cada um recebe seu próprio teste, para que um mutante em qualquer um dos dois branches seja pego dentro desta classe também (mesmo padrão já usado na T23).

**Tests**: unit
**Gate**: quick

---

### T27: `MatriculaService.atualizar` (professor, turma, ano finalizado)

**What**: Implementar `MatriculaService.atualizar(Long matriculaId, AtualizarMatriculaRequest)`: troca `professorId` e/ou transfere para outra turma do mesmo ano letivo (atualiza `serie`), e marca `anoFinalizado=true`; nada disso altera avaliações anteriores (não existem nesta feature, mas o service não toca em nenhuma outra tabela além de `matricula`).
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaService.java` (mesmo arquivo, método novo)
**Depends on**: T26
**Reuses**: `common.error.BusinessException`
**Requirement**: CAD-14, CAD-17

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Troca de professor grava o novo `professorId`
- [ ] Transferência de turma no mesmo ano letivo atualiza `turma`/`serie`
- [ ] `anoFinalizado=true` é gravado
- [ ] `./mvnw test` passa
- [ ] 3 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T28: `AlunoController`

**What**: Implementar `AlunoController` com `POST /api/v1/alunos`, `GET /api/v1/alunos?nome={termo}` (paginado), `PUT /api/v1/alunos/{id}` e `DELETE /api/v1/alunos/{id}`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoController.java`, `.../cadastros/aluno/dto/*`
**Depends on**: T24, T25, T2
**Reuses**: `common.error`, `common.security.ContextoUsuarioPort`
**Requirement**: CAD-11, CAD-15, CAD-16, CAD-19, CAD-20

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `POST /alunos` cria aluno + matrícula (201, `alunoId` + `matriculaId`)
- [ ] `GET /alunos?nome=joao` encontra "João Silva" sem achar "Joana" indevidamente; termo curto → 422
- [ ] `GET /alunos?nome=` com header `X-Perfil: PROFESSOR` + `X-Professor-Id` só retorna os alunos daquele professor (professor B não vê os alunos do professor A)
- [ ] `PUT /alunos/{id}` altera nome com sucesso e retorna 409 quando bloqueado (mock/estado sem avaliação, já que a tabela `avaliacao` não existe)
- [ ] `DELETE /alunos/{id}` retorna 204 sem excluir a linha
- [ ] `./mvnw verify` passa
- [ ] 7 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

**Commit**: `feat(cadastros-aluno): cadastro de aluno com matrícula, busca escopada por professor e bloqueio de nome`

---

### T29: `MatriculaController`

**What**: Implementar `MatriculaController` com `POST /api/v1/alunos/{alunoId}/matriculas` e `PATCH /api/v1/matriculas/{id}`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaController.java`, `.../cadastros/aluno/dto/*`
**Depends on**: T26, T27, T2
**Reuses**: `common.error`
**Requirement**: CAD-12, CAD-13, CAD-14, CAD-17

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `POST /alunos/{alunoId}/matriculas` cria nova matrícula no ano seguinte com o mesmo `alunoId` (2 matrículas confirmadas via `GET` ou consulta direta)
- [ ] Matrícula duplicada no mesmo ano → 409
- [ ] `PATCH /matriculas/{id}` troca professor/turma e marca `anoFinalizado`
- [ ] `./mvnw verify` passa
- [ ] 4 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

**Commit**: `feat(cadastros-matricula): nova matrícula em outro ano letivo e atualização de professor/turma/ano finalizado`

---

## Phase Execution Map

Arestas intra-fase (dependência real dentro da mesma fase):

```
T1 -> T2
T1 -> T3
T1 -> T4
T4 -> T5
T6 -> T7
T7 -> T8
T8 -> T9
T8 -> T10
T9 -> T11
T10 -> T11
T12 -> T13
T13 -> T14
T14 -> T15
T14 -> T16
T15 -> T17
T16 -> T18
T19 -> T20
T20 -> T21
T21 -> T23
T23 -> T24
T22 -> T25
T23 -> T25
T21 -> T26
T26 -> T27
```

Arestas cross-fase (backward, validadas pelo check de "forward-phase dependency", não pelo diagrama):

```
T4 -> T6
T6 -> T12
T6 -> T19
T12 -> T19
T7 -> T14
T7 -> T21
T14 -> T21
T1 -> T22
T2 -> T11
T2 -> T17
T2 -> T18
T2 -> T28
T2 -> T29
T3 -> T24
T24 -> T28
T25 -> T28
T26 -> T29
T27 -> T29
```

As fases em si executam em sequência: Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5.

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

**How phase-based execution works:** ver `SKILL.md` (packing em lotes de ~7 tasks por fase completa, oferta de sub-agentes só se houver mais de 1 lote).

Total: 29 tasks → ~4-5 lotes de ~7 tasks. Ofereço a delegação por sub-agentes antes de começar o Execute.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Dependências e build | 1 arquivo de config (`pom.xml`) + 1 arquivo (`application.yaml`) | ✅ Granular (config, não código de domínio) |
| T2: `BusinessException` + `GlobalExceptionHandler` | 2 arquivos, 1 conceito (erro RFC 7807) | ✅ Granular (coeso) |
| T3: `ContextoUsuarioPort` + adapter | 3 arquivos pequenos, 1 conceito | ✅ Granular (coeso) |
| T4: Migração `ciclo`/`tipo_leitura` | 1 arquivo SQL | ✅ Granular |
| T5: Entidades+repos+controller de domínio fixo | 6 arquivos, mas sem regra de negócio (passthrough) | ✅ Granular (sem service; ver nota no task) |
| T6: Migração `ano_letivo`/`configuracao_avaliacao` | 1 arquivo SQL | ✅ Granular |
| T7: Entidade+repo `AnoLetivo` | 3 arquivos, 1 agregado | ✅ Granular |
| T8: Entidade+repo `ConfiguracaoAvaliacao` | 2 arquivos, 1 agregado | ✅ Granular |
| T9: `AnoLetivoService` | 1 arquivo, 1 serviço | ✅ Granular |
| T10: `ConfiguracaoAvaliacaoService` | 1 arquivo, 1 serviço | ✅ Granular |
| T11: `AnoLetivoController` | 1 controller + DTOs, 1 agregado | ✅ Granular |
| T12: Migração `professor`/`turma` | 1 arquivo SQL | ✅ Granular |
| T13: Entidade+repo `Professor` | 2 arquivos | ✅ Granular |
| T14: Entidade+repo `Turma` | 2 arquivos | ✅ Granular |
| T15: `ProfessorService` | 1 arquivo | ✅ Granular |
| T16: `TurmaService` | 1 arquivo | ✅ Granular |
| T17: `ProfessorController` | 1 controller + DTOs | ✅ Granular |
| T18: `TurmaController` | 1 controller + DTOs | ✅ Granular |
| T19: Migração `aluno`/`matricula` | 1 arquivo SQL | ✅ Granular |
| T20: Entidade+repo `Aluno` | 2 arquivos | ✅ Granular |
| T21: Entidade+repo `Matricula` | 2 arquivos | ✅ Granular |
| T22: `HistoricoAvaliacaoPort`+stub | 2 arquivos pequenos | ✅ Granular |
| T23: `AlunoService.criarComMatricula` | 1 método | ✅ Granular |
| T24: `AlunoService.buscar` | 1 método (mesmo arquivo da T23) | ✅ Granular |
| T25: `AlunoService.atualizarNome`+`inativar` | 2 métodos coesos (mesmo arquivo) | ✅ Granular (coeso) |
| T26: `MatriculaService.matricular` | 1 método | ✅ Granular |
| T27: `MatriculaService.atualizar` | 1 método (mesmo arquivo da T26) | ✅ Granular |
| T28: `AlunoController` | 1 controller + DTOs, 1 agregado | ✅ Granular |
| T29: `MatriculaController` | 1 controller + DTOs, 1 agregado | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | nenhuma seta entrando em T1 | ✅ Match |
| T2 | T1 | T1→T2 | ✅ Match |
| T3 | T1 | T1→T3 | ✅ Match |
| T4 | T1 | T1→T4 | ✅ Match |
| T5 | T4 | T4→T5 | ✅ Match |
| T6 | T4 (cross-fase) | sem seta intra-fase exigida (T4 é da Phase 1) | ✅ Match |
| T7 | T6 | T6→T7 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T8 | T8→T9 | ✅ Match |
| T10 | T8 | T8→T10 | ✅ Match |
| T11 | T9, T10, T2 (cross-fase) | T9→T11, T10→T11 | ✅ Match |
| T12 | T6 (cross-fase) | sem seta intra-fase exigida (T6 é da Phase 2) | ✅ Match |
| T13 | T12 | T12→T13 | ✅ Match |
| T14 | T13, T7 (cross-fase) | T13→T14 | ✅ Match |
| T15 | T14 | T14→T15 | ✅ Match |
| T16 | T14 | T14→T16 | ✅ Match |
| T17 | T15, T2 (cross-fase) | T15→T17 | ✅ Match |
| T18 | T16, T2 (cross-fase) | T16→T18 | ✅ Match |
| T19 | T6, T12 (cross-fase) | sem seta intra-fase exigida (ambos de fases anteriores) | ✅ Match |
| T20 | T19 | T19→T20 | ✅ Match |
| T21 | T20, T14, T7 (cross-fase) | T20→T21 | ✅ Match |
| T22 | T1 (cross-fase) | sem seta intra-fase exigida (T1 é da Phase 1) | ✅ Match |
| T23 | T21 | T21→T23 | ✅ Match |
| T24 | T23, T3 (cross-fase) | T23→T24 | ✅ Match |
| T25 | T22, T23 | T22→T25, T23→T25 | ✅ Match |
| T26 | T21 | T21→T26 | ✅ Match |
| T27 | T26 | T26→T27 | ✅ Match |
| T28 | T24, T25, T2 (todas cross-fase) | sem seta intra-fase exigida (Phase 5 não tem dependência intra-fase) | ✅ Match |
| T29 | T26, T27, T2 (todas cross-fase) | sem seta intra-fase exigida | ✅ Match |

Nenhum `Depends on` aponta para uma task de fase posterior. Toda dependência cross-fase aponta para trás (validada pelo check de forward-phase dependency); a paridade de diagrama só é exigida - e só existe - para as dependências intra-fase acima, todas casadas com uma seta.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: Build config | Entity/config | none | none | ✅ OK |
| T2: `GlobalExceptionHandler` | Infra comum | unit | unit | ✅ OK |
| T3: `ContextoUsuarioPort` | Infra comum | unit | unit | ✅ OK |
| T4: Migração domínios fixos | Migração Flyway | none | none | ✅ OK |
| T5: Domínios fixos (entidade+repo+controller) | Controller (maior exigência entre entidade/repo/controller do grupo) | integration | integration | ✅ OK |
| T6: Migração ano letivo | Migração Flyway | none | none | ✅ OK |
| T7: `AnoLetivo` entidade+repo | Repositório com query customizada | integration | integration | ✅ OK |
| T8: `ConfiguracaoAvaliacao` entidade+repo | Repositório com query customizada | integration | integration | ✅ OK |
| T9: `AnoLetivoService` | Serviço de domínio | unit | unit | ✅ OK |
| T10: `ConfiguracaoAvaliacaoService` | Serviço de domínio | unit | unit | ✅ OK |
| T11: `AnoLetivoController` | Controller | integration | integration | ✅ OK |
| T12: Migração professor/turma | Migração Flyway | none | none | ✅ OK |
| T13: `Professor` entidade+repo | Entidade/repo CRUD puro (sem query customizada) | none | none | ✅ OK |
| T14: `Turma` entidade+repo | Repositório com query customizada | integration | integration | ✅ OK |
| T15: `ProfessorService` | Serviço de domínio | unit | unit | ✅ OK |
| T16: `TurmaService` | Serviço de domínio | unit | unit | ✅ OK |
| T17: `ProfessorController` | Controller | integration | integration | ✅ OK |
| T18: `TurmaController` | Controller | integration | integration | ✅ OK |
| T19: Migração aluno/matrícula | Migração Flyway | none | none | ✅ OK |
| T20: `Aluno` entidade+repo | Repositório com query customizada | integration | integration | ✅ OK |
| T21: `Matricula` entidade+repo | Repositório com query customizada | integration | integration | ✅ OK |
| T22: `HistoricoAvaliacaoPort`+stub | Infra/port simples | unit | unit | ✅ OK |
| T23: `AlunoService.criarComMatricula` | Serviço de domínio | unit | unit | ✅ OK |
| T24: `AlunoService.buscar` | Serviço de domínio | unit | unit | ✅ OK |
| T25: `AlunoService.atualizarNome`/`inativar` | Serviço de domínio | unit | unit | ✅ OK |
| T26: `MatriculaService.matricular` | Serviço de domínio | unit | unit | ✅ OK |
| T27: `MatriculaService.atualizar` | Serviço de domínio | unit | unit | ✅ OK |
| T28: `AlunoController` | Controller | integration | integration | ✅ OK |
| T29: `MatriculaController` | Controller | integration | integration | ✅ OK |

Nenhuma violação. `T13` é o único caso de `Tests: none` fora de migração/config puro - justificado porque `Professor` não tem nenhuma query customizada (CRUD herdado do `JpaRepository`) e sua persistência é exercitada de ponta a ponta pela `T17` (`ProfessorController` IT) na mesma fase.

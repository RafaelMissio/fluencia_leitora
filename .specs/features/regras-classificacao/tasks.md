# Regras de Classificação Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/regras-classificacao/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** (JaCoCo ≥85% linhas; Testcontainers MySQL nos testes de integração). Amostra de testes existentes (`cadastros-base`, `autenticacao-perfis`, `banco-palavras`, já implementadas): `*ServiceTest.java` unit com Mockito, `*RepositoryIT.java`/`*ControllerIT.java` integration via `IntegrationTestBase` (Testcontainers MySQL, singleton container, `bearerCoordenador()` já disponível). Esta feature segue o mesmo padrão, com um tipo de teste novo: concorrência real (duas transações) para o lock pessimista (design.md, Risks & Concerns).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Infra de segurança (`ContextoUsuarioPort`/`JwtContextoUsuarioAdapter`) | unit | Novo método `usuarioIdAtual()` retorna o `usuarioId` do `UsuarioAutenticado` atual; sem regressão nos métodos existentes (`perfilAtual`/`professorIdAtual`) | `src/test/java/com/missio/fluencia_leitora/common/security/JwtContextoUsuarioAdapterTest.java` (estende o arquivo existente) | `./mvnw test` |
| Infra de erro (`GlobalExceptionHandler`) | unit | Falha de validação em `@PathVariable`/`@RequestParam` (`serie` fora de 1-5) mapeia para 422 `VALIDACAO_INVALIDA`; sem regressão nos handlers existentes | `src/test/java/com/missio/fluencia_leitora/common/error/GlobalExceptionHandlerTest.java` (estende o arquivo existente) | `./mvnw test` |
| Serviço de domínio (`RegraClassificacaoService`) | unit | Todos os branches; 1:1 com REG-01..REG-13 (REG-14 deferido para `avaliacao`, ver spec.md); todo edge case listado (Edge Cases + Implicit-requirement sweep, exceto concorrência) tem um teste | `src/test/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoServiceTest.java` | `./mvnw test` |
| Repositório com lock pessimista e queries customizadas (`RegraClassificacaoRepository`) | integration | Lock pessimista exercitado com **duas transações reais concorrentes** (não duas chamadas sequenciais no mesmo thread - ver design.md Risks & Concerns); `buscarHistoricoPorSerie` ordena com o grupo corrente (`alteradoEm=null`) primeiro | `src/test/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoRepositoryIT.java` | `./mvnw verify` |
| Controller (REST) (`RegraClassificacaoController`) | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (401/403/422) do spec | `src/test/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoControllerIT.java` | `./mvnw verify` |
| Entidade (`RegraClassificacao`) / enum (`Fase`) / migração Flyway (`V7`) / DTOs | none | - (build gate only; exercitadas indiretamente pelas tasks de repositório/serviço/controller) | - | `./mvnw compile` |

## Gate Check Commands

> Reaproveita a configuração já existente do projeto (`maven-failsafe-plugin` para `*IT.java`, `jacoco-maven-plugin` com 85% de linhas na fase `verify` - ambos configurados em `cadastros-base`/T1, nada novo a configurar aqui).

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que só adicionam `*ServiceTest`/`*Test` (unit, sem Docker) | `./mvnw test` |
| Full | Após tasks que adicionam/alteram `*RepositoryIT`/`*ControllerIT` (integration, precisa Docker) | `./mvnw verify` |
| Build | Fechamento de fase, ou tasks só de migração/entidade/enum/DTO sem teste novo | `./mvnw compile` (e `./mvnw verify` no fechamento de cada fase) |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Infraestrutura compartilhada

```
T1
T2
T3
T4
```

### Phase 2: Modelo de domínio (enum + entidade JPA)

```
T3 -> T5
T4 -> T5
```

### Phase 3: Persistência

```
T5 -> T6
```

### Phase 4: DTOs

```
T4 -> T7
T4 -> T8
T5 -> T8
```

### Phase 5: Serviço de domínio

```
T6 -> T9
T6 -> T10
T1 -> T11
T6 -> T11
T7 -> T11
```

### Phase 6: Controller (REST + segurança)

```
T2 -> T12
T8 -> T12
T10 -> T12
T11 -> T13
T12 -> T13
T10 -> T14
T13 -> T14
```

---

## Task Breakdown

### T1: Estender `ContextoUsuarioPort`/`JwtContextoUsuarioAdapter` com `usuarioIdAtual()`

**What**: Adicionar o método `Long usuarioIdAtual()` à interface `ContextoUsuarioPort` e implementá-lo em `JwtContextoUsuarioAdapter`, retornando `usuarioAtual().usuarioId()` (já existe em `UsuarioAutenticado`).
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/ContextoUsuarioPort.java`, `src/main/java/com/missio/fluencia_leitora/common/security/JwtContextoUsuarioAdapter.java` (modify)
**Depends on**: None
**Reuses**: `UsuarioAutenticado.usuarioId()` (já existe, `common/security/UsuarioAutenticado.java:1`)
**Requirement**: N/A (infra para REG-13)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `ContextoUsuarioPort` declara `usuarioIdAtual()`; `JwtContextoUsuarioAdapter` implementa lendo o `usuarioId` do principal atual
- [x] Testes existentes em `JwtContextoUsuarioAdapterTest` (`perfilAtual`/`professorIdAtual`) continuam passando sem alteração
- [x] Novo teste: `usuarioIdAtual()` retorna o `usuarioId` do `UsuarioAutenticado` colocado no `SecurityContext`
- [x] Nenhum mock existente de `ContextoUsuarioPort` quebra por não implementar o método novo
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 1 teste novo em `JwtContextoUsuarioAdapterTest` (mais os existentes, sem regressão)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(regras-classificacao): add usuarioIdAtual to ContextoUsuarioPort`

---

### T2: Estender `GlobalExceptionHandler` para validação de `@PathVariable`/`@RequestParam`

**What**: Sobrescrever o handler do Spring MVC para violação de restrições em parâmetros de método (`@PathVariable`/`@RequestParam` anotados com `@Min`/`@Max`, ex. `serie`) para devolver 422 com `code=VALIDACAO_INVALIDA`, no mesmo formato de `handleMethodArgumentNotValid` (que já cobre `@Valid @RequestBody`). **Verificar antes de implementar**: o projeto usa Spring Boot 4.1.1 (Spring Framework 7); confirmar na fonte de `ResponseEntityExceptionHandler` o nome/assinatura exatos do método a sobrescrever para esse tipo de exceção (o mecanismo de validação de parâmetro de método mudou a partir do Spring Framework 6.1 - não presumir a API de versões antigas nem usar `ConstraintViolationException`/`@Validated` na classe sem confirmar que é o que essa versão realmente lança).
**Where**: `src/main/java/com/missio/fluencia_leitora/common/error/GlobalExceptionHandler.java` (modify)
**Depends on**: None
**Reuses**: padrão de `handleMethodArgumentNotValid` já existente no mesmo arquivo (código `VALIDACAO_INVALIDA`, `ProblemDetail` com `code`)
**Requirement**: N/A (infra - design.md, Error Handling Strategy: "`serie` fora de 1-5 no path")

**Tools**:

- MCP: `context7` (confirmar a API de validação de parâmetro de método do Spring Framework 7 antes de implementar)
- Skill: NONE

**Done when**:

- [x] Uma rota de teste com `@Min`/`@Max` num `@PathVariable`/`@RequestParam` fora do intervalo devolve 422 com `code=VALIDACAO_INVALIDA` (não o 400 default do Spring)
- [x] Handlers existentes (`BusinessException`, `ObjectOptimisticLockingFailureException`, `MethodArgumentNotValidException`) continuam passando sem alteração
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 1 teste novo em `GlobalExceptionHandlerTest` (mais os existentes, sem regressão)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(regras-classificacao): map method-parameter validation to VALIDACAO_INVALIDA`

---

### T3: Migração Flyway `V7__regra_classificacao.sql`

**What**: Criar a tabela `regra_classificacao` conforme o DDL do `design.md` (colunas, `CHECK`s de série/mínimo/intervalo/nível, FK para `usuario`, índice `serie+ativo`) e o seed contíguo aprovado (série 1 com 6 faixas; séries 2 a 5 com as mesmas 6 faixas cada, repetidas por extenso - 4 blocos de `INSERT` quase idênticos, sem abstração, conforme a "Nota de execução" do design.md).
**Where**: `src/main/resources/db/migration/V7__regra_classificacao.sql`
**Depends on**: None
**Reuses**: padrão de migração já usado em `V1`..`V6`
**Requirement**: REG-01, REG-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `regra_classificacao` criada exatamente como no `design.md` (incluindo `ck_regra_classificacao_serie`, `ck_regra_classificacao_minima`, `ck_regra_classificacao_intervalo`, `ck_regra_classificacao_nivel`, `fk_regra_classificacao_usuario`, `idx_regra_classificacao_filtro`)
- [x] Seed grava exatamente as faixas da série 1 (6 linhas) e as mesmas faixas para as séries 2, 3, 4 e 5 (6 linhas cada, 24 no total) - valores conferidos contra a tabela de Assumptions do spec.md
- [x] Aplicação sobe sem erro de migração (`./mvnw compile` + contexto Spring Boot inicia num teste de integração já existente, ex. `FluenciaLeitoraApplicationIT`)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(regras-classificacao): add regra_classificacao migration with seed`

---

### T4: Criar enum `Fase`

**What**: Enum Java `Fase { PRE_LEITOR, LEITOR_INICIANTE, LEITOR_FLUENTE }`.
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/Fase.java`
**Depends on**: None
**Reuses**: mesma decisão de `bancopalavras.TipoPalavra` (enum Java puro, sem FK - design.md, Components)
**Requirement**: N/A (suporte a REG-03..REG-15)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Enum compila com exatamente os 3 valores do `design.md`
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(regras-classificacao): add Fase enum`

---

### T5: Criar entidade `RegraClassificacao`

**What**: Entidade JPA mapeando `regra_classificacao` (`serie`, `quantidadeMinimaAcertos`, `quantidadeMaximaAcertos` nullable, `fase` `@Enumerated(STRING)`, `nivel` nullable, `ativo`, `alteradoPor`/`alteradoEm` nullable, `criadoEm`), com o método `inativar(Long alteradoPor, Instant agora)` que seta `ativo=false` + os dois campos de auditoria numa só chamada.
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacao.java`
**Depends on**: T3, T4
**Reuses**: padrão de entidade de `bancopalavras.ListaPalavras` (construtor protegido sem args para o JPA, sem setters públicos além dos necessários)
**Requirement**: N/A (suporte a REG-03..REG-15)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Entidade mapeia todas as colunas de `regra_classificacao` (T3)
- [ ] `inativar(Long, Instant)` seta `ativo=false`, `alteradoPor` e `alteradoEm` numa só chamada
- [ ] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(regras-classificacao): add RegraClassificacao entity`

---

### T6: Criar `RegraClassificacaoRepository`

**What**: Interface `JpaRepository<RegraClassificacao, Long>` com três consultas: `findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(int serie)` (GET e `classificar`); `buscarAtivasParaAtualizarComLock(int serie)` com `@Lock(PESSIMISTIC_WRITE)` (usada só dentro de `substituir`, serializa dois `PUT` concorrentes na mesma série); `buscarHistoricoPorSerie(int serie)` com `ORDER BY (alterado_em IS NULL) DESC, alterado_em DESC` (grupo corrente primeiro).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoRepository.java`
**Depends on**: T5
**Reuses**: padrão `@Query` + `JpaRepository` de `bancopalavras.ListaPalavrasRepository`
**Requirement**: REG-03, REG-06, REG-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc` retorna só as faixas `ativo=true` da série, ordenadas por `quantidadeMinimaAcertos`
- [ ] `buscarHistoricoPorSerie` retorna todas as faixas (ativas e inativas) da série, com o grupo corrente (`alteradoEm=null`) primeiro, depois `alteradoEm DESC`
- [ ] **Teste de concorrência real**: duas transações concorrentes (threads/`TransactionTemplate` distintos, não duas chamadas sequenciais no mesmo thread) chamando `buscarAtivasParaAtualizarComLock` na mesma série - a segunda só prossegue depois que a primeira comita/faz rollback (lock pessimista provado, não apenas presumido)
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 4 tests pass em `RegraClassificacaoRepositoryIT`

**Tests**: integration
**Gate**: full

**Commit**: `feat(regras-classificacao): add RegraClassificacaoRepository with pessimistic lock`

---

### T7: Criar DTOs de request

**What**: Records `FaixaRequest(Integer quantidadeMinimaAcertos, Integer quantidadeMaximaAcertos, Fase fase, Integer nivel)` (`@NotNull @Min(0)` em `quantidadeMinimaAcertos`; `@Min(0)` em `quantidadeMaximaAcertos`, nullable; `@NotNull` em `fase`; `nivel` sem anotação de bound - cross-field, validado no service) e `SubstituirRegrasClassificacaoRequest(@NotNull @Valid List<@NotNull FaixaRequest> faixas)`. **Importante**: a lista NÃO leva `@NotEmpty`/`@Size(min=1)` de propósito - lista vazia deve produzir o código de negócio `FAIXA_NAO_INICIA_EM_ZERO` no service (Edge Cases do spec), não o `VALIDACAO_INVALIDA` genérico do Bean Validation; a checagem de vazio fica no service (T11).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/dto/FaixaRequest.java`, `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/dto/SubstituirRegrasClassificacaoRequest.java`
**Depends on**: T4
**Reuses**: padrão de DTO record de `bancopalavras.dto.ItemPalavraRequest`/`CriarListaPalavrasRequest` (lição L-019/L-023: `@NotNull` no elemento da lista evita elemento nulo passando por `@Valid` sem erro)
**Requirement**: N/A (suporte a REG-07..REG-13)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] As duas classes compilam com as anotações de validação acima
- [ ] `SubstituirRegrasClassificacaoRequest` NÃO tem `@NotEmpty`/`@Size(min=1)` na lista `faixas` (confirmado por leitura do código, não é testável por Bean Validation)
- [ ] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(regras-classificacao): add request DTOs with bean validation`

---

### T8: Criar DTOs de response

**What**: Records `RegraClassificacaoResponse(Long id, int serie, Integer quantidadeMinimaAcertos, Integer quantidadeMaximaAcertos, Fase fase, Integer nivel, boolean ativo, Long alteradoPor, Instant alteradoEm)` (com `from(RegraClassificacao)`) e `HistoricoVersaoResponse(Instant alteradoEm, Long alteradoPor, List<RegraClassificacaoResponse> faixas)` (um grupo do histórico; `alteradoEm=null` identifica o grupo corrente).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/dto/RegraClassificacaoResponse.java`, `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/dto/HistoricoVersaoResponse.java`
**Depends on**: T4, T5
**Reuses**: padrão `from(...)` estático de `bancopalavras.dto.ListaPalavrasResponse`
**Requirement**: N/A (suporte a REG-06, REG-15)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `RegraClassificacaoResponse.from(...)` mapeia todos os campos de `RegraClassificacao`
- [ ] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(regras-classificacao): add response DTOs`

---

### T9: Implementar `RegraClassificacaoService.classificar(int serie, int acertos)`

**What**: Método `ClassificacaoResultado classificar(int serie, int acertos)` (`record ClassificacaoResultado(Fase fase, Integer nivel)`), que busca as faixas ativas da série (`findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc`) e retorna a faixa cujo intervalo cobre `acertos` (ou `fase=null, nivel=null` quando nenhuma cobre - REG-05). **Chamada Java direta, sem HTTP** - assinatura estável para `avaliacao` chamar depois.
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoService.java`
**Depends on**: T6
**Reuses**: `RegraClassificacaoRepository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc` (T6)
**Requirement**: REG-03, REG-04, REG-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Testes parametrizados com a tabela do AC1 (série 1: 0,3,4,5,6,7,8,11,12,20 acertos → resultados exatos do spec.md)
- [ ] Testes parametrizados com a tabela do AC2 (séries 2 a 5: 0,4,5,7,8,9,10,11,12,30,31,60 acertos → resultados exatos do spec.md)
- [ ] Só faixas `ativo=true` são consideradas (REG-03) - teste com uma faixa inativa que, se considerada, mudaria o resultado
- [ ] O resultado não depende de nenhum parâmetro de tipo de leitura - mesma chamada, mesmo resultado para os três tipos (REG-04)
- [ ] Nenhuma faixa cobrindo `acertos` retorna `ClassificacaoResultado(null, null)` em vez de lançar exceção (REG-05)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 24 tests pass em `RegraClassificacaoServiceTest` (método `classificar`) - 10 (AC1) + 12 (AC2) + REG-03 + REG-04/05

**Tests**: unit
**Gate**: quick

**Commit**: `feat(regras-classificacao): add RegraClassificacaoService.classificar`

---

### T10: Implementar `RegraClassificacaoService.buscarAtivas(int serie)` e `buscarHistorico(int serie)`

**What**: Dois métodos de leitura: `buscarAtivas(int serie)` (delega em `findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc`, REG-06) e `buscarHistorico(int serie)` (delega em `buscarHistoricoPorSerie`, agrupa por `alteradoEm` na ordem já dada pelo repositório, REG-15).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoService.java` (modify)
**Depends on**: T6
**Reuses**: `RegraClassificacaoRepository` (T6)
**Requirement**: REG-06, REG-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `buscarAtivas` retorna as faixas ativas da série ordenadas por `quantidadeMinimaAcertos`
- [ ] `buscarHistorico` agrupa as faixas retornadas pelo repositório por `alteradoEm`, preservando a ordem (grupo corrente primeiro)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 3 tests pass em `RegraClassificacaoServiceTest` (métodos `buscarAtivas`/`buscarHistorico`)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(regras-classificacao): add RegraClassificacaoService read methods`

---

### T11: Implementar `RegraClassificacaoService.substituir(int serie, List<FaixaRequest> faixas)`

**What**: Método `List<RegraClassificacao> substituir(int serie, List<FaixaRequest> faixas)` (`@Transactional`), com os validadores privados: lista vazia ou primeira faixa não começa em 0 → `FAIXA_NAO_INICIA_EM_ZERO`; lacuna entre faixas consecutivas → `FAIXA_COM_LACUNA` com `details={"valor": N}` (primeiro valor descoberto); sobreposição (inclui faixa não-última com `quantidadeMaximaAcertos=null`, que "engole" a próxima) → `FAIXA_SOBREPOSTA` com `details={"valor": N}` (primeiro valor duplicado); última faixa com `quantidadeMaximaAcertos != null` → `FAIXA_FINAL_LIMITADA`; `fase=PRE_LEITOR` sem `nivel` 1-4, ou `fase != PRE_LEITOR` com `nivel` preenchido → `FAIXA_NIVEL_INCOERENTE`; `quantidadeMinimaAcertos > quantidadeMaximaAcertos` na mesma faixa → `VALIDACAO_INVALIDA`. Se toda validação passar: busca as faixas ativas com lock pessimista (`buscarAtivasParaAtualizarComLock`), inativa cada uma via `RegraClassificacao.inativar(usuarioIdAtual, Instant.now())`, salva as novas faixas `ativo=true`, tudo na mesma transação (REG-07); se qualquer validação falhar, nada é persistido (REG-12, por não ter alterado nenhum estado ainda quando a exceção é lançada).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoService.java` (modify)
**Depends on**: T1, T6, T7
**Reuses**: `BusinessException` (com `details`, AD-008); `RegraClassificacaoRepository.buscarAtivasParaAtualizarComLock` (T6); `ContextoUsuarioPort.usuarioIdAtual()` (T1); `RegraClassificacao.inativar` (T5); estrutura de validadores privados de `bancopalavras.ListaPalavrasService`
**Requirement**: REG-07, REG-08, REG-09, REG-10, REG-11, REG-12, REG-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Conjunto de faixas válido substitui as anteriores: antigas ficam `ativo=false` com `alteradoPor`/`alteradoEm` preenchidos, novas ficam `ativo=true` (REG-07, REG-13)
- [ ] Lista vazia → `FAIXA_NAO_INICIA_EM_ZERO`
- [ ] Primeira faixa não começa em 0 → `FAIXA_NAO_INICIA_EM_ZERO` (REG-08)
- [ ] Lacuna entre faixas consecutivas → `FAIXA_COM_LACUNA` com o primeiro valor descoberto em `details.valor` (REG-09)
- [ ] Sobreposição entre faixas (mínimos duplicados) → `FAIXA_SOBREPOSTA` com o primeiro valor duplicado em `details.valor` (REG-10)
- [ ] Faixa não-última com `quantidadeMaximaAcertos=null` → `FAIXA_SOBREPOSTA` na faixa seguinte
- [ ] Última faixa com `quantidadeMaximaAcertos != null` → `FAIXA_FINAL_LIMITADA` (REG-11)
- [ ] `fase=PRE_LEITOR` com `nivel` fora de 1-4, ou `fase != PRE_LEITOR` com `nivel` preenchido → `FAIXA_NIVEL_INCOERENTE` (REG-11)
- [ ] `quantidadeMinimaAcertos > quantidadeMaximaAcertos` na mesma faixa → `VALIDACAO_INVALIDA`
- [ ] Qualquer falha de validação não altera as faixas anteriores (REG-12) - teste chama `substituir` com payload inválido e confirma que `buscarAtivas` continua retornando o conjunto anterior
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 12 tests pass em `RegraClassificacaoServiceTest` (método `substituir`)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(regras-classificacao): add RegraClassificacaoService.substituir`

---

### T12: Criar `RegraClassificacaoController` com `GET /api/v1/regras-classificacao?serie=`

**What**: `GET /api/v1/regras-classificacao?serie={1-5}` (sem `@PreAuthorize`, aberto a qualquer perfil autenticado, `@RequestParam @Min(1) @Max(5) int serie`), retorna `List<RegraClassificacaoResponse>` das faixas ativas da série (200).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoController.java`
**Depends on**: T2, T8, T10
**Reuses**: padrão de controller REST de `bancopalavras.ListaPalavrasController` (GETs sem `@PreAuthorize`); `RegraClassificacaoService.buscarAtivas` (T10); `handleHandlerMethodValidationException`/`VALIDACAO_INVALIDA` (T2)
**Requirement**: REG-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `PROFESSOR` e `COORDENADOR` autenticados recebem 200 com as faixas ativas da série pedida, ordenadas por `quantidadeMinimaAcertos`
- [ ] Requisição sem `Authorization` recebe 401
- [ ] `serie` fora de 1-5 recebe 422 `VALIDACAO_INVALIDA`
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 4 tests pass em `RegraClassificacaoControllerIT` (endpoint `GET /regras-classificacao`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(regras-classificacao): add GET /regras-classificacao endpoint`

---

### T13: Criar `RegraClassificacaoController.substituir` (PUT)

**What**: `PUT /api/v1/regras-classificacao/series/{serie}` com `@PreAuthorize("hasRole('COORDENADOR')")`, `@PathVariable @Min(1) @Max(5) int serie`, `@Valid @RequestBody SubstituirRegrasClassificacaoRequest`, retorna `List<RegraClassificacaoResponse>` das novas faixas (200).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoController.java` (modify)
**Depends on**: T11, T12
**Reuses**: padrão `@PreAuthorize`/`@Valid` já aplicado em T12; `RegraClassificacaoService.substituir` (T11)
**Requirement**: REG-07..REG-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `COORDENADOR` autenticado substitui as faixas de uma série válida e recebe 200 com o novo conteúdo
- [ ] `PROFESSOR` autenticado recebe 403
- [ ] Requisição sem `Authorization` recebe 401
- [ ] `serie` fora de 1-5 no path recebe 422 `VALIDACAO_INVALIDA`
- [ ] Cada erro 422 de negócio do spec (`FAIXA_NAO_INICIA_EM_ZERO`, `FAIXA_COM_LACUNA` com `details.valor`, `FAIXA_SOBREPOSTA` com `details.valor`, `FAIXA_FINAL_LIMITADA`, `FAIXA_NIVEL_INCOERENTE`) é testado ponta a ponta
- [ ] Depois de um `PUT` bem-sucedido, `GET /regras-classificacao?serie=` reflete as novas faixas (efeito observável, sem reiniciar a aplicação)
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 9 tests pass em `RegraClassificacaoControllerIT` (endpoint `PUT /regras-classificacao/series/{serie}`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(regras-classificacao): add PUT /regras-classificacao/series/{serie} endpoint`

---

### T14: Criar `RegraClassificacaoController.historico` (GET)

**What**: `GET /api/v1/regras-classificacao/historico?serie={1-5}` (sem `@PreAuthorize`, `@RequestParam @Min(1) @Max(5) int serie`), retorna `List<HistoricoVersaoResponse>` (200).
**Where**: `src/main/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoController.java` (modify)
**Depends on**: T10, T13
**Reuses**: padrão já aplicado em T12/T13; `RegraClassificacaoService.buscarHistorico` (T10)
**Requirement**: REG-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `PROFESSOR` e `COORDENADOR` autenticados recebem 200
- [ ] Substituir as faixas de uma série duas vezes (dois `PUT`) e consultar o histórico mostra 3 grupos (o seed original + as duas substituições), o grupo corrente primeiro
- [ ] `serie` fora de 1-5 recebe 422 `VALIDACAO_INVALIDA`
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 3 tests pass em `RegraClassificacaoControllerIT` (endpoint `GET /regras-classificacao/historico`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(regras-classificacao): add GET /regras-classificacao/historico endpoint`

---

## Phase Execution Map

Visual representation of task ordering. Phases run in sequence, and tasks within a phase run in order:

```
Phase 1 (T1, T2, T3, T4) precede Phase 2 (T5) precede Phase 3 (T6)
precede Phase 4 (T7, T8) precede Phase 5 (T9, T10, T11) precede Phase 6 (T12, T13, T14)

Phase 1:  T1   T2   T3   T4   (independentes entre si)
Phase 2:  T3 --\
                +--> T5
          T4 --/
Phase 3:  T5 --------------> T6
Phase 4:  T4 --------------> T7
          T4, T5 -----------> T8
Phase 5:  T6 --------------> T9
          T6 --------------> T10
          T1, T6, T7 -------> T11
Phase 6:  T2, T8, T10 ------> T12
          T11, T12 ---------> T13
          T10, T13 ---------> T14
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

**Batching**: 14 tasks total → 2 task-budgeted batches at the Phase 4/5 boundary (offer sub-agents per the skill's Sub-Agent Delegation section):

- **Batch 1** (Phases 1-4, 8 tasks): T1, T2, T3, T4, T5, T6, T7, T8
- **Batch 2** (Phases 5-6, 6 tasks): T9, T10, T11, T12, T13, T14

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Estender `ContextoUsuarioPort` | 1 método em 1 interface + 1 implementação | ✅ Granular |
| T2: Estender `GlobalExceptionHandler` | 1 handler novo | ✅ Granular |
| T3: Migração `V7` | 1 arquivo SQL | ✅ Granular |
| T4: Enum `Fase` | 1 enum | ✅ Granular |
| T5: Entidade `RegraClassificacao` | 1 entidade | ✅ Granular |
| T6: `RegraClassificacaoRepository` | 1 interface, 3 queries coesas | ✅ Granular |
| T7: DTOs de request | 2 records coesos (request de um único endpoint) | ✅ Granular |
| T8: DTOs de response | 2 records coesos (response de leitura) | ✅ Granular |
| T9: `classificar` | 1 método | ✅ Granular |
| T10: `buscarAtivas`/`buscarHistorico` | 2 métodos de leitura triviais, mesmo arquivo | ✅ Granular |
| T11: `substituir` | 1 método (validação cruzada + persistência atômica) | ✅ Granular |
| T12: `GET /regras-classificacao` | 1 endpoint | ✅ Granular |
| T13: `PUT /regras-classificacao/series/{serie}` | 1 endpoint | ✅ Granular |
| T14: `GET /regras-classificacao/historico` | 1 endpoint | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | (nenhuma seta necessária) | ✅ Match |
| T2 | None | (nenhuma seta necessária) | ✅ Match |
| T3 | None | (nenhuma seta necessária) | ✅ Match |
| T4 | None | (nenhuma seta necessária) | ✅ Match |
| T5 | T3, T4 | T3 -> T5, T4 -> T5 | ✅ Match |
| T6 | T5 | T5 -> T6 | ✅ Match |
| T7 | T4 | T4 -> T7 | ✅ Match |
| T8 | T4, T5 | T4 -> T8, T5 -> T8 | ✅ Match |
| T9 | T6 | T6 -> T9 | ✅ Match |
| T10 | T6 | T6 -> T10 | ✅ Match |
| T11 | T1, T6, T7 | T1 -> T11, T6 -> T11, T7 -> T11 | ✅ Match |
| T12 | T2, T8, T10 | T2 -> T12, T8 -> T12, T10 -> T12 | ✅ Match |
| T13 | T11, T12 | T11 -> T13, T12 -> T13 | ✅ Match |
| T14 | T10, T13 | T10 -> T14, T13 -> T14 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: `ContextoUsuarioPort` | Infra de segurança | unit | unit | ✅ OK |
| T2: `GlobalExceptionHandler` | Infra de erro | unit | unit | ✅ OK |
| T3: Migração `V7` | Entidade/enum/migração | none | none | ✅ OK |
| T4: `Fase` | Entidade/enum/migração | none | none | ✅ OK |
| T5: `RegraClassificacao` | Entidade/enum/migração | none | none | ✅ OK |
| T6: `RegraClassificacaoRepository` | Repositório (lock pessimista) | integration | integration | ✅ OK |
| T7: DTOs de request | Entidade/enum/migração (DTO) | none | none | ✅ OK |
| T8: DTOs de response | Entidade/enum/migração (DTO) | none | none | ✅ OK |
| T9: `classificar` | Serviço de domínio | unit | unit | ✅ OK |
| T10: `buscarAtivas`/`buscarHistorico` | Serviço de domínio | unit | unit | ✅ OK |
| T11: `substituir` | Serviço de domínio | unit | unit | ✅ OK |
| T12: `GET /regras-classificacao` | Controller | integration | integration | ✅ OK |
| T13: `PUT /regras-classificacao/series/{serie}` | Controller | integration | integration | ✅ OK |
| T14: `GET /regras-classificacao/historico` | Controller | integration | integration | ✅ OK |

---

## Tips

- Reaproveitar ao máximo a estrutura de `bancopalavras` (migração, entidade, repositório, service com validadores privados, controller, DTOs record com Bean Validation).
- O ponto novo de verdade é o lock pessimista (T6) - precisa de um teste de concorrência real (duas transações), não só uma chamada dupla no mesmo thread.
- `classificar(int serie, int acertos)` (T9) é a interface pública que `avaliacao` vai chamar depois - manter a assinatura estável.
- T2 exige confirmar a API de validação de parâmetro de método do Spring Framework 7 antes de implementar - não presumir `ConstraintViolationException`/`@Validated` de versões antigas.

# Banco de Palavras Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/banco-palavras/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** (JaCoCo ≥85% linhas; Testcontainers MySQL nos testes de integração). Amostra de testes existentes (`cadastros-base`, `autenticacao-perfis`, já implementadas): `*ServiceTest.java` unit com Mockito, `*RepositoryIT.java`/`*ControllerIT.java` integration via `IntegrationTestBase` (Testcontainers MySQL, singleton container, `bearerCoordenador()` já disponível; `bearerProfessor` é emitido inline em cada `*ControllerIT` via `JwtService`, mesmo padrão de `MatriculaControllerIT`). Esta feature segue o mesmo padrão.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Utilitário puro (`TokenizadorTexto`) | unit | Todos os branches do AC PAL-08 + edge cases listados (pontuação nas bordas, espaços múltiplos, token vazio descartado, grafia original preservada) | `src/test/java/com/missio/fluencia_leitora/common/texto/TokenizadorTextoTest.java` | `./mvnw test` |
| Infra de erro (`BusinessException`, `GlobalExceptionHandler`) | unit | Novo construtor com `details` copia cada entrada para o `ProblemDetail`, sem regressão nos testes existentes | `src/test/java/com/missio/fluencia_leitora/common/error/GlobalExceptionHandlerTest.java` (estende o arquivo existente) | `./mvnw test` |
| Serviço de domínio (`ListaPalavrasService`) | unit | Todos os branches; 1:1 com os ACs do spec (PAL-01..PAL-12); todo edge case listado (Edge Cases + Implicit-requirement sweep) tem um teste | `src/test/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasServiceTest.java` | `./mvnw test` |
| Repositório com query customizada (`ListaPalavrasRepository`) | integration | Projeção `buscarResumo` (filtra por `serie`+`tipoLeitura`+`ativo`, conta itens corretamente, ignora inativas) | `src/test/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasRepositoryIT.java` | `./mvnw verify` |
| Controller (REST) (`ListaPalavrasController`) | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (401/403/404/409/422) do spec | `src/test/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasControllerIT.java` | `./mvnw verify` |
| Entidade (`ListaPalavras`, `ItemListaPalavras`) / enums / migração Flyway (`V6`) | none | - (build gate only; exercitadas indiretamente pelas tasks de repositório/serviço/controller) | - | `./mvnw compile` |

## Gate Check Commands

> Reaproveita a configuração já existente do projeto (`maven-failsafe-plugin` para `*IT.java`, `jacoco-maven-plugin` com 85% de linhas na fase `verify` - ambos configurados em `cadastros-base`/T1, nada novo a configurar aqui).

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que só adicionam `*ServiceTest`/`*Test` (unit, sem Docker) | `./mvnw test` |
| Full | Após tasks que adicionam/alteram `*RepositoryIT`/`*ControllerIT` (integration, precisa Docker) | `./mvnw verify` |
| Build | Fechamento de fase, ou tasks só de migração/entidade/enum sem teste novo | `./mvnw compile` (e `./mvnw verify` no fechamento de cada fase) |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Infraestrutura compartilhada (AD-008 + tokenização + migração)

```
T1
T2
T3
```

### Phase 2: Modelo de domínio (enums + entidades JPA)

```
T3 -> T5
T4 -> T5
T3 -> T6
T4 -> T6
T5 -> T6
```

### Phase 3: Persistência

```
T6 -> T7
```

### Phase 4: DTOs

```
T4 -> T8
T6 -> T9
T7 -> T9
```

### Phase 5: Serviço de domínio

```
T1 -> T10
T2 -> T10
T7 -> T10
T8 -> T10
T9 -> T10
T10 -> T11
T7 -> T12
```

### Phase 6: Controller (REST + segurança)

```
T10 -> T13
T11 -> T14
T13 -> T14
T12 -> T15
T14 -> T15
```

---

## Task Breakdown

### T1: Estender `BusinessException`/`GlobalExceptionHandler` com `details` estruturados (AD-008)

**What**: Adicionar o construtor `BusinessException(HttpStatus, String code, String message, Map<String, Object> details)` (construtor antigo continua existindo) e fazer `GlobalExceptionHandler.handleBusinessException` copiar cada entrada de `details` para o `ProblemDetail` além do `code`.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/error/BusinessException.java`, `src/main/java/com/missio/fluencia_leitora/common/error/GlobalExceptionHandler.java` (modify)
**Depends on**: None
**Reuses**: `common.error.BusinessException`/`GlobalExceptionHandler` existentes
**Requirement**: N/A (infra - AD-008)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `BusinessException` tem os dois construtores (com e sem `details`); `getDetails()` retorna mapa vazio quando não informado, nunca `null`
- [x] `GlobalExceptionHandler.handleBusinessException` chama `problemDetail.setProperty(key, value)` para cada entrada de `details`
- [x] Testes existentes em `GlobalExceptionHandlerTest` continuam passando sem alteração
- [x] Novo teste: exceção com `details={"posicoes": List.of(2)}` produz `$.posicoes[0] == 2` no `ProblemDetail`
- [x] Gate check passes: `./mvnw test`
- [x] Test count: 4 tests pass em `GlobalExceptionHandlerTest` (3 existentes + 1 novo)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(banco-palavras)!: add structured details to BusinessException`

---

### T2: Criar `TokenizadorTexto` (função pura de tokenização)

**What**: Classe utilitária `TokenizadorTexto` com `static List<String> tokenizar(String texto)`: separa por espaços em branco, remove pontuação do início/fim de cada token, descarta tokens vazios, preserva grafia original (maiúsculas/acentos).
**Where**: `src/main/java/com/missio/fluencia_leitora/common/texto/TokenizadorTexto.java`
**Depends on**: None
**Reuses**: nenhum (nova, pensada para reuso futuro por `avaliacao`)
**Requirement**: PAL-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `tokenizar("O gato, a bola.")` retorna exatamente `["O", "gato", "a", "bola"]` (AC do spec, ordem 1-4)
- [x] Espaços múltiplos entre palavras não geram tokens vazios
- [x] Pontuação só nas bordas do token é removida; hífen interno (`bem-vindo`) não é afetado
- [x] Grafia original (maiúsculas, acentos) é preservada em cada token
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 5 tests pass em `TokenizadorTextoTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(banco-palavras): add pure text tokenizer`

---

### T3: Migração Flyway `V6__banco_palavras.sql`

**What**: Criar as tabelas `lista_palavras` e `item_lista_palavras` conforme o DDL do `design.md` (colunas, `CHECK` de série, índice composto `serie+tipo_leitura+ativo`, FK + unique `lista_palavras_id+ordem`).
**Where**: `src/main/resources/db/migration/V6__banco_palavras.sql`
**Depends on**: None
**Reuses**: padrão de migração já usado em `V1`..`V5`
**Requirement**: N/A (infra de dados para PAL-01..PAL-12)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `lista_palavras` e `item_lista_palavras` criadas exatamente como no `design.md` (incluindo `ck_lista_palavras_serie`, `idx_lista_palavras_filtro`, `fk_item_lista_palavras_lista`, `uk_item_lista_palavras_ordem`)
- [x] Aplicação sobe sem erro de migração (`./mvnw compile` + contexto Spring Boot inicia num teste de integração já existente, ex. `FluenciaLeitoraApplicationIT`)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add lista_palavras and item_lista_palavras migration`

---

### T4: Criar enums `TipoLeituraCodigo` e `TipoPalavra`

**What**: Dois enums Java simples: `TipoLeituraCodigo { PALAVRA, PSEUDOPALAVRA, TEXTO_CURTO }` e `TipoPalavra { CANONICA, NAO_CANONICA }`.
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/TipoLeituraCodigo.java`, `src/main/java/com/missio/fluencia_leitora/bancopalavras/TipoPalavra.java`
**Depends on**: None
**Reuses**: nenhum (ver design.md, Tech Decisions - rejeitado FK para `cadastros.dominio.TipoLeitura`)
**Requirement**: N/A (suporte a PAL-01, PAL-02, PAL-07, PAL-09, PAL-12)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Os dois enums compilam com exatamente os valores do `design.md`
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add TipoLeituraCodigo and TipoPalavra enums`

---

### T5: Criar entidade `ItemListaPalavras`

**What**: Entidade JPA mapeando `item_lista_palavras` (`palavra`, `tipoPalavra` `@Enumerated(STRING)`, `ordem`, FK `lista` para `ListaPalavras`).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ItemListaPalavras.java`
**Depends on**: T3, T4
**Reuses**: padrão de entidade JPA de `cadastros.turma.Turma`/`cadastros.aluno.Aluno` (getters, sem setters públicos além dos necessários, construtor protegido sem args para o JPA)
**Requirement**: N/A (suporte a PAL-01, PAL-07)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Entidade mapeia todas as colunas de `item_lista_palavras` (T3)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add ItemListaPalavras entity`

---

### T6: Criar entidade `ListaPalavras`

**What**: Entidade JPA mapeando `lista_palavras` (`nome`, `serie`, `tipoLeitura`, `tipoPalavra` nullable, `texto` nullable, `ativo`, `@Version version`, `criadoEm`/`atualizadoEm`) com `List<ItemListaPalavras> itens` (`@OneToMany(cascade=ALL, orphanRemoval=true)`, `@OrderBy("ordem")`).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavras.java`
**Depends on**: T3, T4, T5
**Reuses**: padrão `@Version` de `cadastros.turma.Turma`/`cadastros.aluno.Matricula`
**Requirement**: N/A (suporte a PAL-01..PAL-12)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Entidade mapeia todas as colunas de `lista_palavras` (T3) e a coleção `itens` com cascade/orphanRemoval/ordenação corretos
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add ListaPalavras entity`

---

### T7: Criar `ListaPalavrasRepository` com projeção de resumo

**What**: `interface ListaPalavrasRepository extends JpaRepository<ListaPalavras, Long>` + `interface ListaPalavrasResumoProjection { Long getId(); String getNome(); Long getQuantidadePalavras(); }` + método `@Query(...) List<ListaPalavrasResumoProjection> buscarResumo(int serie, TipoLeituraCodigo tipo)` (`COUNT`+`GROUP BY`, só `ativo=true`, conforme `design.md`).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasRepository.java`, `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasResumoProjection.java`
**Depends on**: T6
**Reuses**: padrão de `@Query` + projeção Spring Data (novo neste projeto, mas idiomático - sem precedente direto para copiar)
**Requirement**: PAL-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `buscarResumo(2, PSEUDOPALAVRA)` retorna só listas ativas da série 2 e tipo PSEUDOPALAVRA, com `quantidadePalavras` igual ao número de itens
- [x] Lista inativa não aparece em `buscarResumo`, mesmo casando série/tipo
- [x] Lista de série/tipo diferente não aparece
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 3 tests pass em `ListaPalavrasRepositoryIT`

**Tests**: integration
**Gate**: full

**Commit**: `feat(banco-palavras): add ListaPalavrasRepository with resumo projection`

---

### T8: Criar DTOs de request

**What**: Records `ItemPalavraRequest(String palavra, TipoPalavra tipoPalavra)`, `CriarListaPalavrasRequest(String nome, Integer serie, TipoLeituraCodigo tipoLeitura, TipoPalavra tipoPalavra, String texto, List<ItemPalavraRequest> itens)` e `AtualizarListaPalavrasRequest` (mesmos campos + `Long version`), com bean validation (`@NotBlank`, `@Size(min=3,max=100)` em `nome`; `@NotNull @Min(1) @Max(5)` em `serie`; `@NotNull` em `tipoLeitura`; `@Size(max=60) @Pattern(regexp="^[\\p{L}-]+$")` em `palavra`; `@Size(min=1,max=2000)` em `texto`; `@Valid @Size(min=1,max=200)` em `itens`).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/dto/ItemPalavraRequest.java`, `.../dto/CriarListaPalavrasRequest.java`, `.../dto/AtualizarListaPalavrasRequest.java`
**Depends on**: T4
**Reuses**: padrão de DTO record de `cadastros.professor.dto.CriarProfessorRequest`
**Requirement**: PAL-03, PAL-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] As três classes compilam com as anotações de validação acima
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add request DTOs with bean validation`

---

### T9: Criar DTOs de response

**What**: Records `ItemPalavraResponse(String palavra, TipoPalavra tipoPalavra, int ordem)` (com `from(ItemListaPalavras)`), `ListaPalavrasResponse` (detalhe completo: `id, nome, serie, tipoLeitura, tipoPalavra, texto, ativo, quantidadePalavras, List<ItemPalavraResponse> itens`, com `from(ListaPalavras)`) e `ListaPalavrasResumoResponse(Long id, String nome, Long quantidadePalavras)` (com `from(ListaPalavrasResumoProjection)`).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/dto/ItemPalavraResponse.java`, `.../dto/ListaPalavrasResponse.java`, `.../dto/ListaPalavrasResumoResponse.java`
**Depends on**: T6, T7
**Reuses**: padrão `from(...)` estático de `cadastros.professor.dto.ProfessorResponse`
**Requirement**: PAL-10, PAL-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `ListaPalavrasResponse.from(...)` inclui `texto` só quando `tipoLeitura == TEXTO_CURTO` (demais casos `null`)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(banco-palavras): add response DTOs`

---

### T10: Implementar `ListaPalavrasService.criar(...)`

**What**: Método `criar(CriarListaPalavrasRequest): ListaPalavras` (`@Transactional`) com os validadores privados `validarConteudoCompativel` (PAL-12, `CONTEUDO_INCOMPATIVEL_COM_TIPO`), `validarSerieCanonica` (PAL-02/PAL-09, `NAO_CANONICA_PROIBIDA_1_ANO` com `details={"posicoes": [...]}`, usando o construtor de T1), `validarDuplicidade` (PAL-04, `PALAVRA_DUPLICADA`, case-insensitive após trim) e a chamada a `TokenizadorTexto.tokenizar` para `TEXTO_CURTO` (PAL-07/PAL-08, valida limite de 200 tokens).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasService.java`
**Depends on**: T1, T2, T7, T8, T9
**Reuses**: `common.error.BusinessException` (com `details`, T1), `common.texto.TokenizadorTexto` (T2)
**Requirement**: PAL-01, PAL-02, PAL-03, PAL-04, PAL-05, PAL-07, PAL-08, PAL-09, PAL-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Lista `PALAVRA`/`PSEUDOPALAVRA` válida é criada com os itens na ordem enviada (1..n)
- [x] `serie=1` com item `NAO_CANONICA` lança `BusinessException` 422 `NAO_CANONICA_PROIBIDA_1_ANO` com as posições corretas em `details`
- [x] Palavra duplicada (case-insensitive, trim) lança 422 `PALAVRA_DUPLICADA`
- [x] `itens` com `tipoLeitura=TEXTO_CURTO` (ou `texto`/`tipoPalavra` de lista com `tipoLeitura` != `TEXTO_CURTO`) lança 422 `CONTEUDO_INCOMPATIVEL_COM_TIPO`
- [x] `"O gato, a bola."` tokenizado gera itens `[O, gato, a, bola]` com `ordem` 1-4 e o `tipoPalavra` da lista copiado para cada item
- [x] Texto que gera mais de 200 tokens lança 422
- [x] `serie=1` com `TEXTO_CURTO`/`tipoPalavra=NAO_CANONICA` lança 422 `NAO_CANONICA_PROIBIDA_1_ANO`
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 10 tests pass em `ListaPalavrasServiceTest` (método `criar`)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(banco-palavras): add ListaPalavrasService.criar with cross-field validation`

---

### T11: Implementar `ListaPalavrasService.atualizar(...)`

**What**: Método `atualizar(Long id, AtualizarListaPalavrasRequest): ListaPalavras` (`@Transactional`), reaproveitando os validadores privados de T10; substitui a coleção `itens` (o `orphanRemoval` cuida da exclusão dos antigos) e depende do `version` do request para o lock otimista.
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasService.java` (modify)
**Depends on**: T10
**Reuses**: validadores privados criados em T10
**Requirement**: PAL-06, PAL-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Editar uma lista troca nome/itens e passa pelas mesmas validações de T10 (reexecutadas com o novo conteúdo)
- [x] `version` divergente lança `ObjectOptimisticLockingFailureException` (409 `CONFLITO_DE_VERSAO` via handler existente)
- [x] Itens antigos não referenciados pela nova lista são removidos (`orphanRemoval`), itens novos recebem `ordem` corretas
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 4 tests pass adicionados em `ListaPalavrasServiceTest` (método `atualizar`)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(banco-palavras): add ListaPalavrasService.atualizar`

---

### T12: Implementar `ListaPalavrasService.buscar/buscarPorId/inativar`

**What**: `buscar(int serie, TipoLeituraCodigo tipo): List<ListaPalavrasResumoProjection>` (delega a `ListaPalavrasRepository.buscarResumo`), `buscarPorId(Long id): ListaPalavras` (lança `BusinessException(404, "LISTA_NAO_ENCONTRADA", ...)` se não existir; funciona com lista inativa) e `inativar(Long id): void` (`ativo=false`, soft-delete).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasService.java` (modify)
**Depends on**: T7
**Reuses**: `ListaPalavrasRepository.buscarResumo` (T7)
**Requirement**: PAL-10, PAL-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `buscar` delega ao repositório e retorna a projeção sem transformação extra
- [ ] `buscarPorId` de um id inexistente lança 404 `LISTA_NAO_ENCONTRADA`; de uma lista inativa retorna a lista normalmente
- [ ] `inativar` seta `ativo=false` e não remove a linha (`repository.delete` nunca chamado)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 4 tests pass adicionados em `ListaPalavrasServiceTest`

**Tests**: unit
**Gate**: quick

**Commit**: `feat(banco-palavras): add ListaPalavrasService buscar/buscarPorId/inativar`

---

### T13: Criar `ListaPalavrasController.criar` (POST)

**What**: `POST /api/v1/listas-palavras` com `@PreAuthorize("hasRole('COORDENADOR')")`, `@Valid @RequestBody CriarListaPalavrasRequest`, `@ResponseStatus(CREATED)`, retorna `ListaPalavrasResponse.from(...)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasController.java`
**Depends on**: T10
**Reuses**: padrão `@PreAuthorize`/`@Valid` de `AlunoController.criar`
**Requirement**: PAL-01, PAL-02, PAL-03, PAL-04, PAL-05, PAL-07, PAL-08, PAL-09, PAL-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `COORDENADOR` autenticado recebe 201 com o corpo esperado ao criar uma lista `PALAVRA` válida
- [ ] `PROFESSOR` autenticado recebe 403
- [ ] Requisição sem `Authorization` recebe 401
- [ ] Cada erro 422 do spec (`NAO_CANONICA_PROIBIDA_1_ANO` com posições, `PALAVRA_DUPLICADA`, `CONTEUDO_INCOMPATIVEL_COM_TIPO`, formato de palavra inválido, tamanho de `itens` fora de 1-200, texto com mais de 200 tokens) é testado ponta a ponta
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 8 tests pass em `ListaPalavrasControllerIT` (endpoint `criar`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(banco-palavras): add POST /listas-palavras endpoint`

---

### T14: Criar `ListaPalavrasController.atualizar` (PUT)

**What**: `PUT /api/v1/listas-palavras/{id}` com `@PreAuthorize("hasRole('COORDENADOR')")`, `@Valid @RequestBody AtualizarListaPalavrasRequest`, retorna `ListaPalavrasResponse.from(...)` (200).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasController.java` (modify)
**Depends on**: T11, T13
**Reuses**: padrão `@PreAuthorize` já aplicado em T13
**Requirement**: PAL-06, PAL-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `COORDENADOR` edita uma lista existente e recebe 200 com o novo conteúdo
- [ ] `version` divergente recebe 409 `CONFLITO_DE_VERSAO`
- [ ] `PROFESSOR` recebe 403
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 3 tests pass adicionados em `ListaPalavrasControllerIT` (endpoint `atualizar`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(banco-palavras): add PUT /listas-palavras/{id} endpoint`

---

### T15: Criar `ListaPalavrasController.inativar/buscar/buscarPorId` (DELETE + GETs)

**What**: `DELETE /api/v1/listas-palavras/{id}` (`@PreAuthorize("hasRole('COORDENADOR')")`, soft-delete, 204), `GET /api/v1/listas-palavras?serie=&tipoLeitura=` (sem `@PreAuthorize`, retorna `List<ListaPalavrasResumoResponse>`) e `GET /api/v1/listas-palavras/{id}` (sem `@PreAuthorize`, retorna `ListaPalavrasResponse`, 404 se não existir).
**Where**: `src/main/java/com/missio/fluencia_leitora/bancopalavras/ListaPalavrasController.java` (modify)
**Depends on**: T12, T14
**Reuses**: `ListaPalavrasService.buscar/buscarPorId/inativar` (T12)
**Requirement**: PAL-10, PAL-11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `PROFESSOR` e `COORDENADOR` autenticados recebem 200 no `GET` filtrado, só com listas ativas da série/tipo pedidos
- [ ] `PROFESSOR` recebe 403 no `DELETE`; `COORDENADOR` recebe 204 e a lista some do `GET` filtrado mas continua acessível por `GET /{id}`
- [ ] `GET /{id}` de id inexistente recebe 404
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 6 tests pass adicionados em `ListaPalavrasControllerIT` (endpoints `inativar`/`buscar`/`buscarPorId`)

**Tests**: integration
**Gate**: full

**Commit**: `feat(banco-palavras): add DELETE and GET /listas-palavras endpoints`

---

## Phase Execution Map

Visual representation of task ordering. Phases run in sequence, and tasks within a phase run in order:

```
Phase 1 (T1, T2, T3) precede Phase 2 (T4, T5, T6) precede Phase 3 (T7)
precede Phase 4 (T8, T9) precede Phase 5 (T10, T11, T12) precede Phase 6 (T13, T14, T15)

Phase 1:
T1
T2
T3

Phase 2:
T3 -> T5
T4 -> T5
T3 -> T6
T4 -> T6
T5 -> T6

Phase 3:
T6 -> T7

Phase 4:
T4 -> T8
T6 -> T9
T7 -> T9

Phase 5:
T1 -> T10
T2 -> T10
T7 -> T10
T8 -> T10
T9 -> T10
T10 -> T11
T7 -> T12

Phase 6:
T10 -> T13
T11 -> T14
T13 -> T14
T12 -> T15
T14 -> T15
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: BusinessException/GlobalExceptionHandler `details` | 2 arquivos, uma extensão coesa | ✅ Granular |
| T2: TokenizadorTexto | 1 classe, 1 função | ✅ Granular |
| T3: Migração V6 | 1 arquivo SQL | ✅ Granular |
| T4: Enums | 2 arquivos triviais, mesmo conceito | ✅ Granular |
| T5: Entity ItemListaPalavras | 1 entidade | ✅ Granular |
| T6: Entity ListaPalavras | 1 entidade | ✅ Granular |
| T7: Repository + projeção | 1 repositório + 1 projeção, mesmo conceito | ✅ Granular |
| T8: DTOs de request | 3 records pequenos, mesmo conceito (payload de entrada) | ✅ Granular |
| T9: DTOs de response | 3 records pequenos, mesmo conceito (payload de saída) | ✅ Granular |
| T10: Service.criar | 1 método público + seus validadores privados | ✅ Granular |
| T11: Service.atualizar | 1 método público | ✅ Granular |
| T12: Service.buscar/buscarPorId/inativar | 3 métodos pequenos, mesmo conceito (leitura/soft-delete) | ✅ Granular |
| T13: Controller.criar | 1 endpoint | ✅ Granular |
| T14: Controller.atualizar | 1 endpoint | ✅ Granular |
| T15: Controller.inativar/buscar/buscarPorId | 3 endpoints pequenos, mesmo conceito (leitura/exclusão) | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Sem seta em nenhum diagrama | ✅ Match |
| T2 | None | Sem seta em nenhum diagrama | ✅ Match |
| T3 | None | Sem seta em nenhum diagrama | ✅ Match |
| T4 | None | Sem seta em nenhum diagrama | ✅ Match |
| T5 | T3, T4 | `T3 -> T5`, `T4 -> T5` (Fase 2) | ✅ Match |
| T6 | T3, T4, T5 | `T3 -> T6`, `T4 -> T6`, `T5 -> T6` (Fase 2) | ✅ Match |
| T7 | T6 | `T6 -> T7` (Fase 3) | ✅ Match |
| T8 | T4 | `T4 -> T8` (Fase 4) | ✅ Match |
| T9 | T6, T7 | `T6 -> T9`, `T7 -> T9` (Fase 4) | ✅ Match |
| T10 | T1, T2, T7, T8, T9 | `T1 -> T10`, `T2 -> T10`, `T7 -> T10`, `T8 -> T10`, `T9 -> T10` (Fase 5) | ✅ Match |
| T11 | T10 | `T10 -> T11` (Fase 5) | ✅ Match |
| T12 | T7 | `T7 -> T12` (Fase 5) | ✅ Match |
| T13 | T10 | `T10 -> T13` (Fase 6) | ✅ Match |
| T14 | T11, T13 | `T11 -> T14`, `T13 -> T14` (Fase 6) | ✅ Match |
| T15 | T12, T14 | `T12 -> T15`, `T14 -> T15` (Fase 6) | ✅ Match |

Nota: dentro de uma fase, tasks sem dependência funcional entre si (ex.: T1/T2/T3; T8/T9) ainda executam em ordem sequencial por serem a mesma fase - isso não exige seta própria, só a ordem em que aparecem no `Task Breakdown`.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: BusinessException/GlobalExceptionHandler | Infra de erro | unit | unit | ✅ OK |
| T2: TokenizadorTexto | Utilitário puro | unit | unit | ✅ OK |
| T3: Migração V6 | Entidade/migração | none | none | ✅ OK |
| T4: Enums | Entidade/migração (config) | none | none | ✅ OK |
| T5: Entity ItemListaPalavras | Entidade/migração | none | none | ✅ OK |
| T6: Entity ListaPalavras | Entidade/migração | none | none | ✅ OK |
| T7: Repository + projeção | Repositório com query customizada | integration | integration | ✅ OK |
| T8: DTOs de request | Entidade/migração (config, sem lógica) | none | none | ✅ OK |
| T9: DTOs de response | Entidade/migração (config, sem lógica) | none | none | ✅ OK |
| T10: Service.criar | Serviço de domínio | unit | unit | ✅ OK |
| T11: Service.atualizar | Serviço de domínio | unit | unit | ✅ OK |
| T12: Service.buscar/buscarPorId/inativar | Serviço de domínio | unit | unit | ✅ OK |
| T13: Controller.criar | Controller (REST) | integration | integration | ✅ OK |
| T14: Controller.atualizar | Controller (REST) | integration | integration | ✅ OK |
| T15: Controller.inativar/buscar/buscarPorId | Controller (REST) | integration | integration | ✅ OK |

---

## Tips

- **Phases are ordered** - Each phase completes before the next; tasks run in order within a phase
- **Reuses = Token saver** - Always reference existing code
- **One commit per task** - mensagens seguem Conventional Commits (`check_commit.py`), escopo `banco-palavras`

# Avaliação de Leitura Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/avaliacao/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** (JaCoCo ≥85% linhas; Testcontainers MySQL nos testes de integração). Amostra de testes existentes (`regras-classificacao`, `banco-palavras`, `audio-avaliacao`, já implementadas): `*ServiceTest.java` unit com Mockito, `*RepositoryIT.java`/`*ControllerIT.java` integration via `IntegrationTestBase` (Testcontainers MySQL, singleton container; `bearerCoordenador()` já disponível na base, `bearerProfessor()` é emitido inline em cada `*ControllerIT` via `JwtService`, mesmo padrão de `RegraClassificacaoControllerIT`/`MatriculaControllerIT`). Esta feature segue o mesmo padrão, com dois tipos novos: teste de escalonamento (`@Scheduled`, primeiro da base) e teste multipart (upload de áudio).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Repositório existente estendido (`MatriculaRepository`) | unit/integration | Novo método `findByAlunoIdAndAnoLetivoId` retorna a matrícula certa; sem regressão nos métodos existentes | `src/test/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaRepositoryIT.java` (estende, se existir, ou cria) | `./mvnw verify` |
| Serviço de domínio (`AvaliacaoService`) | unit | Todos os branches; 1:1 com AVA-01..AVA-32; todo edge case listado (Edge Cases + Implicit-requirement sweep) tem um teste | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoServiceTest.java` | `./mvnw test` |
| Repositório com query customizada (`AvaliacaoRepository`) | integration | `existsByAlunoIdAndStatusNot` ignora só `CANCELADA`; `findByStatusAndIniciadoEmBefore` só traz `EM_ANDAMENTO` mais antigas que o limite | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepositoryIT.java` | `./mvnw verify` |
| Adapter (`HistoricoAvaliacaoAdapter`) | unit | `existeAvaliacaoNaoCancelada` reflete o repositório; bean `@Primary` resolvido sem ambiguidade no contexto Spring | `src/test/java/com/missio/fluencia_leitora/avaliacao/HistoricoAvaliacaoAdapterTest.java` | `./mvnw test` |
| Scheduler (`AvaliacaoFinalizacaoScheduler`) | unit | Chama `AvaliacaoService.finalizarInativas()` quando disparado (sem testar o cron em si - ver T27) | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoFinalizacaoSchedulerTest.java` | `./mvnw test` |
| Controller (REST) (`AvaliacaoController`) | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (401/403/404/409/422) do spec | `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoControllerIT.java` | `./mvnw verify` |
| Entidades (`Avaliacao`, `PalavraAvaliacao`, `AvaliacaoAuditoria`, `AvaliacaoAudio`) / enums / migração Flyway (`V9`) / DTOs | none | - (build gate only; exercitadas indiretamente pelas tasks de repositório/serviço/controller) | - | `./mvnw compile` |

## Gate Check Commands

> Reaproveita a configuração já existente do projeto (`maven-failsafe-plugin` para `*IT.java`, `jacoco-maven-plugin` com 85% de linhas na fase `verify` - nada novo a configurar).

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
```

(T1 e T2 são independentes entre si - ambos rodam nesta fase, sem ordem obrigatória.)

### Phase 2: Modelo de domínio (enums + entidades)

```
T2 -> T3
T3 -> T4
T4 -> T5
T5 -> T6
T5 -> T7
```

### Phase 3: Persistência

```
T5 -> T8
T6 -> T9
T7 -> T9
```

### Phase 4: DTOs

```
T5 -> T10
T5 -> T11
```

### Phase 5: Criação

```
T1 -> T12
T8 -> T12
T10 -> T12
T12 -> T13
T11 -> T13
```

### Phase 6: Execução (transições)

```
T12 -> T14
T12 -> T15
T14 -> T16
T15 -> T16
T11 -> T16
```

### Phase 7: Marcação de palavras

```
T15 -> T17
T9 -> T17
T17 -> T18
T10 -> T18
T11 -> T18
```

### Phase 8: Consulta e auditoria

```
T16 -> T19
T17 -> T20
T9 -> T20
T19 -> T21
T20 -> T21
T11 -> T21
```

### Phase 9: Cancelamento

```
T16 -> T22
T22 -> T23
```

### Phase 10: Áudio

```
T16 -> T24
T24 -> T25
```

### Phase 11: Integração com cadastros-base

```
T8 -> T26
```

### Phase 12: Escalonamento

```
T15 -> T27
T8 -> T27
```

---

## Task Breakdown

### T1: Adicionar `MatriculaRepository.findByAlunoIdAndAnoLetivoId`

**What**: Adicionar o método derivado `Optional<Matricula> findByAlunoIdAndAnoLetivoId(Long alunoId, Long anoLetivoId)` a `MatriculaRepository`, para `AvaliacaoService.criar` buscar a matrícula ativa do aluno no ano letivo `ATIVO`.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaRepository.java` (modify)
**Depends on**: None
**Reuses**: `Matricula` (já existe), padrão de `existsByAlunoIdAndAnoLetivoId` já presente no mesmo arquivo
**Requirement**: N/A (infra para AVA-02)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Método derivado adicionado, sem alterar os métodos existentes
- [x] Novo teste de integração: aluno com matrícula no ano encontrado; aluno sem matrícula nesse ano retorna vazio
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 2 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add findByAlunoIdAndAnoLetivoId to MatriculaRepository`

---

### T2: Migração Flyway `V9__avaliacao.sql`

**What**: Criar as 4 tabelas de design.md (Data Models): `avaliacao`, `avaliacao_palavra`, `avaliacao_auditoria`, `avaliacao_audio`, com FKs (`aluno`, `professor`, `ano_letivo`, `ciclo`), `UNIQUE (avaliacao_id, ordem)` em `avaliacao_palavra` e `UNIQUE (avaliacao_id)` em `avaliacao_audio` (design.md, Risks & Concerns - backup da checagem de aplicação contra a corrida de upload duplicado).
**Where**: `src/main/resources/db/migration/V9__avaliacao.sql`
**Depends on**: None
**Reuses**: estilo de `V7__regra_classificacao.sql` (ENUM inline, `CHECK`, `criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP`)
**Requirement**: N/A (infra para todas as AVA-*)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] As 4 tabelas existem com exatamente as colunas de design.md, Data Models (sem `nome_arquivo`/`duracao_segundos` em `avaliacao_audio` - Tech Decisions)
- [x] `UNIQUE (avaliacao_id, ordem)` em `avaliacao_palavra`; `UNIQUE (avaliacao_id)` em `avaliacao_audio`
- [x] FKs para `aluno`, `professor`, `ano_letivo`, `ciclo`, e das tabelas filhas para `avaliacao`
- [x] `./mvnw compile` sobe o contexto sem erro de migração (Flyway valida no boot)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add V9 migration for avaliacao tables`

---

### T3: Criar enums `StatusAvaliacao`, `StatusPalavra` e `AcaoAuditoria`

**What**: Três enums nativos (`EnumType.STRING`): `StatusAvaliacao` (CRIADA, EM_ANDAMENTO, PAUSADA, FINALIZADA, CANCELADA), `StatusPalavra` (PENDENTE, CORRETA, INCORRETA, NAO_LIDA), `AcaoAuditoria` (MARCACAO_PALAVRA, CANCELAMENTO).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/StatusAvaliacao.java`, `StatusPalavra.java`, `AcaoAuditoria.java`
**Depends on**: T2
**Reuses**: mesmo padrão de `regrasclassificacao/Fase.java`, `bancopalavras/TipoPalavra.java`
**Requirement**: N/A (infra para todas as AVA-*)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Os 3 enums existem com exatamente os valores acima
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add StatusAvaliacao, StatusPalavra and AcaoAuditoria enums`

---

### T4: Criar entidade `PalavraAvaliacao`

**What**: Entidade `PalavraAvaliacao` (`ordem`, `palavra`, `tipoPalavra` nullable, `status` default `PENDENTE`), sem repositório próprio - só existe dentro do agregado `Avaliacao` (design.md, Components).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/PalavraAvaliacao.java`
**Depends on**: T3
**Reuses**: `TipoPalavra` (de `bancopalavras`); padrão de `ItemListaPalavras` (entidade filha sem repositório, `@ManyToOne` para o agregado)
**Requirement**: N/A (infra para AVA-01, AVA-15)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Entidade mapeia `avaliacao_palavra` com todas as colunas de design.md
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add PalavraAvaliacao entity`

---

### T5: Criar entidade `Avaliacao` (agregado raiz)

**What**: Entidade `Avaliacao` com todos os campos de design.md (cópias, tempo, status, resultado, classificação, `version` otimista), `@OneToMany` para `PalavraAvaliacao` (cascade ALL, orphanRemoval, `@OrderBy("ordem")`), e um método `tocarAtividade()` que atualiza `ultimaAtividadeEm` (Tech Decisions).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/Avaliacao.java`
**Depends on**: T4
**Reuses**: `Aluno`, `AnoLetivo`, `Ciclo` (FKs); `TipoLeituraCodigo` (de `bancopalavras`); `Fase` (de `regrasclassificacao`); padrão de `ListaPalavras` (agregado com coleção cascade ALL)
**Requirement**: N/A (infra para AVA-01)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Entidade mapeia `avaliacao` com todas as colunas de design.md, incluindo `ultimaAtividadeEm`
- [x] `tocarAtividade()` atualiza `ultimaAtividadeEm` para `Instant.now()`
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add Avaliacao aggregate root entity`

---

### T6: Criar entidade `AvaliacaoAuditoria`

**What**: Entidade `AvaliacaoAuditoria` (`usuarioId`, `dataHora`, `acao`, `valorAnterior`, `valorNovo`, `justificativa` nullable), `@ManyToOne` para `Avaliacao`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAuditoria.java`
**Depends on**: T5
**Reuses**: `AcaoAuditoria` (T3)
**Requirement**: N/A (infra para AVA-19, AVA-24, AVA-26)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Entidade mapeia `avaliacao_auditoria` com todas as colunas de design.md
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add AvaliacaoAuditoria entity`

---

### T7: Criar entidade `AvaliacaoAudio`

**What**: Entidade `AvaliacaoAudio` (`referenciaArmazenamento`, `mimeType`, `tamanhoBytes`, `criadoEm`), `@OneToOne` para `Avaliacao` com `avaliacao_id` único.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAudio.java`
**Depends on**: T5
**Reuses**: nenhum componente existente além da FK
**Requirement**: N/A (infra para AVA-27..AVA-32)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Entidade mapeia `avaliacao_audio` com todas as colunas de design.md (sem `nomeArquivo`/`duracaoSegundos`)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add AvaliacaoAudio entity`

---

### T8: Criar `AvaliacaoRepository`

**What**: `AvaliacaoRepository` com `boolean existsByAlunoIdAndStatusNot(Long alunoId, StatusAvaliacao status)` (consumido por T26) e `List<Avaliacao> findByStatusAndIniciadoEmBefore(StatusAvaliacao status, Instant limite)` (consumido por T27 - filtra por `ultimaAtividadeEm`, não `iniciadoEm`; ver nota abaixo).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepository.java`
**Depends on**: T5
**Reuses**: padrão de `MatriculaRepository`/`AnoLetivoRepository` (métodos derivados simples)
**Requirement**: N/A (infra para AVA-17, AVA-26 via `HistoricoAvaliacaoPort`)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Método usado pelo scheduler é `findByStatusAndUltimaAtividadeEmBefore(StatusAvaliacao, Instant)` (nome corrigido para bater com o campo real da entidade - design.md usa `ultimaAtividadeEm`, não `iniciadoEm`, para essa checagem)
- [x] Teste de integração: `existsByAlunoIdAndStatusNot` retorna `true` só quando existe avaliação com status diferente de `CANCELADA` para o aluno; `findByStatusAndUltimaAtividadeEmBefore` só traz `EM_ANDAMENTO` com `ultimaAtividadeEm` antes do limite, ignorando `PAUSADA` e avaliações recentes
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 4 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add AvaliacaoRepository`

---

### T9: Criar `AvaliacaoAuditoriaRepository` e `AvaliacaoAudioRepository`

**What**: Dois repositórios triviais: `AvaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(Long): List<AvaliacaoAuditoria>`; `AvaliacaoAudioRepository.findByAvaliacaoId(Long): Optional<AvaliacaoAudio>` e `existsByAvaliacaoId(Long): boolean`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAuditoriaRepository.java`, `AvaliacaoAudioRepository.java`
**Depends on**: T6, T7
**Reuses**: métodos derivados simples, sem query customizada - exercitados indiretamente pelas tasks de serviço/controller (T17, T20, T24, T25)
**Requirement**: N/A (infra para AVA-19, AVA-24, AVA-26, AVA-27..AVA-32)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Os dois repositórios existem com os métodos acima
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add AvaliacaoAuditoriaRepository and AvaliacaoAudioRepository`

---

### T10: Criar DTOs de request

**What**: `NovaAvaliacaoRequest` (`alunoId`, `tipoLeitura`, `cicloId`, `dataAvaliacao`, `tempoSegundos` `@Min(10) @Max(600)`, `listaPalavrasId`/`palavras[]`/`texto` - exclusividade validada no service, não aqui), `PalavraDigitadaRequest` (`palavra`, `tipoPalavra` nullable), `MarcarPalavraRequest` (`status`), `MarcarPalavrasRequest` (`itens: List<{ordem, status}>`), `CancelarAvaliacaoRequest` (`justificativa` `@Size(min=10, max=500)`).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/dto/NovaAvaliacaoRequest.java`, `PalavraDigitadaRequest.java`, `MarcarPalavraRequest.java`, `MarcarPalavrasRequest.java`, `CancelarAvaliacaoRequest.java`
**Depends on**: T5
**Reuses**: padrão de `bancopalavras/dto` (records com bean validation; `@NotNull` em elemento de coleção - lição L-023 aplicada aqui em `palavras`/`itens`)
**Requirement**: N/A (infra para AVA-01..AVA-08, AVA-15, AVA-24)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Os 5 DTOs existem como records com as anotações de bean validation acima
- [x] Elementos de `palavras`/`itens` anotados `@NotNull` (não só `@Valid` no container - lição L-023)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add request DTOs`

---

### T11: Criar DTOs de response

**What**: `AvaliacaoResponse` (config + status + cópias + palavras + resultado completo quando `FINALIZADA`, incluindo `classificacaoPendente` derivado), `PalavraAvaliacaoResponse`, `AvaliacaoAuditoriaResponse` (`usuario`, `dataHora`, `acao`, `valorAnterior`, `valorNovo`, `justificativa`).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/dto/AvaliacaoResponse.java`, `PalavraAvaliacaoResponse.java`, `AvaliacaoAuditoriaResponse.java`
**Depends on**: T5
**Reuses**: padrão `XxxResponse.from(entidade)` já usado em `RegraClassificacaoResponse`/`MatriculaResponse`
**Requirement**: N/A (infra para AVA-23, AVA-26)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `AvaliacaoResponse.from(Avaliacao)` inclui `classificacaoPendente = (status == FINALIZADA && fase == null)`
- [x] Campos de resultado (`quantidadeCorretas` etc., `fase`, `nivel`) presentes só quando `FINALIZADA` (`null` fora disso) - teste de leitura de ambos os ramos vem em T19/T21 (lição L-014)
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(avaliacao): add response DTOs`

---

### T12: Implementar `AvaliacaoService.criar`

**What**: Método `criar(NovaAvaliacaoRequest): Avaliacao` cobrindo AVA-01..AVA-08: busca a matrícula ativa (T1) e valida elegibilidade (AVA-02, inclui o caso de matrícula sem professor atribuído - `PertencimentoProfessorGuard` nunca casa com `professorId=null`, então o efeito observável já é 404 por ownership, sem código dedicado), resolve o conteúdo (lista/digitadas/texto, com tokenização via `TokenizadorTexto` para `texto`), valida quantidade contra `ConfiguracaoAvaliacao` (AVA-03), valida exclusividade da fonte e compatibilidade tipo/série da lista (AVA-04, AVA-05), valida tempo 10-600 (AVA-06 no service, já que a validação `@Min`/`@Max` do DTO cobre o formato mas não a mensagem/código do AC), data dentro do ano ATIVO e não futura (AVA-07... nota: renumerar conforme o spec final), restrição de não-canônica no 1º ano (AVA-08... nota: os números exatos vêm do spec.md, mapear 1:1 ao implementar), copia as palavras com `ordem` 1..n e cria a `Avaliacao` com status `CRIADA`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java`
**Depends on**: T1, T8, T10
**Reuses**: `TokenizadorTexto.tokenizar`, `ListaPalavrasRepository`, `MatriculaRepository.findByAlunoIdAndAnoLetivoId` (T1), `AlunoRepository`, `ConfiguracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie`, `AnoLetivoRepository.findBySituacao`, `BusinessException`
**Requirement**: AVA-01..AVA-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Todo AC de AVA-01..AVA-08 tem um teste unitário próprio (1:1)
- [x] Edge case: matrícula sem professor atribuído coberto (comportamento observável validado no controller, T13)
- [x] Palavra digitada sem `tipoPalavra` aceita como `null`; restrição de 1º ano só dispara quando o tipo é informado (assumption confirmada)
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 15 testes novos (8 ACs x múltiplos branches + edge cases)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement AvaliacaoService.criar`

---

### T13: Criar `AvaliacaoController.criar` (POST)

**What**: `POST /api/v1/avaliacoes`, `@PreAuthorize("hasRole('PROFESSOR')")`, `@ResponseStatus(CREATED)`, mapeia `NovaAvaliacaoRequest` → `AvaliacaoService.criar` → `AvaliacaoResponse`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java`
**Depends on**: T12, T11
**Reuses**: padrão `@PreAuthorize` de `MatriculaController`; `GlobalExceptionHandler` para todos os erros (nenhum handler novo)
**Requirement**: AVA-01..AVA-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Caminho feliz retorna 201 com o corpo completo
- [x] Cada 422 de AVA-02..AVA-08 tem um teste de integração dedicado (lição L-016 - um teste por regra, não um representante)
- [x] COORDENADOR autenticado recebe 403 (só PROFESSOR cria)
- [x] Sem autenticação recebe 401
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 10 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add POST /avaliacoes endpoint`

---

### T14: Implementar transições simples (`iniciar`/`pausar`/`continuar`/`resetar`)

**What**: Os 4 métodos de transição, cada um: chama a finalização preguiçosa primeiro (helper compartilhado com T15), valida contra a tabela de status (design.md), aplica o efeito (soma tempo em `pausar`/finalização preguiçosa, zera em `resetar`), idempotência quando a ação repetida já produz o estado atual, chama `tocarAtividade()`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T12
**Reuses**: `Avaliacao.tocarAtividade()` (T5)
**Requirement**: AVA-09, AVA-10, AVA-11, AVA-13, AVA-14 (parte)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Toda célula não-terminal da tabela de status para essas 4 ações tem um teste (transição válida e 409 nas inválidas)
- [x] Idempotência testada para as 4 ações no status que elas já produziriam
- [x] `pausar` soma corretamente o trecho em andamento; `continuar` não conta o tempo pausado (teste com `Clock`/timestamps controlados)
- [x] `resetar` zera `tempoAcumuladoSegundos`, limpa `iniciadoEm` e volta todas as palavras para `PENDENTE`
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 12 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement iniciar, pausar, continuar and resetar`

---

### T15: Implementar `finalizar` + cálculo de resultado/classificação

**What**: `finalizar` (chama a mesma finalização preguiçosa, mas idempotente nela mesma), e o helper privado `recalcularResultado(Avaliacao)` - conta `PENDENTE`→`NAO_LIDA`, calcula `corretas`/`incorretas`/`naoLidas`/`lidas`/`percentualAcerto` (HALF_UP, 2 casas), chama `RegraClassificacaoService.classificar(serie, corretas)` e grava `fase`/`nivel` (ou `null`/`null` se sem cobertura). Reusado por T17 no recálculo pós-finalização.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T12
**Reuses**: `RegraClassificacaoService.classificar(int, int)` (chamada Java direta)
**Requirement**: AVA-12, AVA-17 (parte preguiçosa), AVA-20, AVA-21, AVA-22

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `finalizar` grava `tempoUtilizadoSegundos = min(soma, configurado)`, converte `PENDENTE`→`NAO_LIDA`, calcula o resultado e a classificação
- [x] Exemplo do SDD §13 (total 20, corretas 9, incorretas 4, não lidas 7 → lidas 13, percentualAcerto 45.00) reproduzido exatamente num teste
- [x] Série 1 do exemplo acima grava fase `LEITOR_INICIANTE` com nível nulo (usa a mesma seed de `regras-classificacao`)
- [x] Classificação sem cobertura grava `fase`/`nivel` nulos, finaliza mesmo assim (AVA-22)
- [x] Todas as palavras `PENDENTE` na finalização grava corretas 0, não lidas = total, percentualAcerto 0.00 (edge case)
- [x] `WHILE EM_ANDAMENTO com tempo >= configurado, WHEN comando chega THEN finaliza antes` testado com `finalizar` chamado depois do tempo já esgotado (idempotência preguiçosa)
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 10 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement finalizar and result/classification calculation`

---

### T16: Criar endpoints de transição (`AvaliacaoController`)

**What**: `POST /{id}/iniciar`, `/pausar`, `/continuar`, `/resetar`, `/finalizar`, todos `@PreAuthorize("hasRole('PROFESSOR')")`, chamando `PertencimentoProfessorGuard` antes de delegar ao service.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java` (modify)
**Depends on**: T14, T15, T11
**Reuses**: `PertencimentoProfessorGuard.verificar`
**Requirement**: AVA-09..AVA-14, AVA-17 (parte), AVA-25

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Toda rota tem teste de caminho feliz + 409 de transição inválida + idempotência (200)
- [x] Professor de outro aluno recebe 404 em cada uma das 5 rotas (AUTH-09)
- [x] Teste de conflito de versão: duas requisições concorrentes na mesma avaliação → uma 200, a outra 409 `CONFLITO_DE_VERSAO` (AVA-25, reusa o handler já existente - ver design.md Risks)
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 18 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add status transition endpoints`

---

### T17: Implementar `marcarPalavra` e `marcarPalavras`

**What**: Marcação individual (`PUT .../palavras/{ordem}`) e em lote (tudo ou nada), bloqueada em `CRIADA`/`CANCELADA` (AVA-18), sem efeito quando o status enviado é igual ao atual (sem auditoria), e quando a avaliação está `FINALIZADA`: grava a mudança, chama `recalcularResultado` (T15), grava uma `AvaliacaoAuditoria` com `acao=MARCACAO_PALAVRA` e o texto formatado de `valorAnterior`/`valorNovo` (design.md, Tech Decisions) incluindo a classificação anterior/nova quando ela mudar.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T15, T9
**Reuses**: `recalcularResultado` (T15), `AvaliacaoAuditoriaRepository` (T9), `ContextoUsuarioPort.usuarioIdAtual()`
**Requirement**: AVA-15, AVA-18, AVA-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `CRIADA`/`CANCELADA` bloqueiam com `MARCACAO_NAO_PERMITIDA` (409); `EM_ANDAMENTO`/`PAUSADA`/`FINALIZADA` aceitam
- [x] `ordem` inexistente retorna comportamento que o controller mapeia para 404 (T18)
- [x] `FINALIZADA` + status pedido `PENDENTE` → erro (422) - AC específico do spec
- [x] Status igual ao atual não gera auditoria
- [x] Mudança numa `FINALIZADA` gera exatamente 1 registro de auditoria, recalcula `corretas` e a classificação (teste do AC "palavra 3 de NAO_LIDA para CORRETA: corretas +1, 1 registro de auditoria")
- [x] Lote: um item inválido não grava nenhum (transação única - teste que confirma rollback completo)
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 12 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement marcarPalavra and marcarPalavras`

---

### T18: Criar endpoints de marcação (`AvaliacaoController`)

**What**: `PUT /{id}/palavras/{ordem}` e `PUT /{id}/palavras` (lote), `@PreAuthorize("hasRole('PROFESSOR')")`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java` (modify)
**Depends on**: T17, T10, T11
**Reuses**: `PertencimentoProfessorGuard`
**Requirement**: AVA-15, AVA-18, AVA-19

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Caminho feliz individual e em lote (201/200 conforme o AC), 404 para `ordem` inexistente, 409 para status bloqueado, 422 para `PENDENTE` pós-finalização
- [x] Professor de outro aluno recebe 404
- [x] Gate check passes: `./mvnw verify`
- [x] Test count: >= 10 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add word marking endpoints`

---

### T19: Implementar `AvaliacaoService.buscar`

**What**: `buscar(Long id): Avaliacao` - roda a finalização preguiçosa antes de retornar (design.md, Tech Decisions), depois retorna o agregado completo.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T16
**Reuses**: o mesmo helper de finalização preguiçosa de T14/T15
**Requirement**: AVA-23

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `id` inexistente lança exceção que o controller mapeia para 404 (T21)
- [ ] Avaliação `EM_ANDAMENTO` com tempo já esgotado é finalizada silenciosamente antes de retornar (teste dedicado - evita a leitura mentir sobre o estado)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement AvaliacaoService.buscar`

---

### T20: Implementar `AvaliacaoService.consultarAuditoria`

**What**: `consultarAuditoria(Long id): List<AvaliacaoAuditoria>`, ordem cronológica.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T17, T9
**Reuses**: `AvaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc`
**Requirement**: AVA-26

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Depois de 2 alterações, retorna 2 registros em ordem cronológica (teste 1:1 com o Independent Test do spec)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 2 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement AvaliacaoService.consultarAuditoria`

---

### T21: Criar endpoints de consulta (`AvaliacaoController`)

**What**: `GET /{id}` e `GET /{id}/auditoria`, `@PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java` (modify)
**Depends on**: T19, T20, T11
**Reuses**: `PertencimentoProfessorGuard` (só bloqueia PROFESSOR de outro aluno; COORDENADOR sempre passa)
**Requirement**: AVA-23, AVA-26

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `GET /{id}` retorna config + status + cópias + palavras, e o resultado completo só quando `FINALIZADA` (teste dos dois ramos - lição L-014)
- [ ] `GET /{id}/auditoria` acessível pelo professor dono e por COORDENADOR; professor de outro aluno recebe 404 em ambas as rotas
- [ ] `id` inexistente → 404 em ambas
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 8 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add GET /avaliacoes/{id} and /auditoria endpoints`

---

### T22: Implementar `AvaliacaoService.cancelar`

**What**: `cancelar(Long id, String justificativa): Avaliacao` - permitido em qualquer status exceto `CANCELADA` (inclusive `FINALIZADA`), grava `AvaliacaoAuditoria` com `acao=CANCELAMENTO`, `valorAnterior=statusAnterior`, `justificativa`; não aciona `AudioStoragePort` (edge case - áudio permanece intacto).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T16
**Reuses**: mesmo mecanismo de auditoria de T17
**Requirement**: AVA-24

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Cancela a partir de todo status não-`CANCELADA`, inclusive `FINALIZADA` com áudio já enviado (áudio continua recuperável - teste dedicado)
- [ ] `CANCELADA` → 409 `TRANSICAO_INVALIDA` em qualquer ação subsequente (não só cancelar de novo)
- [ ] Justificativa fora de 10-500 rejeitada (o `@Size` do DTO cobre o formato; teste aqui cobre o efeito de negócio)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 6 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement AvaliacaoService.cancelar`

---

### T23: Criar endpoint de cancelamento (`AvaliacaoController`)

**What**: `POST /{id}/cancelar`, `@PreAuthorize("hasRole('PROFESSOR')")`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java` (modify)
**Depends on**: T22
**Reuses**: `PertencimentoProfessorGuard`
**Requirement**: AVA-24

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Caminho feliz (200) a partir de `FINALIZADA`; 422 para justificativa inválida; 409 a partir de `CANCELADA`
- [ ] Professor de outro aluno recebe 404
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 6 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add POST /avaliacoes/{id}/cancelar endpoint`

---

### T24: Implementar `enviarAudio` e `baixarAudio`

**What**: `enviarAudio(Long id, byte[] conteudo, String mimeType): AvaliacaoAudio` - só em `FINALIZADA`, só se não existir áudio ainda (checa `existsByAvaliacaoId`, mas a garantia real é a constraint `UNIQUE` de T2 - `DataIntegrityViolationException` nessa gravação específica é capturada e traduzida para `AUDIO_JA_ENVIADO`), chama `AudioStoragePort.armazenar`, captura `AudioFormatoInvalidoException`/`AudioTamanhoInvalidoException` e relança como `BusinessException` 422; `AudioArmazenamentoException` sobe sem tratamento (500). `baixarAudio(Long id): AudioBaixado` (bytes + mimeType) - 404 se não existir áudio.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoService.java` (modify)
**Depends on**: T16
**Reuses**: `AudioStoragePort` (injeção Spring, bean real `AudioStorageLocalAdapter`)
**Requirement**: AVA-27..AVA-32

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Envio fora de `FINALIZADA` → 409 `AUDIO_ENVIO_NAO_PERMITIDO`
- [ ] Segundo envio para a mesma avaliação → 409 `AUDIO_JA_ENVIADO` (teste via checagem prévia E via a constraint - simular a corrida com dois saves diretos no repositório)
- [ ] `AudioFormatoInvalidoException`/`AudioTamanhoInvalidoException` mapeadas para 422 com os códigos do spec
- [ ] Download sem áudio gravado → 404; download recupera exatamente os mesmos bytes enviados
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 8 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): implement enviarAudio and baixarAudio`

---

### T25: Criar endpoints de áudio (`AvaliacaoController`)

**What**: `POST /{id}/audio` (multipart, `@PreAuthorize("hasRole('PROFESSOR')")`) e `GET /{id}/audio` (`@PreAuthorize("hasAnyRole('PROFESSOR','COORDENADOR')")`, retorna `ResponseEntity<byte[]>` com `Content-Type` do `mimeType` gravado).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoController.java` (modify)
**Depends on**: T24
**Reuses**: `PertencimentoProfessorGuard`
**Requirement**: AVA-27..AVA-32

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Upload de um áudio pequeno (ex.: WAV de poucos KB) numa avaliação `FINALIZADA` → 201; download devolve os mesmos bytes com o `Content-Type` correto
- [ ] Segundo upload → 409; upload fora de `FINALIZADA` → 409; mimeType não permitido → 422; download sem áudio → 404
- [ ] Professor de outro aluno recebe 404 em ambas as rotas
- [ ] Gate check passes: `./mvnw verify`
- [ ] Test count: >= 8 testes novos

**Tests**: integration
**Gate**: full

**Commit**: `feat(avaliacao): add audio upload/download endpoints`

---

### T26: Implementar `HistoricoAvaliacaoAdapter`

**What**: `@Primary @Component class HistoricoAvaliacaoAdapter implements HistoricoAvaliacaoPort`, delegando para `AvaliacaoRepository.existsByAlunoIdAndStatusNot(alunoId, StatusAvaliacao.CANCELADA)` - substitui `HistoricoAvaliacaoPortStub` como bean resolvido por padrão, sem mudar `cadastros-base`.
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/HistoricoAvaliacaoAdapter.java`
**Depends on**: T8
**Reuses**: `AvaliacaoRepository` (T8); interface `HistoricoAvaliacaoPort` já existe em `cadastros.aluno`
**Requirement**: N/A (fecha o contrato deixado por `cadastros-base`)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `existeAvaliacaoNaoCancelada` retorna `true`/`false` corretamente para os casos: nenhuma avaliação, só `CANCELADA`, com uma não-`CANCELADA`
- [ ] Contexto Spring completo sobe sem erro de bean ambíguo entre `HistoricoAvaliacaoAdapter` e `HistoricoAvaliacaoPortStub` (o `@Primary` resolve; qualquer `*ControllerIT`/`*IT` já existente que suba o contexto completo serve de prova indireta)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): add real HistoricoAvaliacaoPort adapter`

---

### T27: Implementar `finalizarInativas` e `AvaliacaoFinalizacaoScheduler`

**What**: `AvaliacaoService.finalizarInativas(): int` - busca via `AvaliacaoRepository.findByStatusAndUltimaAtividadeEmBefore(EM_ANDAMENTO, agora.minus(24h))`, finaliza cada uma com `tempoUtilizadoSegundos = tempoConfiguradoSegundos` (reusa `recalcularResultado`). `AvaliacaoFinalizacaoScheduler` com `@Scheduled(cron = "0 0 * * * *")` chamando o método acima; adicionar `@EnableScheduling` em `FluenciaLeitoraApplication` (primeiro uso no projeto - design.md, Risks & Concerns).
**Where**: `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoFinalizacaoScheduler.java`, `src/main/java/com/missio/fluencia_leitora/FluenciaLeitoraApplication.java` (modify: `@EnableScheduling`)
**Depends on**: T15, T8
**Reuses**: `recalcularResultado` (T15), `AvaliacaoRepository` (T8)
**Requirement**: AVA-17 (parte de 24h)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `finalizarInativas()` finaliza só `EM_ANDAMENTO` com `ultimaAtividadeEm` > 24h atrás, deixa `PAUSADA` e avaliações recentes intocadas (teste unitário com relógio controlado)
- [ ] `AvaliacaoFinalizacaoScheduler.finalizarInativas()` chama o service (teste unitário do scheduler, sem depender do cron real disparar)
- [ ] `@EnableScheduling` presente; contexto Spring sobe sem erro (qualquer `*ControllerIT` existente confirma isso indiretamente)
- [ ] Gate check passes: `./mvnw test`
- [ ] Test count: >= 5 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(avaliacao): add finalizarInativas and hourly scheduler`

---

## Phase Execution Map

Visual representation of task ordering. Phases run in sequence, and tasks within a phase run in order:

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10 → Phase 11 → Phase 12

Phase 1:   T1   T2   (independentes entre si)
Phase 2:   T2 --------------> T3
           T3 --------------> T4
           T4 --------------> T5
           T5 --------------> T6
           T5 --------------> T7
Phase 3:   T5 --------------> T8
           T6, T7 -----------> T9
Phase 4:   T5 --------------> T10
           T5 --------------> T11
Phase 5:   T1, T8, T10 ------> T12
           T12, T11 ----------> T13
Phase 6:   T12 --------------> T14
           T12 --------------> T15
           T14, T15, T11 -----> T16
Phase 7:   T15, T9 -----------> T17
           T17, T10, T11 -----> T18
Phase 8:   T16 --------------> T19
           T17, T9 -----------> T20
           T19, T20, T11 -----> T21
Phase 9:   T16 --------------> T22
           T22 --------------> T23
Phase 10:  T16 --------------> T24
           T24 --------------> T25
Phase 11:  T8 --------------> T26
Phase 12:  T15, T8 -----------> T27
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

**How phase-based execution works:** see `tasks.md` reference ([tasks.md](../../../.claude/skills/tlc-spec-driven/references/tasks.md)) - the orchestrator packs consecutive whole phases into ~7-task batches and offers sub-agents when that yields more than one batch. With 27 tasks across 12 phases, this is expected to pack into ~4 batches.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | 1 repository method | ✅ Granular |
| T2 | 1 migration file | ✅ Granular |
| T3 | 3 tiny enums (cohesive, same precedent as bancopalavras T4) | ✅ Granular |
| T4 | 1 entity | ✅ Granular |
| T5 | 1 entity (aggregate root) | ✅ Granular |
| T6 | 1 entity | ✅ Granular |
| T7 | 1 entity | ✅ Granular |
| T8 | 1 repository | ✅ Granular |
| T9 | 2 trivial repositories (cohesive, both build-gate-only) | ✅ Granular |
| T10 | 5 cohesive request DTOs (one creation flow) | ✅ Granular |
| T11 | 3 cohesive response DTOs | ✅ Granular |
| T12 | 1 service method | ✅ Granular |
| T13 | 1 endpoint | ✅ Granular |
| T14 | 4 cohesive transition methods (one shared guard/table) | ✅ Granular |
| T15 | 1 service method + 1 shared helper | ✅ Granular |
| T16 | 5 cohesive endpoints (same guard pattern) | ✅ Granular |
| T17 | 2 cohesive service methods (individual + lote) | ✅ Granular |
| T18 | 2 cohesive endpoints | ✅ Granular |
| T19 | 1 service method | ✅ Granular |
| T20 | 1 service method | ✅ Granular |
| T21 | 2 cohesive endpoints | ✅ Granular |
| T22 | 1 service method | ✅ Granular |
| T23 | 1 endpoint | ✅ Granular |
| T24 | 2 cohesive service methods | ✅ Granular |
| T25 | 2 cohesive endpoints | ✅ Granular |
| T26 | 1 adapter class | ✅ Granular |
| T27 | 1 service method + 1 scheduler class | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | None | ✅ Match |
| T2 | None | None | ✅ Match |
| T3 | T2 | T2→T3 | ✅ Match |
| T4 | T3 | T3→T4 | ✅ Match |
| T5 | T4 | T4→T5 | ✅ Match |
| T6 | T5 | T5→T6 | ✅ Match |
| T7 | T5 | T5→T7 | ✅ Match |
| T8 | T5 | T5→T8 | ✅ Match |
| T9 | T6, T7 | T6→T9, T7→T9 | ✅ Match |
| T10 | T5 | T5→T10 | ✅ Match |
| T11 | T5 | T5→T11 | ✅ Match |
| T12 | T1, T8, T10 | T1→T12, T8→T12, T10→T12 | ✅ Match |
| T13 | T12, T11 | T12→T13, T11→T13 | ✅ Match |
| T14 | T12 | T12→T14 | ✅ Match |
| T15 | T12 | T12→T15 | ✅ Match |
| T16 | T14, T15, T11 | T14→T16, T15→T16, T11→T16 | ✅ Match |
| T17 | T15, T9 | T15→T17, T9→T17 | ✅ Match |
| T18 | T17, T10, T11 | T17→T18, T10→T18, T11→T18 | ✅ Match |
| T19 | T16 | T16→T19 | ✅ Match |
| T20 | T17, T9 | T17→T20, T9→T20 | ✅ Match |
| T21 | T19, T20, T11 | T19→T21, T20→T21, T11→T21 | ✅ Match |
| T22 | T16 | T16→T22 | ✅ Match |
| T23 | T22 | T22→T23 | ✅ Match |
| T24 | T16 | T16→T24 | ✅ Match |
| T25 | T24 | T24→T25 | ✅ Match |
| T26 | T8 | T8→T26 | ✅ Match |
| T27 | T15, T8 | T15→T27, T8→T27 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Repository (MatriculaRepository, extended) | integration | integration | ✅ OK |
| T2 | Migration | none | none | ✅ OK |
| T3 | Enums | none | none | ✅ OK |
| T4 | Entity | none | none | ✅ OK |
| T5 | Entity | none | none | ✅ OK |
| T6 | Entity | none | none | ✅ OK |
| T7 | Entity | none | none | ✅ OK |
| T8 | Repository (AvaliacaoRepository) | integration | integration | ✅ OK |
| T9 | Repository (trivial x2) | none (matrix: exercised indirectly) | none | ✅ OK |
| T10 | DTO | none | none | ✅ OK |
| T11 | DTO | none | none | ✅ OK |
| T12 | Service | unit | unit | ✅ OK |
| T13 | Controller | integration | integration | ✅ OK |
| T14 | Service | unit | unit | ✅ OK |
| T15 | Service | unit | unit | ✅ OK |
| T16 | Controller | integration | integration | ✅ OK |
| T17 | Service | unit | unit | ✅ OK |
| T18 | Controller | integration | integration | ✅ OK |
| T19 | Service | unit | unit | ✅ OK |
| T20 | Service | unit | unit | ✅ OK |
| T21 | Controller | integration | integration | ✅ OK |
| T22 | Service | unit | unit | ✅ OK |
| T23 | Controller | integration | integration | ✅ OK |
| T24 | Service | unit | unit | ✅ OK |
| T25 | Controller | integration | integration | ✅ OK |
| T26 | Adapter | unit | unit | ✅ OK |
| T27 | Service + Scheduler | unit | unit | ✅ OK |

---

## Tips

(see `tlc-spec-driven` skill `references/tasks.md` for the general tips - not repeated here)

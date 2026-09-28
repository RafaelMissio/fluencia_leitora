# Áudio de Avaliação Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/audio-avaliacao/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: none - strong defaults applied, mesmo padrão de cobertura já usado nas features anteriores (1:1 com spec ACs para lógica de domínio). Amostra de testes existentes: `JwtServiceTest` (unit puro, sem Spring context - instancia a classe direto e chama o método `@PostConstruct` manualmente no `@BeforeEach`, mesmo padrão que esta feature segue, já que `AudioStorageLocalAdapter` tem a mesma forma - `@Value` no construtor + `@PostConstruct`). Diferença desta feature: usa `@TempDir` do JUnit para o diretório, e não precisa de Testcontainers/MySQL (sem tabela própria - design.md, Data Models).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Interface (`AudioStoragePort`) | none | - (build gate only; comportamento exercitado pelos testes do adapter) | - | `./mvnw compile` |
| Exceções (`AudioStorageException` + 4 subclasses) | none | - (build gate only; cada uma é lançada e verificada pelos testes do adapter, T3/T4) | - | `./mvnw compile` |
| Adapter (`AudioStorageLocalAdapter`) | unit | Todos os branches; 1:1 com AUD-01..AUD-08; todo edge case listado (tamanho zero, mimeType nulo, path traversal) tem um teste | `src/test/java/com/missio/fluencia_leitora/audioavaliacao/AudioStorageLocalAdapterTest.java` | `./mvnw test` |

## Gate Check Commands

> Feature sem tabela/controller (design.md, Data Models) - não há testes de integração, então não existe um gate "Full" nesta feature. `./mvnw test` já roda toda a suíte unitária, inclusive a desta feature.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que adicionam testes unitários (sem Docker) | `./mvnw test` |
| Build | Tasks só de interface/exceção sem teste novo, ou fechamento de fase | `./mvnw compile` (e `./mvnw test` no fechamento da fase) |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Contrato

```
T1
T2
```

### Phase 2: Adapter em disco

```
T1 -> T3
T2 -> T3
```

> **Deviation (during Execute):** T3+T4 do plano original foram fundidas numa única T3. `AudioStorageLocalAdapter` precisa implementar `AudioStoragePort` por inteiro para compilar - dividir `armazenar` e `recuperar` em commits separados forçaria um método placeholder/não funcional num dos dois (`recuperar` teria que existir de alguma forma já em T3, mesmo que "provisório", só para o `implements` compilar). Juntar os dois evita código descartável; nenhum AC, teste ou escopo foi reduzido - a task fundida cobre exatamente o que T3+T4 cobririam juntas.

---

## Task Breakdown

### T1: Criar `AudioStoragePort`

**What**: Interface Java com as duas assinaturas do design: `String armazenar(byte[] conteudo, String mimeType)` e `byte[] recuperar(String referencia)`.
**Where**: `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioStoragePort.java`
**Depends on**: None
**Reuses**: padrão porta/adapter de `common/security/ContextoUsuarioPort.java` (design.md, Code Reuse Analysis)
**Requirement**: N/A (contrato - suporte a AUD-01..AUD-08)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Interface compila com as duas assinaturas exatas do design.md
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(audio-avaliacao): add AudioStoragePort interface`

---

### T2: Criar hierarquia de exceções

**What**: `AudioStorageException` (abstract, `extends RuntimeException`) e as 4 subclasses do design: `AudioFormatoInvalidoException`, `AudioTamanhoInvalidoException`, `AudioArmazenamentoException` (com construtor que aceita uma `Throwable cause`, para embrulhar `IOException`), `AudioNaoEncontradoException`.
**Where**: `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioStorageException.java`, `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioFormatoInvalidoException.java`, `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioTamanhoInvalidoException.java`, `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioArmazenamentoException.java`, `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioNaoEncontradoException.java`
**Depends on**: None
**Reuses**: N/A (nova - decisão de não usar `common.error.BusinessException`, ver design.md Components)
**Requirement**: N/A (suporte a AUD-03, AUD-04, AUD-05, AUD-06)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] As 5 classes compilam; as 4 subclasses estendem `AudioStorageException`
- [x] `AudioArmazenamentoException` tem um construtor `(String, Throwable)` para embrulhar `IOException`
- [x] Gate check passes: `./mvnw compile`

**Tests**: none
**Gate**: build

**Commit**: `feat(audio-avaliacao): add AudioStorageException hierarchy`

---

### T3: Implementar `AudioStorageLocalAdapter` (`armazenar` + `recuperar`)

**What**: Classe `@Component implements AudioStoragePort`. Construtor com `@Value("${APP_AUDIO_STORAGE_DIR:data/audios}") String diretorioBase` e `@Value("${APP_AUDIO_TAMANHO_MAXIMO_BYTES:26214400}") long tamanhoMaximoBytes`; método `@PostConstruct` que cria o diretório configurado (`Files.createDirectories`), embrulhando qualquer `IOException` em `AudioArmazenamentoException`. Método `armazenar(byte[] conteudo, String mimeType)`: valida `mimeType` (nulo/vazio/fora de `audio/webm`, `audio/ogg`, `audio/mp4`, `audio/mpeg`, `audio/wav` → `AudioFormatoInvalidoException`), valida tamanho (`conteudo.length == 0` ou `> tamanhoMaximoBytes` → `AudioTamanhoInvalidoException`), gera um nome de arquivo único (`UUID.randomUUID()` + extensão derivada do `mimeType` - tabela fixa de 5 entradas, design.md Tech Decisions), grava o arquivo (`Files.write`) e devolve o nome gerado como referência; se a escrita falhar, exclui qualquer arquivo parcial e lança `AudioArmazenamentoException`. Método `recuperar(String referencia)`: resolve o caminho dentro do diretório base e confirma que o resultado fica dentro dele (normaliza e checa `startsWith`, design.md Risks & Concerns); se o arquivo não existir ou o caminho resolver fora do diretório base, lança `AudioNaoEncontradoException`; caso contrário, devolve os bytes (`Files.readAllBytes`).
**Where**: `src/main/java/com/missio/fluencia_leitora/audioavaliacao/AudioStorageLocalAdapter.java`
**Depends on**: T1, T2
**Reuses**: padrão `@Value`/`@PostConstruct` de `common/security/JwtService.java:32,35` (design.md, Code Reuse Analysis)
**Requirement**: AUD-01, AUD-02, AUD-03, AUD-04, AUD-05, AUD-06, AUD-07, AUD-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `armazenar` com bytes válidos, mimeType permitido e tamanho dentro do limite grava o arquivo no diretório configurado e devolve uma referência não nula (AUD-01)
- [x] Duas chamadas a `armazenar` com o mesmo conteúdo/mimeType devolvem referências diferentes (nome único, AUD-08)
- [x] `mimeType` fora da lista permitida lança `AudioFormatoInvalidoException` e não cria nenhum arquivo no diretório (AUD-03)
- [x] `mimeType` nulo ou vazio lança `AudioFormatoInvalidoException` (Edge Case)
- [x] Tamanho zero (`byte[0]`) lança `AudioTamanhoInvalidoException` e não cria nenhum arquivo (AUD-04, Edge Case)
- [x] Tamanho acima do limite configurado lança `AudioTamanhoInvalidoException` e não cria nenhum arquivo (AUD-04)
- [x] Apontar `APP_AUDIO_STORAGE_DIR` para um subdiretório que ainda não existe dentro do `@TempDir` do teste: depois de construir o adapter e chamar o método `@PostConstruct`, o diretório existe (AUD-07)
- [x] Simular falha de escrita (diretório tornado não-gravável depois de criado) lança `AudioArmazenamentoException` e não deixa arquivo novo no diretório (AUD-05)
- [x] `recuperar` com uma referência devolvida por `armazenar` devolve bytes idênticos byte a byte ao conteúdo original gravado (AUD-02) - testado com pelo menos 2 mime types diferentes da lista permitida
- [x] `recuperar` com uma referência que não existe no diretório lança `AudioNaoEncontradoException` (AUD-06)
- [x] `recuperar` com uma referência contendo `../` (tentando escapar do diretório base) lança `AudioNaoEncontradoException`, nunca lê um arquivo fora do diretório configurado (Risks & Concerns)
- [x] Gate check passes: `./mvnw test`
- [x] Test count: >= 11 tests pass em `AudioStorageLocalAdapterTest` (13 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(audio-avaliacao): add AudioStorageLocalAdapter`

---

## Phase Execution Map

Visual representation of task ordering. Phases run in sequence, and tasks within a phase run in order:

```
Phase 1 (T1, T2) precede Phase 2 (T3)

Phase 1:  T1   T2   (independentes entre si)
Phase 2:  T1, T2 --------------> T3
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent works one task at a time, in order.

**Batching**: 3 tasks total → fits a single task-budgeted batch (≤ ~8 tasks) → executed inline, no sub-agents.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: `AudioStoragePort` | 1 interface, 2 assinaturas | ✅ Granular |
| T2: Hierarquia de exceções | 5 classes triviais, coesas (uma família de erro) | ✅ Granular |
| T3: `armazenar` + `recuperar` + construção | 1 classe, 2 métodos públicos que implementam o mesmo contrato + `@PostConstruct` - fundidas de T3+T4 do plano original (ver Deviation na Execution Plan); coesa porque `AudioStoragePort` exige as duas para compilar | ✅ Granular (coesa) |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | (nenhuma seta necessária) | ✅ Match |
| T2 | None | (nenhuma seta necessária) | ✅ Match |
| T3 | T1, T2 | T1 -> T3, T2 -> T3 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: `AudioStoragePort` | Interface | none | none | ✅ OK |
| T2: Hierarquia de exceções | Exceções | none | none | ✅ OK |
| T3: `armazenar` + `recuperar` | Adapter | unit | unit | ✅ OK |

---

## Tips

- Primeiro uso de `java.nio.file` no projeto - testes usam `@TempDir` do JUnit, sem Spring context nem Docker (design.md, Tips).
- `armazenar`/`recuperar` são a interface pública que `avaliacao` vai chamar depois - manter a assinatura estável, igual ao `classificar(int serie, int acertos)` de `regras-classificacao`.
- T3 já cobre a criação do diretório (`@PostConstruct`) porque `armazenar` não funciona sem ele - não vale a pena uma task separada só para isso.

# Cadastros Base Validation

**Date**: 2026-09-27
**Spec**: `.specs/features/cadastros-base/spec.md`
**Diff range**: `aba8d24^..HEAD` (b150c6e), 89 files changed, 4808 insertions(+), 152 deletions(-)
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

All 29 tasks (T1-T29) in `tasks.md` are marked `[x]`. No partial or blocked tasks found.

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T5 | ✅ Done | Build config, error handling, security stub, domínios fixos |
| T6-T11 | ✅ Done | Ano letivo + configuração de palavras |
| T12-T18 | ✅ Done | Professor + turma |
| T19-T27 | ✅ Done | Aluno + matrícula (domínio) |
| T28-T29 | ✅ Done | Aluno + matrícula (API) |

---

## Spec-Anchored Acceptance Criteria

### P1: Gerenciar ano letivo e limites de palavras

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: POST cria ano válido | 201, situação `PLANEJADO`, 5 configs seed (série1=15-20, 2-5=20-60) | `src/test/java/.../AnoLetivoControllerIT.java:73-88` - `assertEquals(15,...getQuantidadeMinima())`/`assertEquals(20,...getQuantidadeMaxima())` série 1; loop `assertEquals(20/60)` séries 2-5 | ✅ PASS |
| AC2: ano duplicado | 409 `ANO_LETIVO_DUPLICADO` | `AnoLetivoControllerIT.java:90-100` - `.andExpect(status().isConflict())` + `jsonPath("$.code").value("ANO_LETIVO_DUPLICADO")` | ✅ PASS |
| AC3: datas/ano inválidos | 422 | `AnoLetivoControllerIT.java:102-120` - `isUnprocessableEntity()` para ano 1999 e para `dataFim < dataInicio` | ✅ PASS |
| AC4: ativar encerra o `ATIVO` anterior | novo `ATIVO`, anterior `ENCERRADO` | `AnoLetivoControllerIT.java:122-134` - `jsonPath("$.situacao").value("ATIVO")` + `assertEquals(ENCERRADO, antigo.getSituacao())`; unit: `AnoLetivoServiceTest.java:102-118` | ✅ PASS |
| AC5: PUT configuração grava | 200 + novos limites | `AnoLetivoControllerIT.java:136-149` - `jsonPath("$.quantidadeMinima").value(10)` | ✅ PASS |
| AC6: limites inválidos (min<1, max>200, min>max) | 422 sem alterar | `ConfiguracaoAvaliacaoServiceTest.java:54-81` - 3 testes com `code` distinto por violação, `verify(...,never()).save(any())`; integration: `AnoLetivoControllerIT.java:151-168` (min>max) | ✅ PASS |
| AC7: série fora de 1-5 | 422 | `ConfiguracaoAvaliacaoServiceTest.java:83-91` - `assertEquals("SERIE_INVALIDA", exception.getCode())` | ✅ PASS |

### P1: Gerenciar professores e turmas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: POST professor nome 3-150 | 201, ativo | `ProfessorControllerIT.java:65-73` - `jsonPath("$.ativo").value(true)`; validation: `ProfessorServiceTest.java:53-60` (`NOME_INVALIDO`) | ✅ PASS |
| AC2: POST turma nome/serie/ano/professor opcional | 201, ativa | `TurmaControllerIT.java:79-93` - `jsonPath("$.ativo").value(true)` | ✅ PASS |
| AC3: nome duplicado (case-insensitive) mesmo ano | 409 `TURMA_DUPLICADA` | `TurmaControllerIT.java:95-108` - payload `"turma b"` vs `"Turma B"`, `jsonPath("$.code").value("TURMA_DUPLICADA")`; repo: `TurmaRepositoryIT.java:41-49` | ✅ PASS |
| AC4: professorId/anoLetivoId inexistente/inativo | 422 referência inválida | `TurmaServiceTest.java:95-123` - 2 testes, `assertEquals("REFERENCIA_INVALIDA",...)`; integration: `TurmaControllerIT.java:110-117` | ✅ PASS |
| AC5: GET professor retorna turmas ativas (0..n) | lista de turmas ativas | `ProfessorControllerIT.java:75-90` - cria 1 ativa + 1 inativa, `jsonPath("$.turmas.length()").value(1)`; unit: `ProfessorServiceTest.java:62-85` (0 e n) | ✅ PASS |
| AC6: inativar professor com turma ativa | 409 `PROFESSOR_COM_TURMA_ATIVA` | `ProfessorControllerIT.java:103-116` - `jsonPath("$.code").value("PROFESSOR_COM_TURMA_ATIVA")` + `assertTrue(...isAtivo())` (não inativou) | ✅ PASS |
| AC7: trocar professor da turma não afeta avaliações | atualiza turma, outros campos intactos | `TurmaControllerIT.java:120-139` - troca só `professorId`, confirma `nome`/`serie`/`anoLetivoId` inalterados | ✅ PASS |

### P1: Gerenciar alunos e matrículas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: POST aluno cria aluno+matrícula | 201, `alunoId`+`matriculaId`, serie/professorId copiados da turma | `AlunoControllerIT.java:117-128` (201 + ids numéricos) + `AlunoServiceTest.java:67-83` (`assertEquals(4, resultado.matricula().getSerie())`, professor/anoLetivo/turma copiados) | ⚠️ PASS parcial - ver nota¹ |
| AC2: nova matrícula outro ano, mesmo `alunoId` | 201, ligada ao mesmo aluno | `MatriculaControllerIT.java:99-114` - `assertEquals(2, matriculas.size())` | ✅ PASS |
| AC3: matrícula duplicada mesmo ano | 409 `MATRICULA_DUPLICADA` | `MatriculaControllerIT.java:116-129` + `MatriculaServiceTest.java:76-90` | ✅ PASS |
| AC4: trocar professorId da matrícula | grava novo professor | `MatriculaServiceTest.java:123-136` + `MatriculaControllerIT.java:131-148` (`jsonPath("$.professorId").value(...)`) | ✅ PASS |
| AC5: transferir matrícula outra turma mesmo ano | atualiza turma+serie | `MatriculaServiceTest.java:138-152` (`assertEquals(4, atualizada.getSerie())`) + `MatriculaControllerIT.java:131-148` | ✅ PASS |
| AC6: alterar nome com avaliação não cancelada | 409 `ALUNO_COM_AVALIACAO` | `AlunoServiceTest.java:170-183` + `AlunoControllerIT.java:186-201` (mock `HistoricoAvaliacaoPort` via `@MockitoBean`, confirma nome não muda no banco) | ✅ PASS |
| AC7: `anoFinalizado=true` grava e impede novas avaliações | grava indicador | `MatriculaServiceTest.java:154-165` + `MatriculaControllerIT.java:150-165` (`jsonPath("$.anoFinalizado").value(true)`) | ⚠️ PASS parcial - ver nota² |

### P1: Buscar aluno

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: termo ≥2 chars, contém, accent/case-insensitive, paginado 20/pág, ordenado | lista filtrada, paginação, ordem | `AlunoRepositoryIT.java:62-90` ("joao repocolacao" encontra "João RepoColacao Silva" sem achar "Joana"; paginação 3/pág ordenada); `AlunoControllerIT.java:130-146` (mesmo teste via API) | ⚠️ PASS - ver nota³ (page size 20 confirmado só por leitura de código, `AlunoController.java:31`) |
| AC2: termo <2 chars | 422 | `AlunoServiceTest.java:116-125` + `AlunoControllerIT.java:148-151` | ✅ PASS |
| AC3: perfil PROFESSOR só vê seus alunos (ano `ATIVO`) | escopo por professor | `AlunoServiceTest.java:142-156` + `AlunoControllerIT.java:153-170` + `AlunoRepositoryIT.java:100-119` (native query com JOIN `matricula`/`ano_letivo`) | ✅ PASS |
| AC4: item inclui alunoId/nome/turma/serie/professor/anoLetivo/situação | todos os campos presentes | `AlunoControllerIT.java:137-145` - `jsonPath` para cada campo | ✅ PASS (valores exatos de `situacao`/`anoLetivo` são spec-precision gap documentado em `AlunoBuscaItemResponse.java:12-18`) |

### P2: Consultar domínios fixos

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: GET /ciclos exato, em ordem | 3 ciclos, ordem do seed | `DominioFixoControllerIT.java:25-36` | ✅ PASS |
| AC2: GET /tipos-leitura exato, em ordem | 3 tipos, ordem do seed | `DominioFixoControllerIT.java:38-49` | ✅ PASS |
| AC3: criar/alterar/excluir → 405 | 405 em qualquer verbo de escrita nos 2 endpoints | `DominioFixoControllerIT.java:51-55` - só `POST /ciclos` testado | ❌ GAP - ver nota⁴ |

---

## Edge Cases

| Edge case | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DELETE ano/turma/professor/aluno | 204, sem exclusão física | `AnoLetivoControllerIT.java:170-179`, `TurmaControllerIT.java:141-156`, `ProfessorControllerIT.java:92-101`, `AlunoControllerIT.java:203-214` - todos `isNoContent()` + `assertTrue(...isPresent())` + `assertFalse(...isAtivo())` | ✅ PASS |
| Turma inativa em nova matrícula | 422 | `AlunoServiceTest.java:85-99` (`TURMA_INVALIDA`) + `MatriculaServiceTest.java:92-106` | ✅ PASS |
| Turma de ano `ENCERRADO` em matrícula | 422 | `AlunoServiceTest.java:101-114` (`ANO_LETIVO_ENCERRADO`) + `MatriculaServiceTest.java:108-121` | ✅ PASS |
| Busca sem resultado | 200 + lista vazia | `AlunoRepositoryIT.java:92-98` (repositório - `Page` vazio) | ❌ GAP - ver nota⁵ (sem teste no nível de controller/API para este edge case específico) |
| Lock otimista concorrente | 1ª aceita, 2ª → 409 `CONFLITO_DE_VERSAO` | `AnoLetivoControllerIT.java:181-218` - 2 threads reais + `CyclicBarrier`, confirma um 200 e um 409 com `code=CONFLITO_DE_VERSAO` no corpo | ✅ PASS |

**Status**: ⚠️ Gaps present (3 narrow test-coverage gaps, 1 documented spec-precision gap, all low-risk) - detailed notes below.

---

## Notes on Flagged Items

1. **CAD-11 AC1 (`anoFinalizado=false`)**: o campo tem default `false` incondicional em `Matricula.java:45` (`private boolean anoFinalizado = false;`), mas nenhum teste faz uma asserção direta sobre esse valor no momento da criação (só é confirmado indiretamente pelo fato de `MatriculaControllerIT.patchMarcaAnoFinalizadoTrue` precisar setá-lo para `true`). Risco baixo (campo não é parametrizável na criação), mas evidence-or-zero exige registrar como gap de cobertura, não de comportamento.
2. **CAD-17 AC7 ("impede novas avaliações")**: a parte "grava o indicador" está coberta. A parte "impede novas avaliações nessa matrícula" não é testável dentro de `cadastros-base` porque a feature `avaliacao` (que criaria avaliações) ainda não existe - é o limite de escopo já documentado em `design.md` (`HistoricoAvaliacaoPort`/Risks & Concerns). Não é uma lacuna de implementação desta feature; fica como responsabilidade da feature `avaliacao` consultar `anoFinalizado` ao validar novas avaliações.
3. **CAD-16 AC1 (paginação 20/página)**: o tamanho de página é uma constante (`AlunoController.java:31`, `TAMANHO_PAGINA = 20`) aplicada incondicionalmente; o mecanismo de paginação em si é testado (`AlunoRepositoryIT.buscarPorNomePaginaEOrdenaPorNome`, 3 itens/página), mas nenhum teste de API confirma o valor exato 20. Risco muito baixo (constante hardcoded, não input do usuário).
4. **CAD-18 AC3 (405 em escrita)**: só `POST /api/v1/ciclos` tem teste (`DominioFixoControllerIT.java:51-55`). `PUT`/`DELETE` em `/ciclos` e todos os verbos de escrita em `/tipos-leitura` (5 de 6 combinações verbo×endpoint) não têm teste, embora o comportamento seja o default mecânico do Spring MVC (nenhum handler registrado para esses verbos nesses paths) e portanto de baixíssimo risco de regressão silenciosa.
5. **Edge case "busca vazia → 200"**: coberto no nível de repositório (`AlunoRepositoryIT.buscarPorNomeSemResultadoRetornaPaginaVazia`, confirma `Page` vazio), mas não há teste de `AlunoControllerIT` que faça `GET /api/v1/alunos?nome=<sem-match>` e confirme `status().isOk()` + corpo com lista vazia no nível HTTP. Como este é um dos 5 edge cases nomeados explicitamente no spec, fica registrado como gap real, não cosmético, apesar do baixo risco (nenhum `@ResponseStatus` diferente de 200 é aplicado no `GET`, e o mapeamento de `Page` vazio para JSON é comportamento padrão do Spring).

None desses gaps indica comportamento incorreto na implementação - a inspeção de código confirma que o comportamento esperado existe (`anoFinalizado=false` no construtor, 405 mecânico do Spring para verbos não mapeados, 200 padrão do Spring para `GET` sem match). O que falta é a asserção de teste no nível exato que o spec declara.

---

## Confirmação específica: decisões reportadas pelos batch workers

- **`AnoLetivoService.inativar`/`TurmaService.inativar` sem task explícita**: confirmado. `AnoLetivoService.java:73-80` e `TurmaService` (via `TurmaControllerIT.java:141-156`) implementam soft-delete (`ativo=false`), exercitados via `DELETE` nos respectivos controllers com 204 + linha mantida no banco. `tasks.md` T16 já documenta o desvio explicitamente ("Deviation: adicionado `TurmaService.inativar`..."). Consistente com `design.md`.
- **`HistoricoAvaliacaoPort`/`ContextoUsuarioPort` stubs testados via mock/header, não apenas declarados**: confirmado. `HistoricoAvaliacaoPort` é mockado via `@MockitoBean` em `AlunoControllerIT.java:55-56` e exercitado nos dois cenários (bloqueio e sucesso, linhas 172-201); `ContextoUsuarioHeaderAdapter` tem 3 testes diretos (`ContextoUsuarioHeaderAdapterTest.java:15-47`) e é exercitado via headers HTTP reais em `AlunoControllerIT.java:153-170` (escopo do professor via `X-Perfil`/`X-Professor-Id`).
- **Busca accent-insensitive contra MySQL real (Testcontainers)**: confirmado com evidência concreta. `AlunoRepositoryIT.java:62-75` (`buscarPorNomeEncontraComAcentoECaseInsensitiveSemIncluirNomeDiferente`) roda contra `IntegrationTestBase` (MySQL real via `MySQLContainer`, `support/IntegrationTestBase.java:28-32`), busca `"joao repocolacao"` e confirma que encontra `"João RepoColacao Silva"` (1 resultado) sem incluir `"Joana RepoColacao"`. A migração `V4__aluno_matricula.sql:3` define `nome VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`, exatamente a mitigação do risco documentado em `design.md`. O sensor de discriminação (mutação 3, abaixo) confirma que essa collation é a peça ativa: forçar a query para `BINARY` mata o teste.
- **`spec.md` "20 total, 20 mapped, 0 unmapped, 20 Done"**: confirmado verdadeiro. Reli a tabela de Requirement Traceability (`spec.md:164-185`) e cada `CAD-01`..`CAD-20` tem código e teste correspondentes nos arquivos revisados acima.

---

## Discrimination Sensor

Executado em worktree isolado (`git worktree add /tmp/verifier-scratch-cadastros HEAD`), nunca no repositório real. Baseline `git status --porcelain` vazio antes e depois do sensor (confirmado por `diff`).

| # | File:line (real) | Mutação | Teste(s) afetado(s) | Resultado |
| - | --- | --- | --- | --- |
| 1 | `src/main/java/.../anoletivo/AnoLetivoService.java:39` | Inverteu a condição de duplicidade: `if (anoLetivoRepository.existsByAno(ano))` → `if (!anoLetivoRepository.existsByAno(ano))` | `AnoLetivoServiceTest` (unit) | ✅ Killed - `criarComSucessoGravaOAnoEAsCincoConfiguracoesDoSeed` lança `BusinessException` inesperada; `criarComAnoDuplicadoLanca409SemGravarConfiguracoes` falha (nada é lançado) |
| 2 | `src/main/java/.../common/error/GlobalExceptionHandler.java:29-31` | Trocou o status retornado para `BusinessException` de `ex.getStatus()` (dinâmico) para `HttpStatus.BAD_REQUEST` (fixo) | `GlobalExceptionHandlerTest` (unit) | ✅ Killed - `businessExceptionIsMappedToItsOwnStatusAndCode` espera 409, recebe 400 |
| 3 | `src/main/java/.../aluno/AlunoRepository.java:30-31` | Removeu o comportamento accent-insensitive da query: `LIKE` → `BINARY ... LIKE BINARY ...` (força comparação byte-a-byte, ignora a collation `utf8mb4_0900_ai_ci` da coluna) | `AlunoRepositoryIT` (integration, MySQL real) | ✅ Killed - `buscarPorNomeEncontraComAcentoECaseInsensitiveSemIncluirNomeDiferente` espera 1 resultado, recebe 0 (busca minúscula/sem-acento não encontra "João...") |

**Sensor depth**: lightweight (default tier, 3 mutações)
**Result**: 3/3 killed - PASS ✅
**Isolation check**: `git status --porcelain` idêntico antes/depois (vazio nos dois casos); worktree removido com `git worktree remove --force`.

---

## Code Quality

Amostra revisada: entidade (`Aluno.java`), repositório (`AlunoRepository.java`, `AlunoRepositoryIT.java`), serviço (`AnoLetivoService.java`, `AlunoService.java`, `TurmaService.java`, `ProfessorService.java`, `MatriculaService.java`), controller (`AnoLetivoController.java`, `AlunoController.java`), DTO (`AlunoBuscaItemResponse.java`), migração Flyway (`V1`-`V4`), `GlobalExceptionHandler.java`, `pom.xml`.

| Principle | Status |
| --- | --- |
| Minimum code | ✅ - services curtos, um método por responsabilidade, sem abstrações não usadas |
| Surgical changes | ✅ - nenhuma mudança fora do escopo das tasks nos diffs revisados |
| No scope creep | ✅ - `TurmaService.inativar`/`AnoLetivoService.inativar` são desvios documentados, exigidos pelo `design.md`, não feature extra |
| Matches patterns | ✅ - padrão consistente entre os 5 agregados (records DTO + `from(...)`, `BusinessException` com `code`, soft-delete via `ativo=false`) |
| Spec-anchored outcome check (asserted values match spec) | ✅ - amostragem confirma asserções exatas (códigos de erro, status HTTP, valores numéricos), ver tabela acima |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ⚠️ - domínio 1:1 com ACs confirmado; rotas cobrem happy+erro na maioria, com as 3 lacunas de cobertura já listadas nas Notes |
| Every test maps to a spec requirement - no unclaimed tests | ✅ - todo teste revisado tem javadoc/nome referenciando um CAD-NN | 
| Documented guidelines followed | ✅ - AD-007 (Testcontainers MySQL real, JaCoCo 85%) seguido; `IntegrationTestBase` documenta e justifica o padrão singleton-container (desvio do `@Container` padrão) |

**SPEC_DEVIATION markers found in code**: sim, documentados e justificados em javadoc (não são desvios não anunciados):
- `AnoLetivoService.java:15-20` (parâmetros primitivos em vez de DTO, DTO só existe na T11)
- `AlunoService.java:19-23` (mesmo padrão)
- `AlunoRepository.java:12-25` (LIKE puro em vez de `IgnoreCase` para preservar a collation; native query com JOIN em vez de JPQL sobre `Matricula`, que só existe na T21)
- `IntegrationTestBase.java:14-24` (singleton container em vez de `@Container` por classe, evita restart-churn)

Todos com justificativa técnica válida e sem impacto negativo encontrado.

---

## Gate Check

- **Gate command**: `./mvnw verify` (Full, do `tasks.md`)
- **Result**: 90 passed (44 unit via Surefire + 46 integration via Failsafe), 0 failed, 0 skipped
- **JaCoCo**: `[INFO] All coverage checks have been met.` (linha ≥85%, AD-007)
- **Test count before feature**: 1 (placeholder `FluenciaLeitoraApplicationTests`, removido no commit `d545412`)
- **Test count after feature**: 90
- **Delta**: +89 novos testes (líquido, após remoção do placeholder)
- **Skipped tests**: nenhum
- **Failures**: nenhuma

---

## Requirement Traceability Update

Tabela em `spec.md:164-185` já lista todos os 20 requisitos (`CAD-01`..`CAD-20`) como `Done`, com coverage `20 total, 20 mapped, 0 unmapped, 20 Done`. Confirmado correto por esta verificação independente - nenhuma mudança necessária.

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| CAD-01 a CAD-20 | Done | ✅ Verified (com as ressalvas de cobertura pontuais nas Notes acima, que não invalidam a implementação) |

---

## Summary

**Overall**: ✅ Ready (com 3 fix tasks de cobertura recomendados, nenhum bloqueador)

**Spec-anchored check**: 30/33 critérios com evidência exata `file:line` batendo o outcome preciso do spec; 3 gaps de cobertura de teste (não de comportamento) + 1 spec-precision gap já documentado no código (`situacao`/`anoLetivo` em `AlunoBuscaItemResponse`)
**Sensor**: 3/3 mutações mortas
**Gate**: 90 passed, 0 failed, JaCoCo 85% atingido

**What works**: todas as regras de negócio críticas (duplicidade de ano/turma/matrícula, único `ATIVO`, lock otimista com `CONFLITO_DE_VERSAO`, soft-delete sem exclusão física em ano/turma/professor/aluno, bloqueio de nome via `HistoricoAvaliacaoPort`, escopo de busca por professor via `ContextoUsuarioPort`, busca accent-insensitive via collation `utf8mb4_0900_ai_ci` contra MySQL real) estão implementadas corretamente e cobertas por teste com asserção precisa.

**Issues found**:
1. `DominioFixoControllerIT` só testa `POST /ciclos` → 405; faltam `PUT`/`DELETE` em `/ciclos` e os 3 verbos em `/tipos-leitura`. Fix: adicionar os 5 testes faltantes (comportamento já correto, é só lacuna de teste).
2. Nenhum teste de `AlunoControllerIT` cobre o edge case "busca sem resultado → 200 + lista vazia" no nível de API. Fix: adicionar `getSemResultadoRetorna200ComListaVazia` em `AlunoControllerIT`.
3. `anoFinalizado=false` na criação da matrícula não tem asserção direta. Fix: adicionar `assertEquals(false, ...)` em `AlunoServiceTest.criarComMatriculaComSucessoCopiaSerieEProfessorDaTurma` ou teste dedicado.

**Next steps**: os 3 fix tasks acima são de baixo risco e podem ser tratados como follow-up (não bloqueiam PASS, já que build/sensor passam e nenhum comportamento incorreto foi encontrado). Recomendo abri-los como tasks T30-T32 se o time quiser fechar as lacunas de cobertura antes de considerar `cadastros-base` definitivamente encerrada.

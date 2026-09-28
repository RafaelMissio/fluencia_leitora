# Regras de Classificação Validation

**Date**: 2026-09-28 (re-verified same day, fix→re-verify iteration 2 of 3)
**Spec**: `.specs/features/regras-classificacao/spec.md`
**Diff range**: `9b76b49^..f84eaf2` (updated from `9b76b49^..4a87d84` after fix commits `c688709`, `f84eaf2`)
**Verifier**: independent sub-agent (author ≠ verifier)

**Re-verification note**: this report was updated in place after two fix commits addressed both gaps from the first PASS/FAIL round (see `## Re-verification Log` at the end). The original iteration-1 findings are kept inline (marked "Iteration 1") alongside the iteration-2 results rather than deleted, per the fix→re-verify audit trail.

---

## Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | `ContextoUsuarioPort.usuarioIdAtual()` + `JwtContextoUsuarioAdapter` impl + test (`JwtContextoUsuarioAdapterTest.java:47-53`) |
| T2   | ✅ Done | `GlobalExceptionHandler.handleHandlerMethodValidationException` + test (`GlobalExceptionHandlerTest.java:75-83`) |
| T3   | ✅ Done | `V7__regra_classificacao.sql` - schema + seed matches spec.md Assumptions table exactly (verified by direct read) |
| T4   | ✅ Done | `Fase.java` - 3 values |
| T5   | ✅ Done | `RegraClassificacao.java` - entity + `inativar(Long, Instant)` |
| T6   | ✅ Done | `RegraClassificacaoRepository.java` - 3 queries; concurrency IT rewritten in `c688709` and re-verified genuinely proves the lock (see Discrimination Sensor) |
| T7   | ✅ Done | `FaixaRequest.java`, `SubstituirRegrasClassificacaoRequest.java` - no `@NotEmpty` on list, confirmed by read |
| T8   | ✅ Done | `RegraClassificacaoResponse.java`, `HistoricoVersaoResponse.java` |
| T9   | ✅ Done | `classificar` - 39 unit tests, AC1/AC2 tables covered |
| T10  | ✅ Done | `buscarAtivas`/`buscarHistorico` |
| T11  | ✅ Done | `substituir` - all validators present, REG-07..REG-13 |
| T12  | ✅ Done | `GET /regras-classificacao` |
| T13  | ✅ Done | `PUT /regras-classificacao/series/{serie}` |
| T14  | ✅ Done | `GET /regras-classificacao/historico` + `V8` migration (precision fix) |

All 14 tasks marked done in tasks.md; none blocked or partial.

---

## Spec-Anchored Acceptance Criteria

### P1: Classificar por série e acertos

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: série 1, acertos {0,3,4,5,6,7,8,11,12,20} | PRE_LEITOR/1,1,2,2,3,4; LEITOR_INICIANTE/null,null; LEITOR_FLUENTE/null,null | `RegraClassificacaoServiceTest.java:72-84` (data) + `:105,110-111` (`assertEquals(faseEsperada, resultado.fase())`, `assertEquals(nivelEsperado, resultado.nivel())`) - all 10 rows exact | ✅ PASS |
| AC2: séries 2-5, acertos {0,4,5,7,8,9,10,11,12,30,31,60} | PRE_LEITOR 1,1,2,2,3,3,4,4; LEITOR_INICIANTE null,null; LEITOR_FLUENTE null,null | `RegraClassificacaoServiceTest.java:86-101` (data) + `:116,121-122` - all 12 rows exact, exercised for série 2 | ⚠️ Spec-precision note (see below) |
| AC3: só faixas `ativo=true` usadas | faixa inativa que cobriria 999 é ignorada | `RegraClassificacaoServiceTest.java:126-134` - `assertEquals(Fase.LEITOR_FLUENTE, resultado.fase())` (999 not covered by the inactive-simulating mock) | ✅ PASS |
| AC4: mesmo resultado para os 3 tipos de leitura | 3 chamadas idênticas retornam o mesmo resultado (método não recebe o parâmetro) | `RegraClassificacaoServiceTest.java:136-151` - `assertEquals(primeiraChamada, segundaChamada)` etc. | ✅ PASS |
| AC5: nenhuma faixa cobre → sem erro | `ClassificacaoResultado(null, null)` | `RegraClassificacaoServiceTest.java:153-163` - `assertEquals(null, resultado.fase())`, `assertEquals(null, resultado.nivel())` | ✅ PASS |

**Note on AC2**: the test hard-codes série=2 only; séries 3/4/5 are not separately parameterized. This is a reasonable engineering choice since `classificar()` is série-agnostic code (série is only a repository lookup key, confirmed by reading `RegraClassificacaoService.java:45-59`), and `RegraClassificacaoRepositoryIT.java:51-59` independently confirms série 4's seed data equals the spec table. Not treated as a gap, but flagged as the literal "qualquer série de 2 a 5" quantifier is proven by code-structure argument + a different test, not by one parameterized test covering all four séries.

### P1: Configurar faixas

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: GET retorna faixas ativas ordenadas | 200, 6 faixas, `[0].quantidadeMinimaAcertos=0` | `RegraClassificacaoControllerIT.java:81-88` (`jsonPath("$.length()").value(6)`, `jsonPath("$[0].quantidadeMinimaAcertos").value(0)`) | ✅ PASS |
| AC2: PUT válido inativa+grava+200 | 200, novas faixas ativas, GET seguinte reflete | `RegraClassificacaoControllerIT.java:115-132` (status 200, `$.length()`=3, GET follow-up asserts `$[1].quantidadeMinimaAcertos`=5) + `RegraClassificacaoServiceTest.java:214-247` (auditoria completa) | ✅ PASS |
| AC3: 1ª faixa não começa em 0 → 422 `FAIXA_NAO_INICIA_EM_ZERO` | exact code | `RegraClassificacaoServiceTest.java:257-264` (`assertEquals("FAIXA_NAO_INICIA_EM_ZERO", ex.getCode())`) + `RegraClassificacaoControllerIT.java:162-168` (empty-list sub-case, `$.code`="FAIXA_NAO_INICIA_EM_ZERO") | ✅ PASS |
| AC4: lacuna → 422 `FAIXA_COM_LACUNA` + 1º valor descoberto | exact code + exact value | `RegraClassificacaoServiceTest.java:266-277` (`ex.getDetails().get("valor")`=4) + `RegraClassificacaoControllerIT.java:170-182` (`$.valor`=4) | ✅ PASS |
| AC5: sobreposição → 422 `FAIXA_SOBREPOSTA` + 1º valor duplicado | exact code + exact value | `RegraClassificacaoServiceTest.java:279-305` (mínimos duplicados: valor=5; intervalos: valor=8) + `RegraClassificacaoControllerIT.java:184-196` (`$.valor`=4) | ✅ PASS |
| AC6: última faixa com máximo != null → 422 `FAIXA_FINAL_LIMITADA` | exact code | `RegraClassificacaoServiceTest.java:318-326` + `RegraClassificacaoControllerIT.java:198-209` (`$.code`="FAIXA_FINAL_LIMITADA") | ✅ PASS |
| AC7: nível incoerente com fase → 422 | code `FAIXA_NIVEL_INCOERENTE` (spec doesn't name one; documented Tech Decision) | `RegraClassificacaoServiceTest.java:328-344` + `RegraClassificacaoControllerIT.java:211-220` | ✅ PASS |
| AC8: falha preserva faixas anteriores | nenhuma escrita ocorre; `buscarAtivas` inalterado | `RegraClassificacaoServiceTest.java:356-373` (`verify(repository, never()).buscarAtivasParaAtualizarComLock(...)`, `verify(repository, never()).saveAll(...)`, `assertEquals(faixasAnteriores, aindaAtivas)`) | ✅ PASS |
| AC9: `alteradoPor`/`alteradoEm` gravados nas linhas inativadas | ambos os campos preenchidos, mesmo instante nas duas linhas | `RegraClassificacaoServiceTest.java:233-240` (`assertEquals(42L, anterior1.getAlteradoPor())`, `assertNotNull(anterior1.getAlteradoEm())`, `assertEquals(anterior1.getAlteradoEm(), anterior2.getAlteradoEm())`) | ✅ PASS |

### P2: Consultar histórico de regras

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: GET historico retorna todas as faixas agrupadas por `alteradoEm` desc, corrente primeiro | grouping correct, current group first | `RegraClassificacaoServiceTest.java:177-202` (3 groups, exact order) + `RegraClassificacaoRepositoryIT.java:75-95` (`assertEquals(8, historico.size())`, current 6 first, then recent/old by id) + `RegraClassificacaoControllerIT.java:246-283` (`$[0].alteradoEm` doesNotExist, `$[0].faixas.length()`=1, `$[1].alteradoEm` exists, `$[1].faixas.length()`=2, count = `gruposAntes + 2`) | ✅ PASS |

**Note on the T14 relative-count design (SPEC_DEVIATION, tasks.md T14)**: verified sound. `RegraClassificacaoControllerIT` has no `junit-platform.properties` enabling parallel execution (confirmed: none exists in the project), so JUnit 5 runs this class's tests sequentially in one JVM - the série-5 PUT-success test (T13) and this historico test never race. The relative assertion (`gruposAntes + 2`) is therefore not covering for order-dependent flakiness; it is a deliberate, still-precise design (index 0 and index 1 content are asserted exactly) that correctly avoids coupling to whichever of the two série-5-mutating tests in the class happens to run first. Sound.

### Edge Cases

| Edge Case | `file:line` + assertion | Result |
| --- | --- | --- |
| `quantidadeMinimaAcertos` > `quantidadeMaximaAcertos` → 422 | `RegraClassificacaoServiceTest.java:346-354` (`VALIDACAO_INVALIDA`) | ✅ PASS |
| `quantidadeMinimaAcertos` negativo → 422 | `RegraClassificacaoControllerIT.java:223-231` (`putComQuantidadeMinimaAcertosNegativaRetorna422ValidacaoInvalida` - PUT with `quantidadeMinimaAcertos=-1`, `status().isUnprocessableEntity()`, `jsonPath("$.code").value("VALIDACAO_INVALIDA")`) | ✅ PASS (fixed in `f84eaf2`, was ❌ GAP in iteration 1) |
| lista vazia → 422 `FAIXA_NAO_INICIA_EM_ZERO` | `RegraClassificacaoServiceTest.java:249-255` + `RegraClassificacaoControllerIT.java:162-168` | ✅ PASS |
| faixas mudam → avaliações FINALIZADAS não são alteradas (REG-14) | Deferred - `avaliacao` feature not yet built; deferral documented in spec.md traceability AND `.specs/features/avaliacao/spec.md:171` (Edge Cases) | ⏸ Correctly deferred, not a gap |
| série fora de 1-5 → 422 | `RegraClassificacaoControllerIT.java:105-109,153-158,239-243` (all 3 endpoints) | ✅ PASS |

**Status**: ✅ All edge cases covered as of `f84eaf2` (iteration 1's evidence gap fixed). See `## Re-verification Log` for the Discrimination Sensor re-run on the concurrency-test fix.

---

## Discrimination Sensor

**Iteration 1** (against `4a87d84`): isolated in a temporary git worktree (`git worktree add /tmp/rc-sensor-worktree HEAD`), never the real tree. Baseline `git status --porcelain` before/after sensor work: identical. Worktree discarded with `git worktree remove --force` after all mutations.

| Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- |
| 1 | `RegraClassificacaoService.java:189` | Cross-field validator: overlap check `proxima.quantidadeMinimaAcertos() <= atual.quantidadeMaximaAcertos()` → `<` (off-by-one on the boundary case) | ✅ Killed - `substituirComMinimosDuplicadosLancaFaixaSobrepostaComPrimeiroValorDuplicado` failed (no exception thrown) |
| 2 | `RegraClassificacaoRepository.java:24` | Pessimistic lock: `@Lock(LockModeType.PESSIMISTIC_WRITE)` → `@Lock(LockModeType.NONE)` (removes the row lock entirely) | ❌ **Survived** - `RegraClassificacaoRepositoryIT` ran 4/4 green, BUILD SUCCESS, with the lock completely removed |
| 3 | `RegraClassificacaoService.java:90` | History grouping: `if (grupoAtual == null \|\| !Objects.equals(chaveGrupoAtual, faixa.getAlteradoEm()))` → `if (grupoAtual == null)` (collapses all rows into one group) | ✅ Killed - `buscarHistoricoAgrupaFaixasConsecutivasComMesmoAlteradoEmPreservandoAOrdemDoRepositorio` failed (expected 3 groups, got 1) |

**Iteration 1 outcome** - 2/3 killed, sensor verdict at the time: FAIL (root cause detailed in the original finding, reproduced in `## Re-verification Log` below for the audit trail)

**Iteration 2** (against `f84eaf2`, after fix commit `c688709`): mutation 2 re-run in isolation (mutations 1 and 3 were never in question - only the concurrency test changed) against the fixed `RegraClassificacaoRepositoryIT`. New isolated worktree (`/tmp/rc-sensor-worktree2`), same procedure, discarded after.

| Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- |
| 2 (re-run) | `RegraClassificacaoRepository.java:24` | Same mutation: `@Lock(LockModeType.PESSIMISTIC_WRITE)` → `@Lock(LockModeType.NONE)` | ✅ **Killed** - `RegraClassificacaoRepositoryIT` failed 1/4 (`buscarAtivasParaAtualizarComLockSerializaDuasTransacoesConcorrentesNaMesmaSerie`), `expected: <true> but was: <false>` on the rewritten assertion at line 144, BUILD FAILURE as expected |

**Sensor depth**: lightweight (default tier)
**Result**: 3/3 killed - **PASS** ✅

---

## Interactive UAT Results

Not performed - backend-only feature (REST API + persistence), no UI/interaction-pattern judgment required. Per validate.md §3, automated checks are sufficient for this feature.

---

## Code Quality

| Principle | Status |
| --- | --- |
| Minimum code | ✅ - no unrequested abstractions; 4 near-identical `INSERT` blocks in V7 intentionally left unabstracted per design.md's explicit instruction |
| Surgical changes | ✅ - diff surface matches the 14 tasks; shared-infra touches (T1/T2) are minimal, additive, non-breaking |
| No scope creep | ✅ |
| Matches patterns | ✅ - follows `bancopalavras` structure throughout (entity/repository/service/controller/DTO) |
| Spec-anchored outcome check (asserted values match spec) | ✅ - see AC table above (all criteria and edge cases pass as of `f84eaf2`) |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ✅ - service layer is 1:1 with REG-01..REG-13; controller covers happy+edge+error for all 3 routes; the concurrency IT now genuinely proves the lock (sensor re-run confirms) |
| Every test maps to a spec requirement - no unclaimed tests | ✅ - every test in the 3 feature test files traces to an AC, edge case, or Done-when criterion |
| Documented guidelines followed | ✅ - `AD-007` (JaCoCo ≥85%, Testcontainers MySQL) - `pom.xml:196-208` enforces the 85% line-coverage gate at `verify`, confirmed met (`[INFO] All coverage checks have been met.`) |

---

## Edge Cases

- [x] `quantidadeMinimaAcertos > quantidadeMaximaAcertos` → 422: handled, tested
- [x] `quantidadeMinimaAcertos` negativo → 422: handled, tested (`RegraClassificacaoControllerIT.java:223-231`, fixed in `f84eaf2`)
- [x] lista vazia → 422 `FAIXA_NAO_INICIA_EM_ZERO`: handled, tested
- [x] `avaliacao` finalizadas não reclassificadas (REG-14): correctly deferred, documented in both specs
- [x] série fora de 1-5 → 422: handled, tested on all 3 routes

---

## Gate Check

- **Gate command**: `./mvnw verify` (per tasks.md Gate Check Commands, full gate - required since this feature adds `*RepositoryIT`/`*ControllerIT`)
- **Iteration 1 result** (`4a87d84`): run 1 → 143 unit + 130 integration = 273 tests, 0 failures, BUILD SUCCESS. Run 2 (repeated to rule out IT-ordering flakiness) → identical.
- **Iteration 2 result** (`f84eaf2`, current HEAD, after fix commits `c688709`+`f84eaf2`): 143 unit + 131 integration = **274 tests**, 0 failures, 0 errors, 0 skipped, JaCoCo 85% line coverage check met, BUILD SUCCESS. (`RegraClassificacaoControllerIT` went 17→18 tests: `+1` for `putComQuantidadeMinimaAcertosNegativaRetorna422ValidacaoInvalida`; `RegraClassificacaoRepositoryIT` stayed at 4 tests, same test rewritten not added.) Matches the coordinator's self-reported "274 tests, 0 failures" exactly.
- **Test count before feature**: not independently re-derived (pre-feature baseline not re-run); feature adds `RegraClassificacaoServiceTest` (39), `RegraClassificacaoRepositoryIT` (4), `RegraClassificacaoControllerIT` (18), plus 1 new test each in `JwtContextoUsuarioAdapterTest` and `GlobalExceptionHandlerTest` = 63 new tests total, matching/exceeding every task's stated minimum.
- **Test count after feature**: 274 (143 unit + 131 integration)
- **Delta**: +63 tests attributable to this feature's tasks (+1 vs iteration 1, from the Fix 2 edge-case test)
- **Skipped tests**: none
- **Failures**: none on the real tree (3 total `./mvnw verify` runs across both iterations)

---

## Fix Plans

Both fixes below were applied and independently re-verified. Kept here (not deleted) as the audit trail of iteration 1 → iteration 2.

### Fix 1: Concurrency test for the pessimistic lock is a tautological assertion (surviving mutant) - ✅ RESOLVED in `c688709`

- **Root cause**: `RegraClassificacaoRepositoryIT.java:130,133,138` (pre-fix) compared two `System.nanoTime()` values captured sequentially on the same (main) thread via back-to-back `Future.get()` calls. Per-thread `nanoTime()` monotonicity guaranteed `instanteRetornoTxB >= instanteCommitTxA` regardless of whether `txB` was actually blocked by the lock.
- **Fix applied**: `c688709` rewrote the assertion to measure `txB`'s actual blocking behavior - each transaction now records its own `nanoTime()` from inside its own worker-thread lambda (`instantePoucoAntesDoCommitTxA` set by `txA` right before its lambda returns/commits; `instanteEmQueOSelectDeTxBDesbloqueou` set by `txB` right after its `SELECT ... FOR UPDATE` call returns). See current code at `RegraClassificacaoRepositoryIT.java:104-144`.
- **Independent re-verification**: re-ran the exact same mutation (`@Lock(PESSIMISTIC_WRITE)` → `@Lock(NONE)`) in a fresh isolated worktree (`/tmp/rc-sensor-worktree2`, separate from the coordinator's own self-check worktree). Result: `RegraClassificacaoRepositoryIT` now fails 1/4 with `expected: <true> but was: <false>` on the rewritten assertion (line 144), BUILD FAILURE - the mutant is killed. Confirmed NOT just trusting the coordinator's self-report.
- **Priority**: Major → Closed.

### Fix 2: `quantidadeMinimaAcertos` negativo edge case has no dedicated test - ✅ RESOLVED in `f84eaf2`

- **Root cause**: covered only by the reused `@Min(0)` Bean Validation + existing exception-handler pattern; no test in this feature's suite exercised it end-to-end.
- **Fix applied**: `f84eaf2` added `putComQuantidadeMinimaAcertosNegativaRetorna422ValidacaoInvalida` (`RegraClassificacaoControllerIT.java:223-231`) - `PUT` with `quantidadeMinimaAcertos=-1`, asserts 422 `VALIDACAO_INVALIDA`.
- **Independent re-verification**: confirmed present, correctly structured (uses série 1, a validation-only série per the class's isolation convention, never touches the repository since it fails at Bean Validation before reaching the service), and green in both `./mvnw verify` runs of iteration 2.
- **Priority**: Minor → Closed.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| --- | --- | --- |
| REG-01 | Implementing | ✅ Verified (V7 seed matches spec.md Assumptions exactly) |
| REG-02 | Implementing | ✅ Verified (V7 seed matches spec.md Assumptions exactly) |
| REG-03 | Done | ✅ Verified |
| REG-04 | Done | ✅ Verified |
| REG-05 | Done | ✅ Verified |
| REG-06 | Done | ✅ Verified |
| REG-07 | Done | ✅ Verified - atomic-replace behavior AND the concurrency-serialization sub-claim ("Dois PUT simultâneos... são serializados por lock pessimista") are now both proven; the dedicated test genuinely fails when the lock is removed (confirmed by independent sensor re-run) |
| REG-08 | Done | ✅ Verified |
| REG-09 | Done | ✅ Verified |
| REG-10 | Done | ✅ Verified |
| REG-11 | Done | ✅ Verified |
| REG-12 | Done | ✅ Verified |
| REG-13 | Done | ✅ Verified |
| REG-14 | ⏸ Deferred | ⏸ Deferred (unchanged - correctly documented) |
| REG-15 | Done | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready (PASS)

**Spec-anchored check**: 16/16 criteria matched spec outcome exactly; 1 spec-precision note (AC2 série coverage - not a gap, code-structure argument holds)
**Sensor**: 3/3 mutations killed (mutation 2 re-run independently after the fix - now genuinely kills the mutant)
**Gate**: 274/274 passed (143 unit + 131 integration), 3 total `./mvnw verify` runs across both iterations, all green, JaCoCo ≥85% line coverage met

**What works**: Classification logic (REG-03..REG-05) is exhaustively and exactly tested against both spec tables. Cross-field validation in `substituir` (REG-07..REG-13) has precise, spec-matching assertions for every error code and detail value, including the atomicity guarantee (REG-12) proven via `never()` verification, and now including the negative-bound edge case. History grouping (REG-15) is correctly implemented and tested at 3 layers. Auth boundaries (401/403) are proven end-to-end. The pessimistic lock (REG-07's concurrency sub-claim) is now genuinely proven by its dedicated test - independently confirmed by re-running the exact same fault-injection mutation that caught the original gap. The build gate is solid and reproducible across 3 independent runs.

**Issues found**: none remaining. Both issues from iteration 1 (concurrency test tautology; missing negative-bound edge case test) are fixed and independently re-verified.

**Next steps**: None required for this feature - ready to close. `validate_state.py regras-classificacao` confirmed green (see Re-verification Log).

---

## Re-verification Log

**Trigger**: coordinator reported both iteration-1 gaps fixed - commit `c688709` (concurrency test rewrite) and `f84eaf2` (negative-bound edge case test) - and asked for independent re-verification rather than trusting the self-check described in the fix commit message.

**What was re-run** (2026-09-28, same day):
1. `git log --oneline -1` confirmed HEAD at `f84eaf2`; diff range updated to `9b76b49^..f84eaf2`.
2. Read both fix commits in full (`git show c688709`, `git show f84eaf2`) before trusting anything.
3. Build gate: `./mvnw verify` on the real tree → 274 tests (143 unit + 131 integration), 0 failures, BUILD SUCCESS. Matches the coordinator's self-reported numbers exactly.
4. Discrimination sensor re-run: fresh isolated `git worktree` (`/tmp/rc-sensor-worktree2`, independent of any worktree the coordinator used), same mutation as iteration 1 (`@Lock(PESSIMISTIC_WRITE)` → `@Lock(NONE)`), run against the *rewritten* `RegraClassificacaoRepositoryIT`. Result: mutant killed (`RegraClassificacaoRepositoryIT` 1/4 failed, `expected: <true> but was: <false>` on the new assertion, BUILD FAILURE) - independently confirms the fix works, not merely that the coordinator says it does.
5. Worktree discarded (`git worktree remove --force`); `git status --porcelain` on the real tree confirmed unchanged before/after (`M .specs/LESSONS.md`, `M .specs/features/avaliacao/spec.md`, `M .specs/lessons.json`, `?? .specs/features/regras-classificacao/design.md`, `?? .specs/features/regras-classificacao/validation.md` - all attributable to this Verifier's own report/lesson writes, none to the sensor).
6. Spec-anchored re-check: both affected criteria (negative-bound edge case; REG-07's concurrency sub-claim) re-traced to their new/rewritten `file:line` citations - both now PASS.
7. This report updated in place; `validate_state.py regras-classificacao` re-run (see below).

**Lessons status**: L-025 (surviving_mutant, concurrency-test timestamping) and L-026 (ac_gap, negative-bound edge case) are left as-is (`candidate`, recurrence=1). `lessons.py` has no "resolve on fix" mechanism by design (see `lessons.md`) - a lesson is a general project-local rule for *future* features to apply, not a per-instance issue tracker entry; it only transitions candidate→confirmed after recurrence across ≥2 distinct features, or confirmed→quarantined via `penalize` if a *confirmed* lesson's guidance later fails to prevent a repeat. Fixing this instance doesn't invalidate the general rule (e.g., "capture concurrency-test timestamps from inside each worker thread" remains true guidance regardless of this specific fix), so no lessons.py command was run this round. L-027 (spec_deviation, T14 relative-count pattern) is unaffected - unrelated to either fix.

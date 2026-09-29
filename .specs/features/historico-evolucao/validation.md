# Histórico e Evolução Validation

**Date**: 2026-09-29
**Spec**: `.specs/features/historico-evolucao/spec.md`
**Diff range**: `8e98756^..5cec54a` (feature T1-T8 plus fix commit `5cec54a`). This pass focuses on the fix commit `6539743..5cec54a`.
**Verifier**: independent sub-agent (author ≠ verifier)
**Iteration**: **2 of 3** in the fix→re-verify loop. Iteration 1 (HEAD `6539743`) returned FAIL with 4 ranked gaps. This report re-verifies those gaps against `5cec54a`. Findings that the fix did not touch are carried forward by reference to iteration 1. That report was never committed and is overwritten here. Its per-AC `file:line` citations pointed at test lines that `5cec54a` has since shifted by up to about 20 lines through insertions, but the tests and assertions are unchanged.

## Validation: historico-evolucao (iteration 2) - Result: PASS

All 4 gaps from iteration 1 are closed, with empirical evidence. The previously surviving mutants M4 and M5 are now killed, a new claim-check mutant M7 on the HIST-22 fix is killed, and the full gate is green (614/614, JaCoCo met). One spec-precision note (Anual AC3) and one traceability observation are carried forward. Neither blocks PASS.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T8 | ✅ Done | Unchanged since iteration 1 (see its Task Completion table) |
| Fix round (`5cec54a`) | ✅ Done | Fix 1-4 from iteration 1 are all addressed. It touches 1 main file (`HistoricoEvolucaoService.java`, +8 lines) and 3 test files |

---

## Spec-Anchored Acceptance Criteria

Scope: the ACs and edge cases that were gaps or partial in iteration 1. The other 17 ACs (HIST-02..06, HIST-08..19) are **unchanged, see iteration 1**. The fix commit did not modify any code they depend on. The only main-code change is a guard added in `historico()` *after* aluno lookup and ownership. The full gate re-ran every test cited for them, and all passed.

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| HIST-01 (Histórico AC1): page of FINALIZADA, `dataAvaliacao` desc, **size 20** | Page size 20 | `src/test/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoControllerIT.java:382` - `jsonPath("$.content", hasSize(20))`; `:383` - `$.totalElements == 21`; `:384` - `$.totalPages == 2` (21 FINALIZADA seeded at `:376-378`). Filter and order evidence: unchanged, see iteration 1 | ✅ PASS (M5 now killed) |
| HIST-07 (Ciclo AC1): per cycle, corretas + percentual + classificação | `quantidadeCorretas`, `percentualAcerto`, `fase`/`nivel` | `HistoricoEvolucaoControllerIT.java:413` - `$.entrada.percentualAcerto == 100.00`; `:414` - `$.entrada.fase == "LEITOR_FLUENTE"`; `:415` - `$.entrada.nivel doesNotExist()` (fixture phase has no level). `quantidadeCorretas` and cycle labels: unchanged, see iteration 1 | ✅ PASS |
| HIST-20 / Edge Case 2 (evolucao-anos path): several FINALIZADA in the same (ano, ciclo) → the one with the highest `finalizadoEm` | Most recent wins | Repository: `src/test/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepositoryIT.java:401-404` - `assertEquals(List.of(ano2210Ciclo0Recente, ano2210Ciclo0Antiga, ano2210Ciclo1, ano2211Ciclo0), ids)`. The newer row is inserted *first*, so the order cannot come from insertion or ID. Full stack: `HistoricoEvolucaoControllerIT.java:578` - `$.anos[0].saida.quantidadeCorretas == 18` (the newer row is inserted *second* with 18, the older first with 5). The two tests seed in opposite insertion orders, so neither passes by ID tie-break | ✅ PASS (M4 now killed by both) |
| HIST-22 / Edge Case 4: `cicloId` or `tipoLeitura` not a valid fixed-domain value → 400 | 400 | Numeric out-of-domain: `HistoricoEvolucaoControllerIT.java:304-313` - `cicloId=999999999` → `isBadRequest()` + `$.code == "CICLO_INVALIDO"`; unit `src/test/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoServiceTest.java:188-189` - `assertEquals(BAD_REQUEST, …)`, `assertEquals("CICLO_INVALIDO", …)`. No regression: `tipoLeitura=NAO_EXISTE` → 400 (`HistoricoEvolucaoControllerIT.java:286`) and `cicloId=abc` → 400 (`:298`), both green in the gate | ✅ PASS (M7 killed) |
| Edge Case 1 (evolucao-ciclos): aluno never evaluated → 200, empty result | 200, 3 × `null` | `HistoricoEvolucaoControllerIT.java:422-434` - `isOk()`, `$.entrada`/`$.acompanhamento`/`$.saida` `nullValue()` | ✅ PASS |
| Anual AC3 (HIST-14): "dois anos letivos consecutivos" | Not precisely defined in the spec for year gaps | Implementation compares each row to the preceding row in the filtered list (`HistoricoEvolucaoService.java` javadoc; spec.md Assumptions "Alinhamento da evolução anual entre ciclos"). Numeric outcome evidence: unchanged, see iteration 1 | ⚠️ Spec-precision gap (carried forward, non-blocking) |

**Status**: ✅ All ACs covered (20/20 match the spec outcome). ⚠️ 1 spec-precision gap flagged (Anual AC3).

**HIST-22 implementation review** (`src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoService.java:71-75`): the guard `cicloId != null && !cicloRepository.existsById(cicloId)` runs after `alunoService.buscarPorId` (`:71`, which gives 404 `ALUNO_NAO_ENCONTRADO`) and `verificarPertencimento` (`:72`, which gives 404 for a non-owner), and before the repository query. So a 400 is only reachable by a caller already authorized for an existing aluno. It cannot be used to probe whether an aluno exists or who owns it. The error follows the project's `BusinessException(HttpStatus, code, msg)` pattern. The `cicloId=null` path skips the check, so the query without a filter is unchanged.

---

## Discrimination Sensor

Run in a temporary `git worktree --detach` at HEAD `5cec54a`, under the session scratchpad. Each mutant was applied alone, and the worktree was reset with `git checkout -- .` between mutants (confirmed clean). Targeted failsafe runs: `./mvnw verify -Djacoco.skip=true -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=<Class#method>`. M7 also ran surefire `-Dtest=HistoricoEvolucaoServiceTest`.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| M1-M3, M6 | see iteration 1 | Killed in iteration 1. The code under them was not changed by `5cec54a`, and their killing tests are still green and unchanged | ✅ Killed (iteration 1, carried forward) |
| M4 (re-run) | `src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepository.java:58` | `buscarFinalizadasPorTipo`: `a.finalizadoEm desc` → `asc` | ✅ **Killed** (was Survived): `AvaliacaoRepositoryIT.java:401` - `expected: <[3, 4, 5, 6]> but was: <[4, 3, 5, 6]>`; `HistoricoEvolucaoControllerIT.java:578` - `$.anos[0].saida.quantidadeCorretas expected:<18> but was:<5>` |
| M5 (re-run) | `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoController.java:32` | `TAMANHO_PAGINA = 20` → `10` | ✅ **Killed** (was Survived): `HistoricoEvolucaoControllerIT.historicoPaginaEmBlocosDe20` - `JSON path "$.content" Expected: a collection with size <20>` |
| M7 (new, claim check on the HIST-22 fix) | `src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoService.java:73` | Disabled the guard (`if (false && cicloId != null && …)`) | ✅ Killed: `HistoricoEvolucaoServiceTest.java:184` - `Expected BusinessException to be thrown, but nothing was thrown`; `HistoricoEvolucaoControllerIT.java:312` - `Status expected:<400> but was:<200>` |

**Sensor depth**: lightweight+ (3 mutants run this iteration, 7 across the loop)
**Result**: 3/3 killed this iteration (7/7 cumulative). ✅ PASS
**Isolation**: the real tree's `git status --porcelain` was captured before the sensor work and diffed after cleanup, with an identical result (the pre-existing ` M .specs/LESSONS.md`, ` M .specs/lessons.json` and `?? validation.md` were untouched). HEAD is unchanged at `5cec54a`. The worktree was removed with `--force` and pruned, and `git worktree list` shows only the main tree.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ One 3-line guard plus a constructor dependency. It reuses the existing `CicloRepository` (`JpaRepository.existsById`) |
| Surgical changes | ✅ 1 main file, 3 test files. The test helper `novaAvaliacaoFinalizadaComCorretas` now delegates to a new overload that takes `finalizadoEm`, with the same behavior as before (`Instant.now()`) |
| No scope creep | ✅ Only the 4 gaps were addressed |
| Matches patterns | ✅ `BusinessException(HttpStatus.BAD_REQUEST, "CICLO_INVALIDO", …)`, constructor injection, `lenient()` default stub in the unit test `setUp` |
| Spec-anchored outcome check | ✅ Page size 20, `CICLO_INVALIDO` 400, most recent per group and the DTO fields are all asserted on exact values |
| Per-layer Coverage Expectation met | ✅ The repository layer now tests the ordering tail with 2 FINALIZADA in the same group for **both** queries, and the route layer covers happy, edge and error paths |
| Every test maps to a spec requirement | ✅ The 5 new tests are javadoc-tagged HIST-01 / HIST-20 / HIST-22 / Edge Case 1 |
| Documented guidelines followed: `.specs/STATE.md` AD-007 (JaCoCo ≥85%, Testcontainers MySQL) | ✅ |

**Minor observation (not a gap)**: `$.entrada.nivel` is asserted with `doesNotExist()`, which also passes for a JSON `null`. That is correct for the `LEITOR_FLUENTE` fixture, but no `evolucao-ciclos` test exercises a non-null `nivel`. `nivel` mapping is shared with `historico-avaliacoes`, whose DTO test asserts all 17 fields (iteration 1, `HistoricoEvolucaoControllerIT` historico item test).

---

## Edge Cases

- [x] **E1** (aluno never evaluated → 200 on all 3 endpoints): historico and evolucao-anos: unchanged, see iteration 1. evolucao-ciclos ✅ `HistoricoEvolucaoControllerIT.java:422-434`.
- [x] **E2 / HIST-20** (several FINALIZADA in a group → highest `finalizadoEm`): evolucao-ciclos ✅ (iteration 1, M1/M3). evolucao-anos ✅ `AvaliacaoRepositoryIT.java:401-404` and `HistoricoEvolucaoControllerIT.java:578` (M4 killed).
- [x] **E3** (`anoLetivoId` does not exist → 404 `ANO_LETIVO_NAO_ENCONTRADO`): unchanged, see iteration 1.
- [x] **E4 / HIST-22** (invalid `cicloId`/`tipoLeitura` → 400): numeric out-of-domain ✅ `HistoricoEvolucaoControllerIT.java:304-313`. Non-numeric and bad enum values still ✅ (`:286`, `:298`).

---

## Gate Check

- **Gate command**: `./mvnw verify` (Build level, tasks.md Gate Check Commands), run on the real tree at `5cec54a`
- **Result**: 614 passed, 0 failed, 0 skipped (surefire 316 unit + failsafe 298 integration), `BUILD SUCCESS`, exit 0
- **JaCoCo**: `All coverage checks have been met.` The line ratio from `target/site/jacoco/jacoco.csv` is 98.03% (≥85%, AD-007)
- **Test count before feature**: 543 (290 unit + 253 IT)
- **Test count at iteration 1**: 609 (315 + 294)
- **Test count now**: 614 (316 + 298)
- **Delta vs iteration 1**: +5. That is +1 unit (`historicoComCicloIdForaDoDominioLanca400`) and +4 IT (`historicoComCicloIdForaDoDominioRetorna400`, `historicoPaginaEmBlocosDe20`, `evolucaoCiclosComAlunoSemAvaliacaoRetorna200ComOsTresCiclosNulos`, `evolucaoAnosComDuasFinalizadaNoMesmoAnoECicloUsaAMaisRecente`), which matches the surefire/failsafe split exactly. One existing test was modified: `AvaliacaoRepositoryIT.buscarFinalizadasPorTipoOrdenaPorAnoLetivoDepoisCicloEFiltraEscopo`
- **Test integrity** (`git show 5cec54a`): 0 `@Test` removed, 0 `@Disabled` added, 0 assertions removed. The 7 deleted lines are (a) `Instant.now()` → a shared `agora` in 4 fixtures, (b) renaming `ano2210Ciclo0` → `ano2210Ciclo0Recente`, (c) the 3-element `assertEquals` replaced by a **4-element** one that is a strict superset (same 3 IDs in the same relative order, plus `ano2210Ciclo0Antiga` in position 2), and (d) the IT helper's `setFinalizadoEm(Instant.now())` moved into a delegating overload. The out-of-scope fixtures (TEXTO_CURTO, non-FINALIZADA) are kept. The unit `setUp` adds `lenient().when(cicloRepository.existsById(any())).thenReturn(true)`. This default only lets existing tests pass `cicloId` through the new guard and does not relax any existing `verify`/`assert`
- **Skipped tests**: none
- **Failures**: none

---

## Fix Plans

None required. All 4 iteration-1 fixes are verified closed:

| Iteration-1 gap | Priority | Status | Evidence |
| --------------- | -------- | ------ | -------- |
| Fix 1: HIST-20 evolucao-anos (M4) | Major | ✅ Closed | M4 killed by 2 tests |
| Fix 2: page size 20 (M5) | Minor | ✅ Closed | M5 killed |
| Fix 3: HIST-22 numeric cicloId | Minor | ✅ Closed, option (a) code | M7 killed, and the guard ordering was reviewed |
| Fix 4: evolucao-ciclos DTO fields + E1 | Minor | ✅ Closed | `HistoricoEvolucaoControllerIT.java:413-415`, `:422-434` |

**Non-blocking follow-ups for the spec author (not fix tasks):**

1. **Spec-precision (Anual AC3, carried forward)**: "dois anos letivos consecutivos" does not define behavior across a year gap (2026 → 2028) or when the previous row lacks a cycle. The implementation compares to the immediately preceding row in the filtered list and documents this in the service javadoc and the spec.md Assumptions table. No test covers a year gap. Resolve the wording in spec.md, then add a test once it is decided.
2. **Traceability observation (carried forward)**: spec.md's Mapping note (HIST-12..19 = Anual AC1-8) disagrees with the HIST numbering in tasks.md (T5/T8) and the code javadocs for the division-by-zero, metric and 404 ACs. The Mapping note also mislabels the AC9 and Edge Case 1 cross-references. See iteration 1 for the details. Out of scope for this validation.

---

## Requirement Traceability Update

Recommended statuses. spec.md was **not** edited by the Verifier.

| Requirement | Status at iteration 1 | New Status |
| ----------- | --------------------- | ---------- |
| HIST-01 | ❌ Needs Fix | ✅ Verified |
| HIST-02..06 | ✅ Verified | ✅ Verified |
| HIST-07 | ⚠️ Verified with minor gap | ✅ Verified |
| HIST-08..19 | ✅ Verified | ✅ Verified (HIST-14 with spec-precision note) |
| HIST-20 | ❌ Needs Fix | ✅ Verified |
| HIST-21 | ✅ Verified | ✅ Verified |
| HIST-22 | ❌ Needs Fix | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready (PASS)

**Spec-anchored check**: 20/20 ACs matched the spec outcome. Edge cases E1-E4 all ✅. 1 spec-precision gap flagged (Anual AC3, non-blocking).
**Sensor**: 3/3 killed this iteration (M4, M5 re-run and M7 new). 7/7 cumulative.
**Gate**: 614 passed, 0 failed, 0 skipped. JaCoCo 98.03% (≥85%).

**What works**: every iteration-1 gap is closed with discriminating tests, and both previously surviving mutants are now killed. The HIST-22 guard is correctly placed after authorization, so it does not leak whether an aluno exists. No existing test or assertion was weakened, and the modified repository IT is a strict superset of the old one.

**Issues found**: none blocking.

**Next steps**: apply the Requirement Traceability table to spec.md. Optionally, have the spec author resolve the Anual AC3 wording and the HIST-numbering Mapping note. No new lessons: this is a clean PASS, and candidates L-030..L-033 from iteration 1 stand.

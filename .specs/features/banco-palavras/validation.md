# Banco de Palavras Validation

## Validation: banco-palavras (closing pass, post-`220e4d7`) - PASS ✅

**Date**: 2026-09-28
**Spec**: `.specs/features/banco-palavras/spec.md` (with `design.md`, `tasks.md`)
**Diff range**: `76e2e41..HEAD` (T1-T15 plus fixes `bd91825`, `5d665f0`, `9a45587`, `3f7baf6`, `220e4d7`)
**Verifier**: independent sub-agent (author ≠ verifier). This is a fresh re-verification of all 12 ACs. The sensor targets the `220e4d7` fixes. The DB-constraint sweep was only a light sanity check, because earlier rounds already covered it.

### Fix history

| Round | Verdict | Finding | Fix commit |
| ----- | ------- | ------- | ---------- |
| Iteration 1 | FAIL | Surviving mutants: the `texto` branch in the response and `.trim()`. Partial gaps on PAL-03/05/07/11 | `bd91825`: GET /{id} body, trim, TEXTO_CURTO HTTP happy path, `ativo=true`, format boundaries |
| Iteration 2 | FAIL | The POST >200-itens test used digit-suffixed words, so `@Pattern` rejected the request before `@Size` was checked | `5d665f0`: letter-only words |
| Iteration 3 | FAIL, escalated | `@Size(max=200)` was never exercised through PUT | `9a45587`: PUT 200-itens boundary test |
| Final pass #1 | FAIL | An item (or a TEXTO_CURTO list) without `tipoPalavra` returned 500 at the NOT NULL column. A list-level `tipoPalavra` on PALAVRA/PSEUDOPALAVRA was silently accepted | `3f7baf6`: `@NotNull` on `ItemPalavraRequest.tipoPalavra`, stricter `validarConteudoCompativel` |
| Final pass #2 | FAIL | A TEXTO_CURTO token longer than 60 characters returned 500. `"itens":[null]` returned 500 (NPE). A text that yields zero tokens was accepted as an orphan list | `220e4d7`: token length and empty-token checks in `montarItensDeTexto`; `List<@NotNull ItemPalavraRequest>` on both request DTOs |
| **This pass (closing)** | **PASS** | All 3 `220e4d7` fixes are correct and their mutants are killed. 11/11 in-scope ACs match the spec. There are 2 Minor test-strength gaps (surviving mutants on code that is correct) and no behavioural defects | - |

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1-T15 | ✅ Done | All Done-when boxes have backing tests (spot-checked T10: the list-level `tipoPalavra` box is tested at ST `:156-166` and IT `:192-201`) |

---

## Spec-Anchored Acceptance Criteria

Unit tests are in `ListaPalavrasServiceTest.java` (ST). Integration tests are in `ListaPalavrasControllerIT.java` (IT) and `ListaPalavrasRepositoryIT.java` (RIT). All paths are under `src/test/java/com/missio/fluencia_leitora/`. Line numbers are re-derived at HEAD `220e4d7`.

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| -- | -------------------- | ----------------------- | ------ |
| PAL-01 | 201, list active, item order 1..n | `bancopalavras/ListaPalavrasControllerIT.java:84-91` - `status().isCreated()`, `$.ativo == true`, `$.itens[0].ordem == 1`, `$.itens[1].ordem == 2`; `bancopalavras/ListaPalavrasServiceTest.java:57-61` | ✅ PASS |
| PAL-02 | 422 `NAO_CANONICA_PROIBIDA_1_ANO` + positions | IT `:125-127` - `isUnprocessableEntity()`, `$.code`, `$.posicoes[0] == 2`; ST `:88-90` - `List.of(1, 3)` | ✅ PASS |
| PAL-03 | 422 + item position for an empty word, >60 characters, or an invalid character (accented letters and hyphens accepted) | IT `:568-570` (empty), `:582-584` (61 characters), `:225-227` (`gato123`), each asserting `$.errors[0].field == "itens[0].palavra"`; accepted case at IT `:596-598` (`Ação`, `pé-de-moleque`) | ✅ PASS (the position format is documented in the spec note) |
| PAL-04 | 422 `PALAVRA_DUPLICADA`, case-insensitive after trim | IT `:139-140` (`Gato`/`gato`), `:556-557` (`gato`/`" GATO "`); ST `:104-105` | ✅ PASS |
| PAL-05 | 422 for 0 or >200 itens | IT `:237-238` (0 itens), `:619-621` (POST 201, `errors[0].field == "itens"`), `:392-394` (PUT 201), `:251-252` (null element, new); TEXTO_CURTO: IT `:302-303` (0 tokens, new), `:271` (201 tokens); ST `:212-213`, `:236-237` | ✅ PASS |
| PAL-06 | Changing a list does not change existing evaluations | - | ⏸ Deferred to the `avaliacao` feature (recorded in the spec traceability) |
| PAL-07 | 201 + `quantidadePalavras` | IT `:505-506` - `isCreated()`, `$.quantidadePalavras == 4`; a token longer than 60 characters is rejected with 422 at IT `:286-287` and ST `:224-225` (new) | ✅ PASS |
| PAL-08 | Exactly [O, gato, a, bola], order 1-4 | `common/texto/TokenizadorTextoTest.java:19` - `assertEquals(List.of("O","gato","a","bola"), tokens)`; IT `:512-521`; ST `:188-196` | ✅ PASS |
| PAL-09 | 422 `NAO_CANONICA_PROIBIDA_1_ANO` | ST `:248-250` - status 422, code, `posicoes == [1,2,3]` | ✅ PASS (unit layer, as the Success Criteria require) |
| PAL-10 | Only active lists of the requested series and type, with `id`, `nome`, `quantidadePalavras` | RIT `:39-42` (id, nome, `3L`), `:53` (inactive excluded), `:62` (other series), `:72` (other type); IT `:413`, `:427` (PROFESSOR), `:441` | ✅ PASS |
| PAL-11 | Items in order, plus `texto` for TEXTO_CURTO; an inactive list leaves the filter but stays readable by id | IT `:512-521` (texto + ordered items), `:532` (`texto` null for PALAVRA), `:469` (removed from the filter), `:482-484` (by id, `ativo == false`), `:490` (404) | ✅ PASS |
| PAL-12 | 422 `CONTEUDO_INCOMPATIVEL_COM_TIPO` | IT `:155-156`, `:172-173`, `:187-188`, `:200-201`; ST `:110-178` (6 branches) | ✅ PASS |

**Status**: ✅ 11/11 in-scope ACs match the spec outcome. PAL-06 is deferred.

### `220e4d7` fix confirmation

| Claim | Code | Test assertions | Non-trivial? |
| ----- | ---- | --------------- | ------------ |
| A texto token longer than 60 characters returns 422, not 500 | `ListaPalavrasService.java:187-194` | ST `:224-226` (422, `VALIDACAO_INVALIDA`, `save` never called); IT `:286-287` (422 + code) | ✅ Input `"a"×61` is 1 token of 61 characters. The only rule that can reject it is the new check, since `@Size(max=2000)` and PAL-12 are satisfied. Killed by M1 |
| A texto that yields zero tokens returns 422 | `:177-180` | ST `:236-238`; IT `:302-303` | ✅ `"123 456"` is non-blank, so it passes PAL-12's `temTexto` check. The tokenizer strips the non-letter edges, leaving 0 tokens. Killed by M2 |
| A `null` element in `itens` returns 422, not an NPE 500 | `CriarListaPalavrasRequest.java:27`, `AtualizarListaPalavrasRequest.java:25` | IT `:251-252` (POST, 422 + `VALIDACAO_INVALIDA`) | ✅ for POST (killed by M3). ⚠️ PUT has no test (M4 survived, see Findings) |

---

## Edge Cases

- [x] Itens sent for TEXTO_CURTO, or texto sent for PALAVRA: 422 `CONTEUDO_INCOMPATIVEL_COM_TIPO` (IT `:155-156`, ST `:123-131`)
- [x] Text yielding more than 200 words: 422 (IT `:271`, ST `:212-213`)
- [x] Leading and trailing spaces are trimmed before validation and storage (IT `:543-544`, `"  gato "` stored as `"gato"`)
- [x] Original spelling (case and accents) is preserved (`TokenizadorTextoTest.java:40`, IT `:597`)

---

## Discrimination Sensor

Mutations were run in an isolated `git worktree` at HEAD `220e4d7` (`ListaPalavrasServiceTest` + `ListaPalavrasControllerIT` + `ListaPalavrasRepositoryIT`, `./mvnw verify`). The worktree was removed afterwards, and `git status --porcelain` matched the pre-sensor baseline.

| # | File:line | Mutation | Killed? |
| - | --------- | -------- | ------- |
| M1 | `ListaPalavrasService.java:188` | Removed the >60-character token check (`if (false)`) | ✅ Killed (`ListaPalavrasServiceTest.criarTextoCurtoComPalavraMaiorQue60CaracteresLanca422:222`) |
| M2 | `ListaPalavrasService.java:177` | Removed the empty-tokens check | ✅ Killed (`...criarTextoCurtoSemPalavraNenhumaLanca422:234`) |
| M3 | `dto/CriarListaPalavrasRequest.java:27` | `List<@NotNull ItemPalavraRequest>` changed to `List<ItemPalavraRequest>` | ✅ Killed (`ListaPalavrasControllerIT.postComItemNuloNaListaRetorna422PorFormatoInvalido:248`, 500) |
| M4 | `dto/AtualizarListaPalavrasRequest.java:25` | Same change as M3, on the PUT DTO | ❌ Survived: no PUT test sends a null element |
| M5 | `ListaPalavrasRepository.java:19` | Removed `and l.ativo = true` from `buscarResumo` | ✅ Killed (`ListaPalavrasRepositoryIT:53`, `ListaPalavrasControllerIT:469`) |
| M6 | `ListaPalavrasService.java:188` | `>` changed to `>=` (a 60-character token rejected) | ❌ Survived: there is no accepted-side test at exactly 60 characters on the texto path |

**Sensor depth**: lightweight+ (6 mutations: 3 requested, 1 elsewhere in the feature, 2 probes). **Result**: 4/6 killed. Both survivors are on code that is correct at HEAD. They are test-strength gaps, not behavioural defects (see Findings).

---

## Code Quality

| Check | Status |
| ----- | ------ |
| No features beyond what was asked / no single-use abstractions / no extra flexibility | ✅ |
| Only the required files touched; unrelated code not "improved" | ✅ (`220e4d7` touches the service, the 2 request DTOs, and the 2 test files) |
| Matches existing patterns/style | ✅ (`BusinessException` + `VALIDACAO_INVALIDA`, as the existing >200-token check does; a named constant `LIMITE_CARACTERES_PALAVRA`) |
| Would a senior engineer approve? | ✅ Yes, with the 2 Minor test follow-ups below |
| Tests map to ACs and are non-shallow (spot-check: PAL-05) | ✅ Every bound is tested through both item and texto paths, and each IT isolates its rule (L-018 respected) |
| Spec-anchored outcome check | ✅ 11/11 |
| Per-layer coverage (domain 1:1 with ACs; routes happy + edge + error) | ✅ with the minor PUT null-element gap (M4) |
| Every test maps to an AC, edge case, or Done-when criterion | ✅ |
| Guidelines followed: `.specs/STATE.md` AD-007 (JaCoCo ≥85% lines, Testcontainers MySQL) | ✅ 97.5% line coverage (852/874); `bancopalavras` package 100% |

---

## Gate Check

- **Command**: `./mvnw verify`. No concurrent mvnw, surefirebooter, or failsafebooter processes were running beforehand.
- **Result**: exit 0. Only report files timestamped from this run were counted. Surefire: 102 passed, 0 failed, 0 skipped (17 classes). Failsafe: 109 passed, 0 failed, 0 skipped (18 classes). **Total 211 passed, 0 failed.**
- **Test count before `220e4d7`**: 206. **After**: 211. **Delta**: +5 (2 unit tests, 3 ITs), which matches the commit message.
- **Feature tests**: ServiceTest 23, TokenizadorTextoTest 5, GlobalExceptionHandlerTest 4, ControllerIT 35, RepositoryIT 4 (71 total).

---

## Findings / Fix Plans (none blocking)

### Finding 1 (Minor): the PUT null-element rule is untested (M4 survived)
- The `@NotNull` on the type argument of `AtualizarListaPalavrasRequest.itens` is correct but not tested. If it is removed, PUT `"itens":[null]` would return a 500 again. This is the same failure class as L-019.
- **Fix task**: add `putComItemNuloNaListaRetorna422PorFormatoInvalido` to `ListaPalavrasControllerIT` (422, `VALIDACAO_INVALIDA`).

### Finding 2 (Minor): no accepted-side test for a 60-character token (M6 survived)
- There is no test at exactly 60 characters on the texto path. An off-by-one change would reject valid words and no test would catch it. This is the same failure class as L-005.
- **Fix task**: add a ServiceTest case (`"a".repeat(60)` creates 1 item).

### Note (Cosmetic)
- The 3 new 422s use `VALIDACAO_INVALIDA` with only a message, no `details` (no token position). The spec does not require a position for texto-derived words, so this is acceptable.
- `@Valid` stays at container level on the `List`. The earlier Hibernate Validator deprecation note (HV000271) was not re-checked in this run.
- Carried over from the previous pass: tokens with non-letter characters inside (`"a1b"`) are stored. PAL-03 is written for `itens` only, so this remains a spec-precision note, not a defect.

---

## Requirement Traceability Update

| Requirement | Status |
| ----------- | ------ |
| PAL-01..PAL-05, PAL-07..PAL-12 | ✅ Verified |
| PAL-06 | ⏸ Deferred (`avaliacao`) |

---

## Summary

**Overall**: ✅ Ready. The Minor follow-ups are optional.
**Spec-anchored check**: 11/11 in-scope ACs matched; 1 deferred (PAL-06)
**Sensor**: 4/6 killed (all 3 requested `220e4d7` mutations killed; 2 survivors are Minor test-strength gaps on correct code)
**Gate**: 211 passed, 0 failed

**What works**: CRUD for PALAVRA, PSEUDOPALAVRA, and TEXTO_CURTO lists; the 1st-grade canonical-only rule; format, duplicate, and size validation on both the item path and the texto path; filtered GET and GET by id; soft delete; optimistic lock.

**Next steps**: optionally add the 2 one-line tests (Findings 1-2). Otherwise close the feature.

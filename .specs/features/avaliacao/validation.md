# Avaliação de Leitura Validation

**Date**: 2026-09-28
**Spec**: `.specs/features/avaliacao/spec.md`
**Diff range**: `cf3cfe1..HEAD` (33 commits; code starts at `865250f`, fix at `02ddaf3`)
**Verifier**: independent sub-agent (author ≠ verifier)
**Iteration**: 2 of 3 (re-verification after fix)

---

## Iteration 1 recap

Iteration 1 found 2 gaps, both in `AvaliacaoService.java`:

1. `marcarPalavra`/`marcarPalavras` used plain `@Transactional` despite calling the same lazy-finalization helper (`finalizarSeTempoEsgotado`) as the six transition methods - a marking request that auto-finalizes an avaliação and then itself fails validation (unknown `ordem`, or `PENDENTE` on a just-finalized avaliação) rolled back the finalization along with the marking failure.
2. `enviarAudio` never called `finalizarSeTempoEsgotado` at all - an audio upload arriving on an EM_ANDAMENTO avaliação whose time had already expired incorrectly returned 409 instead of auto-finalizing and accepting the upload.

Commit `02ddaf3` fixed both: `marcarPalavra`/`marcarPalavras` and `enviarAudio` now use `@Transactional(noRollbackFor = BusinessException.class)` (matching the six transition methods), and `enviarAudio` now calls `finalizarSeTempoEsgotado(avaliacao)` right after loading the avaliação. Four new `AvaliacaoControllerIT` tests were added, one per failure branch of each fixed method, each asserting the persisted DB row reflects `FINALIZADA`/`tempoUtilizadoSegundos=60` despite the HTTP response being an error.

This iteration re-verified the whole feature (30 previously-passing ACs spot-re-confirmed, not re-derived from zero) with deepest scrutiny on the fix itself and a fresh discrimination-sensor run targeting the fixed code.

---

## Task Completion

All 27 tasks in `tasks.md` are marked `[x]`. Confirmed against `git log --oneline cf3cfe1..HEAD`: 33 commits total (27 feature commits + 4 docs/handoff commits + the `02ddaf3` fix), one commit per task, matching each task's declared commit message, plus the fix commit `02ddaf3` on top.

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | `865250f` |
| T2 | ✅ Done | `8a4e391` |
| T3 | ✅ Done | `2d526ea` |
| T4 | ✅ Done | `e82a958` |
| T5 | ✅ Done | `a2bd66c` |
| T6 | ✅ Done | `4fbf185` |
| T7 | ✅ Done | `ed260b6` |
| T8 | ✅ Done | `821567f` |
| T9 | ✅ Done | `f75f69a` |
| T10 | ✅ Done | `00aa8a8` |
| T11 | ✅ Done | `32ae2dd` |
| T12 | ✅ Done | `ca2787a` |
| T13 | ✅ Done | `8658f03` |
| T14 | ✅ Done | `8f423bd` |
| T15 | ✅ Done | `d39ef3a` |
| T16 | ✅ Done | `9cef798` |
| T17 | ✅ Done | `ec2323f` |
| T18 | ✅ Done | `3058048` |
| T19 | ✅ Done | `25e5f0a` |
| T20 | ✅ Done | `fa3c925` |
| T21 | ✅ Done | `fba85ae` |
| T22 | ✅ Done | `226c918` |
| T23 | ✅ Done | `9e05e18` |
| T24 | ✅ Done | `1b6aba9` |
| T25 | ✅ Done | `e512820` |
| T26 | ✅ Done | `d572847` |
| T27 | ✅ Done | `d5f3ac2` |
| Fix | ✅ Done | `02ddaf3` - `noRollbackFor` on `marcarPalavra`/`marcarPalavras`; `finalizarSeTempoEsgotado` call added to `enviarAudio` |

---

## Spec-Anchored Acceptance Criteria

### P1: Criar avaliação (AVA-01..AVA-08)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: criação com fonte única | 201, status `CRIADA`, palavras 1..n `PENDENTE`, cópias da matrícula | `AvaliacaoServiceTest.java:235` `criarComPalavrasDigitadasCriaAvaliacaoCriadaComPalavrasPendentesEmOrdemECopiasDaMatricula` | ✅ PASS |
| AC2: aluno não avaliável | 422 `ALUNO_NAO_AVALIAVEL` | `AvaliacaoServiceTest.java:295,302,309,316` | ✅ PASS |
| AC3: quantidade fora do limite | 422 `QUANTIDADE_PALAVRAS_FORA_DO_LIMITE` + `minimo/maximo/informado` | `AvaliacaoServiceTest.java:341` - re-confirmed as a live discriminator this iteration (sensor mutation 3, see below) | ✅ PASS |
| AC4: fonte múltipla/ausente/`texto` fora de TEXTO_CURTO | 422 `CONTEUDO_INVALIDO` | `AvaliacaoServiceTest.java:369,374,382,390` | ✅ PASS |
| AC5: lista de série/tipo diferente | 422 `LISTA_INCOMPATIVEL` | `AvaliacaoServiceTest.java:398,406` | ✅ PASS |
| AC6: tempo fora de 10–600 | 422 | `AvaliacaoServiceTest.java:422-436` | ✅ PASS |
| AC7: data futura/fora do ano ativo | 422 | `AvaliacaoServiceTest.java:439,444,449,456` | ✅ PASS |
| AC8: 1º ano com não-canônica | 422 `NAO_CANONICA_PROIBIDA_1_ANO` + posições | `AvaliacaoServiceTest.java:465` | ✅ PASS |
| AC9: tokenização do texto (PAL-08) | mesma regra do banco-palavras | `AvaliacaoServiceTest.java:499` | ✅ PASS |
| AC10: palavra inválida (vazia/60+/caractere) | 422 com posição | `AvaliacaoServiceTest.java:514,525,536` | ✅ PASS |

### P1: Controlar a execução (AVA-09..AVA-17)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: iniciar (CRIADA→EM_ANDAMENTO) | grava `iniciadoEm`, conta o tempo | `AvaliacaoServiceTest.java:606` | ✅ PASS |
| AC2: pausar soma o trecho | `tempoAcumuladoSegundos` somado | `AvaliacaoServiceTest.java:632` | ✅ PASS |
| AC3: continuar não conta a pausa | tempo pausado excluído | `AvaliacaoServiceTest.java:656` | ✅ PASS |
| AC4: resetar | volta `CRIADA`, zera tempo, `iniciadoEm=null`, palavras `PENDENTE` | `AvaliacaoServiceTest.java:697` | ✅ PASS |
| AC5: finalizar | `FINALIZADA`, `finalizadoEm`, `tempoUtilizado=min(...)`, PENDENTE→NAO_LIDA, resultado+classificação, 200 | `AvaliacaoServiceTest.java:873,889,911` | ✅ PASS |
| AC6: transição inválida | 409 `TRANSICAO_INVALIDA` + `statusAtual`+`acao` | `AvaliacaoServiceTest.java:621,647,685,717` | ✅ PASS |
| AC7: ação repetida idempotente | 200, estado inalterado | `AvaliacaoServiceTest.java:727,743,757` | ✅ PASS |
| AC8: finalização preguiçosa antes do comando | `tempoUtilizado=tempoConfigurado`, comando processado depois, finalização não desfeita mesmo se o comando falhar | `AvaliacaoService.java:150-201` (`noRollbackFor` nos 6 métodos de transição/cancelar) `+` `AvaliacaoService.java:235,241,378` (fix: mesmo `noRollbackFor` em `marcarPalavra`/`marcarPalavras`/`enviarAudio`) `+` `AvaliacaoControllerIT.java:817` `pausarComTempoJaEsgotadoFinalizaEGravaAntesDeResponder409` `+` `AvaliacaoControllerIT.java:1136,1153` (marcação) `+` `AvaliacaoControllerIT.java:1410,1425` (áudio) - **all 4 new fix tests confirmed discriminating by fresh sensor mutations 1/2, see below** | ✅ PASS (was ⚠️ Partial in iteration 1) |
| AC9: log INFO de toda transição | `avaliacaoId`, origem, destino, `usuarioId` | `AvaliacaoServiceTest.java:826`; `AvaliacaoService.java:559-564` | ✅ PASS |

### P1: Marcar palavras (AVA-15, AVA-18, AVA-19)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: marcar individual EM_ANDAMENTO/PAUSADA | grava status, 200 | `AvaliacaoServiceTest.java:1053` | ✅ PASS |
| AC2: marcar em lote tudo-ou-nada | transação única | `AvaliacaoServiceTest.java:1169,1182` | ✅ PASS |
| AC3: CRIADA/CANCELADA bloqueia | 409 `MARCACAO_NAO_PERMITIDA` | `AvaliacaoServiceTest.java:1079` | ✅ PASS |
| AC4: `ordem` inexistente | 404 | `AvaliacaoServiceTest.java:1091`; `AvaliacaoService.java:270-278` | ✅ PASS |
| AC5: FINALIZADA + PENDENTE pedido | 422 | `AvaliacaoServiceTest.java:1102` | ✅ PASS |
| AC6: mudança em FINALIZADA | grava, audita, recalcula, guarda classificação anterior/nova | `AvaliacaoServiceTest.java:1114,1141`; `AvaliacaoService.java:312-333` | ✅ PASS |
| AC7: status igual ao atual | 200 sem auditoria | `AvaliacaoServiceTest.java:1157` | ✅ PASS |
| Transactional integrity of AC4/AC5 under lazy finalization (fix scope) | a marking request whose own validation fails (unknown `ordem`, or `PENDENTE` after finalization) must not undo a finalization the same request just triggered | `AvaliacaoService.java:235,241` (`@Transactional(noRollbackFor = BusinessException.class)`, was plain `@Transactional`); `AvaliacaoControllerIT.java:1136-1150` `marcarPalavraComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaOrdemInexistente`; `AvaliacaoControllerIT.java:1153-1167` `marcarPalavrasEmLoteComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaItemInvalido` - both assert `avaliacaoRepository.findById(id)` is `FINALIZADA` with `tempoUtilizadoSegundos=60` despite the request itself returning 404 | ✅ PASS (was ❌ GAP in iteration 1) |

### P1: Calcular resultado e classificar (AVA-20..AVA-23)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1/2: cálculo (SDD §13, total 20/9/4/7→lidas 13, 45.00%) | valores exatos | `AvaliacaoServiceTest.java:911` | ✅ PASS |
| AC1 (HALF_UP): arredondamento 2/3 | 66.67 (não 66.66) | `AvaliacaoServiceTest.java:941` | ✅ PASS |
| AC3/4: fase/nível por série+corretas | `LEITOR_INICIANTE`/null para série 1, 9 corretas | `AvaliacaoServiceTest.java:911,929` | ✅ PASS |
| AC5: sem classificação | `fase`/`nivel` null, `classificacaoPendente=true`, finaliza mesmo assim | `AvaliacaoServiceTest.java:951` | ✅ PASS |
| AC6: GET com resultado só se FINALIZADA | campos `null` fora de FINALIZADA | `AvaliacaoResponse.java:48-80` (`finalizada ? x : null` em todos os 8 campos condicionais) | ✅ PASS |

### P1: Cancelar avaliação (AVA-24)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: cancela de qualquer status não-CANCELADA, audita | `CANCELADA`, auditoria com status anterior + justificativa | `AvaliacaoServiceTest.java:1371` (parametrizado) | ✅ PASS |
| AC2: justificativa fora de 10-500 | 422 | `AvaliacaoServiceTest.java:1427,1441` | ✅ PASS |
| AC3: CANCELADA→qualquer ação | 409 `TRANSICAO_INVALIDA`; cancelar de novo = 200 idempotente | `AvaliacaoServiceTest.java:1406,1392`; `AvaliacaoService.java:735-737` (`CANCELAR.origens` exclui `CANCELADA`) | ✅ PASS |

### P1: Enviar e baixar o áudio (AVA-27..AVA-32)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: upload em FINALIZADA sem áudio | 201, grava `avaliacao_audio` | `AvaliacaoServiceTest.java:1499`; `AvaliacaoControllerIT.java:1360` | ✅ PASS |
| AC2: fora de FINALIZADA (inclui a finalização preguiçosa) | 409 `AUDIO_ENVIO_NAO_PERMITIDO`; se o tempo já esgotou, finaliza antes e aceita | `AvaliacaoServiceTest.java:1516` (409 direto); `AvaliacaoService.java:379-388` (fix: `finalizarSeTempoEsgotado(avaliacao)` chamado antes da checagem de status, com `noRollbackFor`); `AvaliacaoControllerIT.java:1409-1422` `enviarAudioComTempoJaEsgotadoFinalizaAntesEAceitaOEnvio` - assert 201 + `FINALIZADA`/`tempoUtilizadoSegundos=60`; `AvaliacaoControllerIT.java:1424-1437` `enviarAudioComTempoJaEsgotadoFinalizaEGravaAntesDeResponder422ParaFormatoInvalido` - assert a finalização sobrevive mesmo quando o próprio envio falha por formato inválido - **both confirmed discriminating by fresh sensor mutation 2, see below** | ✅ PASS (was ⚠️ Partial in iteration 1) |
| AC3: áudio já enviado | 409 `AUDIO_JA_ENVIADO` | `AvaliacaoServiceTest.java:1530,1544` | ✅ PASS |
| AC4/5: formato/tamanho inválido | 422 `AUDIO_FORMATO_INVALIDO`/`AUDIO_TAMANHO_INVALIDO` | `AvaliacaoServiceTest.java:1558,1572` | ✅ PASS |
| AC6: download com Content-Type | 200, mesmos bytes, `Content-Type=mimeType` | `AvaliacaoServiceTest.java:1610`; `AvaliacaoControllerIT.java:1360-1372` | ✅ PASS |
| AC7: download sem áudio | 404 | `AvaliacaoServiceTest.java:1624` | ✅ PASS |

### P2: Consultar auditoria (AVA-26)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| AC1: ordem cronológica, campos completos | 2 alterações → 2 registros em ordem | `AvaliacaoServiceTest.java:1330` `consultarAuditoriaDepoisDeDuasAlteracoesRetornaOsDoisRegistrosEmOrdem` | ✅ PASS |

**Status**: ✅ 32/32 ACs matched the spec-defined outcome with direct evidence. Both gaps from iteration 1 (AC8 of "Controlar a execução", AC2 of "Enviar e baixar o áudio") are now closed and confirmed by a fresh discrimination-sensor run against the exact code the fix touched (not a reuse of iteration 1's sensor results).

---

## Edge Cases (spec.md)

- [x] Concorrência (409 `CONFLITO_DE_VERSAO`) - `AvaliacaoControllerIT.java:880-905`, real two-thread test.
- [x] Professor de outro aluno → 404 - `AvaliacaoServiceTest.java:325,812,1245,1598,1636`.
- [x] Professor da matrícula muda depois de criada → avaliação mantém a cópia original - `AvaliacaoService.java:131-146`.
- [x] Todas PENDENTE na finalização → corretas 0, percentual 0.00 - `AvaliacaoServiceTest.java:965`.
- [x] Rotina agendada de hora em hora (>24h) - `AvaliacaoRepositoryIT.java:108-131`, `AvaliacaoServiceTest.java:1659`, `AvaliacaoFinalizacaoSchedulerTest.java:23`.
- [x] Sem exclusão física - no `AvaliacaoRepository`/`AvaliacaoController` há método `delete`.
- [x] Lista inativada depois da cópia não afeta a avaliação já criada - by construction, `palavrasDaLista` (`AvaliacaoService.java:640-651`) copia valores, sem referência viva à lista.
- [x] Faixas de classificação substituídas não reescrevem uma FINALIZADA sem gatilho explícito - by construction, `recalcularResultado` só é chamado de `aplicarFinalizacao`/`marcarComAuditoria`.
- [x] Cancelar uma FINALIZADA com áudio mantém o áudio recuperável - `AvaliacaoControllerIT.java:613` `cancelarUmaFinalizadaComAudioMantemORegistroDeAudioRecuperavel`; `AvaliacaoService.cancelar` nunca referencia `audioStoragePort`.
- [x] `AudioArmazenamentoException` → 500 sem gravar `avaliacao_audio` - `AvaliacaoServiceTest.java:1586`.

### Tabela de status

All 25 cells (5×5) covered: 6 valid transitions + idempotent-repeat cases per action (`AvaliacaoServiceTest.java:606-830,1371-1450`), every invalid-origin cell via parametrized `@EnumSource` tests, plus `depoisDeCancelarQualquerOutraAcaoRetorna409TransicaoInvalida` for the CANCELADA row.

---

## Discrimination Sensor

Isolated scratch worktree: `git worktree add /tmp/avaliacao-sensor-worktree2 HEAD` (`HEAD` = `02ddaf3`, the fix commit). Removed with `git worktree remove --force` after the run; real tree `git status --porcelain` confirmed byte-identical before and after (baseline captured to `/tmp/baseline_status.txt` and diffed post-cleanup - `IDENTICAL - clean`). This is a fresh run - none of iteration 1's mutation results were reused.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ------------ | ------- |
| 1 (fix-target) | `AvaliacaoService.java:235,241` | Removed `noRollbackFor = BusinessException.class` from `marcarPalavra`/`marcarPalavras` (reverted to plain `@Transactional`) | ✅ Killed - `AvaliacaoControllerIT.marcarPalavraComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaOrdemInexistente` and `...marcarPalavrasEmLoteComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaItemInvalido` both fail: `expected: <FINALIZADA> but was: <EM_ANDAMENTO>` - the exact regression the fix closed |
| 2 (fix-target) | `AvaliacaoService.java:381` | Removed the `finalizarSeTempoEsgotado(avaliacao);` call from `enviarAudio` | ✅ Killed - `AvaliacaoControllerIT.enviarAudioComTempoJaEsgotadoFinalizaAntesEAceitaOEnvio` fails `Status expected:<201> but was:<409>`; `...enviarAudioComTempoJaEsgotadoFinalizaEGravaAntesDeResponder422ParaFormatoInvalido` fails `Status expected:<422> but was:<409>` - both reproduce the exact bug the fix closed |
| 3 | `AvaliacaoService.java:698` | Off-by-one on the AVA-03 quantity boundary: `informado < minima` → `informado <= minima` | ✅ Killed - 8 `AvaliacaoServiceTest` errors (`Business Quantidade de palavras fora do limite...`) on every test using the 15-word boundary fixture, e.g. `criarComPalavrasDigitadasCriaAvaliacaoCriadaComPalavrasPendentesEmOrdemECopiasDaMatricula:241` |

**Sensor depth**: lightweight (3 targeted mutations, default tier), 2 of 3 targeting the exact fixed code as required.
**Result**: 3/3 killed - ✅ PASS

**Methodology note**: mutations 1-3 were first injected together and run via `./mvnw -q verify -Dtest=AvaliacaoServiceTest,AvaliacaoControllerIT -Dit.test=AvaliacaoControllerIT` (confirmed mutation 3 killed cleanly via `AvaliacaoServiceTest`, but this surfaced Maven's `-Dtest` explicit-inclusion behavior running `AvaliacaoControllerIT` through Surefire instead of Failsafe, and mutation 3 alone broke nearly every controller-test fixture, producing unreadable collateral noise for mutations 1/2). Mutation 3's kill was already unambiguous from the clean unit-test run, so it was reverted in the scratch tree and mutations 1+2 were re-run in isolation via `failsafe:integration-test failsafe:verify -Dit.test=AvaliacaoControllerIT#<4 fix tests>`, producing the clean, attributable failures recorded above.

---

## Interactive UAT

Skipped - backend-only feature, no UI (per task instructions and validate.md §3).

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ - the fix is a 2-annotation change plus one added method call plus 4 new tests; no speculative abstraction added |
| Surgical changes | ✅ - `git show 02ddaf3` touches exactly `AvaliacaoService.java` (24 lines, mostly Javadoc) and `AvaliacaoControllerIT.java` (+65 lines, new tests only) |
| No scope creep | ✅ - no unrelated method touched; `buscar`, `consultarAuditoria`, `finalizarInativas`, the 6 transition methods are untouched by the fix and remain consistent with it |
| Matches patterns | ✅ - `marcarPalavra`/`marcarPalavras`/`enviarAudio` now use the exact same `@Transactional(noRollbackFor = BusinessException.class)` pattern already established by `iniciar`/`pausar`/`continuar`/`resetar`/`finalizar`/`cancelar` - no new pattern introduced |
| Spec-anchored outcome check (asserted values match spec) | ✅ - the 4 new tests assert the persisted DB row's exact `status`/`tempoUtilizadoSegundos`, not just the HTTP response code |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ✅ - both previously-flagged routes (`marcarPalavra(s)`, `enviarAudio`) now have a dedicated IT test for the lazy-finalization-then-failure interaction, closing the gap iteration 1 flagged here |
| Every test maps to a spec requirement - no unclaimed tests | ✅ - the 4 new tests map to AC8 ("Controlar a execução") and AC2 ("Enviar e baixar o áudio") respectively, both already-numbered ACs |
| Documented guidelines followed | AD-007 (JaCoCo ≥85%, Testcontainers MySQL) - confirmed 96.79% line coverage this run; AD-008 (`BusinessException` details map) - unaffected by the fix |

**Did the fix introduce any new inconsistency?** Checked every other call site touching `finalizarSeTempoEsgotado` and `@Transactional`:
- `buscar` (`AvaliacaoService.java:346-351`): plain `@Transactional`, no `noRollbackFor` - correct as-is, because nothing after `finalizarSeTempoEsgotado` in `buscar` can throw a `BusinessException` (it only returns the loaded avaliação); no rollback risk exists there.
- `consultarAuditoria` (`AvaliacaoService.java:354-358`): `@Transactional(readOnly = true)`, does not call `finalizarSeTempoEsgotado` at all - this is unchanged from iteration 1 and outside the fix's scope (AVA-26 does not require lazy finalization on read); not a regression introduced by this fix.
- `baixarAudio` (`AvaliacaoService.java:411-419`): `@Transactional(readOnly = true)`, does not call `finalizarSeTempoEsgotado` - same as `consultarAuditoria`, a read path with no state mutation at risk; outside the fix's scope and not newly broken by it.
- `finalizarInativas` (`AvaliacaoService.java:442-452`): plain `@Transactional`, no `noRollbackFor` needed - it only calls `aplicarFinalizacao`/`mudarStatus` per item, no `BusinessException` is thrown from that loop.

No other method was left inconsistent by the fix. The fix is narrowly and correctly scoped to exactly the two methods iteration 1 flagged.

**Invented error codes** (`JUSTIFICATIVA_INVALIDA`, `VALIDACAO_INVALIDA`, `DATA_AVALIACAO_INVALIDA`, `PALAVRA_INVALIDA`, `REFERENCIA_INVALIDA`, `STATUS_PALAVRA_INVALIDO`): unchanged from iteration 1 - spec.md does not name codes for these ACs (only the HTTP status), a spec-precision gap in spec.md itself, not an implementation defect. Re-spot-checked: no collisions, consistent usage.

---

## Gate Check

- **Gate command**: `./mvnw verify` (tasks.md, Gate Check Commands - Full/Build level)
- **Result**: 543 tests total, 0 failed, 0 skipped. Verified from fresh (this-run) report files only, cross-checked by timestamp against a pre-existing set of orphaned/stale `target/surefire-reports/*.txt` files left over from earlier ad-hoc single-class Maven invocations during this project's history (e.g. a manually-run `AlunoControllerIT` surefire report from Sep 27 with genuine old failures, never touched by a normal `verify` because `*ControllerIT` is a Failsafe-only pattern by default) - those stale files were excluded from the count.
  - Fresh unit (Surefire, 22 report files, all timestamped to this run): 290 tests, 0 failures, 0 errors.
  - Fresh integration (Failsafe, 22 report files, all timestamped to this run): 253 tests, 0 failures, 0 errors.
  - JaCoCo line coverage (AD-007, ≥85% required, bound to `verify` via `jacoco-maven-plugin:check`): **96.79%** (6936 covered / 7166 total lines, `target/site/jacoco/jacoco.csv`). `mvn verify` exited 0, which would not happen if `jacoco:check` failed its 85% gate.
- **Test count before feature** (iteration 1 baseline): 539 (290 unit + 249 integration).
- **Test count after fix**: 543 (290 unit + 253 integration).
- **Delta**: +4, exactly the 4 new `AvaliacaoControllerIT` tests added by the fix. No unit test count changed, no test was deleted or weakened.
- **Skipped tests**: none.
- **Failures**: none.

---

## Fix Plans

None - both gaps from iteration 1 are closed and independently re-confirmed by a fresh discrimination-sensor run against the fixed code.

---

## Requirement Traceability Update

| Requirement | Previous Status (iteration 1) | New Status |
| ----------- | ------------------------------ | ---------- |
| AVA-01..AVA-16 | ✅ Verified | ✅ Verified (re-confirmed) |
| AVA-17 | ⚠️ Needs Fix | ✅ Verified - `noRollbackFor` fix confirmed via fresh sensor mutations 1/2 |
| AVA-18..AVA-26 | ✅ Verified | ✅ Verified (re-confirmed) |
| AVA-27 | ✅ Verified | ✅ Verified (re-confirmed) |
| AVA-28 | ⚠️ Needs Fix | ✅ Verified - `finalizarSeTempoEsgotado` call in `enviarAudio` confirmed via fresh sensor mutation 2 |
| AVA-29..AVA-32 | ✅ Verified | ✅ Verified (re-confirmed) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 32/32 ACs matched spec outcome with direct evidence (both iteration-1 partials now closed)
**Sensor**: 3/3 mutations killed, 2/3 targeting the exact fixed code
**Gate**: 543 passed (290 unit + 253 integration), 0 failed, JaCoCo 96.79% (≥85% required)

**What works**: Everything iteration 1 confirmed (creation validation, the full status-transition table, SDD §13 result calculation, post-finalization marking with audit trail, cancellation, the audio write-once/download flow, the hourly inactivity scheduler, the `HistoricoAvaliacaoAdapter` `@Primary` bean), plus the two previously-inconsistent write paths (`marcarPalavra`/`marcarPalavras`, `enviarAudio`) now correctly protect an in-flight lazy finalization from being rolled back by their own subsequent validation failure, matching the pattern already used by the six transition methods. The fix is minimal, surgical, and introduces no new inconsistency across the other read/write paths in the service.

**Issues found**: none this iteration.

**Next steps**: none - feature ready to close. Update `.specs/STATE.md` Handoff to mark `avaliacao` Verifier-passed.

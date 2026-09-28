# Autenticação e Perfis Validation

## Validation: autenticacao-perfis - PASS ✅

**Date**: 2026-09-28
**Spec**: `.specs/features/autenticacao-perfis/spec.md`
**Diff range**: `2f21a61..442a190` (21 commits, 53 files)
**Verifier**: independent sub-agent (author ≠ verifier). Re-verification, iteration 2 of 3.

**Verdict**: PASS ✅. The gate is green with 143 tests (up from 141). All 10 sensor mutants were killed, including the 3 that survived iteration 1. Every implemented AC has spec-anchored `file:line` evidence. AUTH-08 is a documented deferral with a pointer in the owning spec. Two spec-precision gaps and three minor observations remain. None of them blocks the feature.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | `0d1a777`, deps in `pom.xml` |
| T2 | ✅ Done | `2c8b52e`, `V5__usuario.sql`, `Usuario`, `UsuarioRepository` |
| T3 | ✅ Done | `f443fb3`. SPEC_DEVIATION (test secret in `IntegrationTestBase.java:44`) is test-only and matches the code |
| T4 | ✅ Done | `174ce84` |
| T5 | ✅ Done | `3f5421d` |
| T6 | ✅ Done | `937a203` (inserted during Execute, `119e475`). Only `Authorization` headers were added |
| T7 | ✅ Done | `9b01b04`. `ContextoUsuarioHeaderAdapter` and its test are gone from the tree |
| T8 | ✅ Done | `bb28f6f` |
| T9 | ✅ Done | `cbe79e8` |
| T10 | ✅ Done | `9e763d2` |
| T11 | ✅ Done | `8dbd45a`. SPEC_DEVIATION (loose params, `Usuario.redefinirSenha`) matches `UsuarioService.java:15-19` and `Usuario.java:104-108` |
| T12 | ✅ Done | `262fc92` |
| T13 | ✅ Done | `afa4ec3` |
| T14 | ✅ Done | `81fd014` |
| T15 | ✅ Done | `61584bd` |
| T16 | ✅ Done | `3bfcab5` |
| T17 | ✅ Done | `d83e9b2` |
| Fix (iter. 1) | ✅ Done | `442a190`: 2 new boundary tests, 1 tightened boundary, AUTH-08 deferral notes |

All 17 tasks are `[x]` in tasks.md and each maps to one commit in the range. None is blocked or partial.

---

## Spec-Anchored Acceptance Criteria

Test paths are relative to `src/test/java/com/missio/fluencia_leitora/`.

### P1: Login

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| AUTH-01: active user, correct credentials → 200 | 200, `accessToken`, `expiresIn=28800`, `perfil`, `professorId` for PROFESSOR | `autenticacao/AuthControllerIT.java:77-84`: `status().isOk()`, `jsonPath("$.expiresIn").value(28800)`, `$.perfil=="PROFESSOR"`, `$.professorId==professor.getId()`, token decodes to `usuario.getId()`; `autenticacao/AuthServiceTest.java:71`: `assertEquals(new Sucesso("token-jwt", 28800L, PROFESSOR, 7L), resultado)`; `common/security/JwtServiceTest.java:53`: `assertEquals(28800L, validadeSegundos)` | ✅ PASS |
| AUTH-02: unknown e-mail or wrong password → 401 | 401, "Credenciais inválidas", identical in both cases | `autenticacao/AuthControllerIT.java:92-100`: `isUnauthorized()`, `$.detail=="Credenciais inválidas"`, `assertEquals(inexistente, senhaErrada)` on the full body; `autenticacao/AuthServiceTest.java:82-83` | ✅ PASS |
| AUTH-03: 5 consecutive failures → 15-min lock, 429 + `Retry-After` even with correct password | 429, `Retry-After` in seconds, 15 min | `autenticacao/AuthControllerIT.java:117-127`: 5×401, then `isTooManyRequests()`, `header().exists("Retry-After")`, `0 < s <= 900`; `autenticacao/AuthServiceTest.java:107-110`: `registrarFalha(eq(10L), captured, eq(5))`, duration in [15m, 15m+5s); `autenticacao/UsuarioRepositoryIT.java:61-72`: lock stays null for failures 1-4, set at the 5th | ✅ PASS |
| AUTH-03 (AC4): success resets the counter | counter 0, lock null | `autenticacao/AuthServiceTest.java:140`: `verify(usuarioRepository).zerarFalhas(10L)`; `autenticacao/UsuarioRepositoryIT.java:86-87`: `assertEquals(0, …)`, `assertNull(getBloqueadoAte())` | ✅ PASS |
| AUTH-04: inactive user → 401 | 401 "Credenciais inválidas" | `autenticacao/AuthControllerIT.java:109-111`: `isUnauthorized()`, `$.detail=="Credenciais inválidas"`; `autenticacao/AuthServiceTest.java:95` | ✅ PASS |
| AUTH-05: BCrypt only, no password in logs | BCrypt hash; password absent from logs | `autenticacao/UsuarioServiceTest.java:83-84`: hash does not contain the plain password, `ENCODER.matches`; `autenticacao/AuthServiceTest.java:153-154`: `assertFalse(output.getAll().contains(SENHA))` and the wrong password | ✅ PASS |

### P1: Route protection by role

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| AUTH-06: no token, invalid or expired token → 401 | 401 | `common/security/SecurityConfigIT.java:50-60`: no token and `Bearer token-invalido` → `isUnauthorized()`, `$.code=="NAO_AUTENTICADO"`. Expired token: `common/security/JwtServiceTest.java:81` → `Optional.empty()`, same filter path as the invalid-token IT | ✅ PASS |
| AUTH-07: PROFESSOR on cadastro/usuário write endpoints → 403 | 403 | Every write endpoint in the codebase has a 403 test: `cadastros/anoletivo/AnoLetivoControllerIT.java:240-249` (POST, ativar, PUT config, DELETE); `cadastros/professor/ProfessorControllerIT.java:146-149` (POST, DELETE); `cadastros/turma/TurmaControllerIT.java:194-201` (POST, PUT, DELETE); `cadastros/aluno/AlunoControllerIT.java:247-254` (POST, PUT, DELETE, plus `$.code=="ACESSO_NEGADO"`); `cadastros/aluno/MatriculaControllerIT.java:193-198` (POST, PATCH); `autenticacao/UsuarioControllerIT.java:168,172` (POST, PUT senha). All `status().isForbidden()` with state unchanged afterward. Classification rules and word lists do not exist yet | ✅ PASS (all existing endpoints) |
| AUTH-08: COORDENADOR on assessment-execution endpoints → 403 | 403 | Deferred, documented. `spec.md:70` carries a **Deferred** note: no execution endpoint exists yet; the test belongs to `avaliacao`/`audio-avaliacao`. Pointers: `.specs/features/audio-avaliacao/spec.md:61` (AC9, COORDENADOR `PUT /avaliacoes/{id}/audio` → 403) and `.specs/features/avaliacao/spec.md:188` (Auth boundaries row cites AUTH-08). Mechanism evidence: `common/security/SecurityConfigIT.java:70-75`, a `@PreAuthorize` role mismatch → 403 ProblemDetail | ✅ Accepted deferral (not a gap) |
| AUTH-09: PROFESSOR accessing another professor's aluno → 404 | 404 | `cadastros/aluno/AlunoControllerIT.java:293-296`: `isNotFound()`, `$.code=="RECURSO_NAO_ENCONTRADO"`, `$.nome` absent; owner 200 at `:279-282`, COORDENADOR 200 at `:267-271`; `common/security/PertencimentoProfessorGuardTest.java:41-42`. Assessment and audio scopes belong to future features | ✅ PASS (aluno scope) |
| AUTH-10: user deactivated after issuance → 401 | 401 on next request | `common/security/JwtAuthenticationFilterTest.java:87`: `assertNull(getAuthentication())` for a deactivated user. An unauthenticated context → 401 is proven at `common/security/SecurityConfigIT.java:50-54` | ✅ PASS (unit + composition) |
| AUTH-15: only login, api-docs, swagger-ui and actuator/health are public | only these 4 | `common/security/SecurityConfigIT.java:80-87`: `/v3/api-docs` 200, `/swagger-ui/index.html` 200, `/actuator/health` 404 (reaches MVC, not 401); login public via `autenticacao/AuthControllerIT.java:77` (no token, 200); other routes 401 at `SecurityConfigIT.java:50` | ✅ PASS (see spec-precision gap 2) |

### P1: User management

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| AUTH-11: POST /usuarios → 201, no password or hash | 201, active, no `senha`/`senhaHash` | `autenticacao/UsuarioControllerIT.java:85-92`: `isCreated()`, `$.ativo==true`, `$.senha` and `$.senhaHash` `doesNotExist()` | ✅ PASS |
| AUTH-11 (AC2): PROFESSOR without valid active professorId, or COORDENADOR with one → 422 | 422 | `autenticacao/UsuarioControllerIT.java:105,110`: `isUnprocessableContent()`, `$.code=="PROFESSOR_ID_INVALIDO"`; `autenticacao/UsuarioServiceTest.java:93-98` (null, missing, inactive professor), `:106-107` (COORDENADOR with id) | ✅ PASS |
| AUTH-12: duplicate e-mail, case-insensitive → 409 `EMAIL_DUPLICADO` | 409, `EMAIL_DUPLICADO` | `autenticacao/UsuarioControllerIT.java:130-131`: upper-cased e-mail → `isConflict()`, `$.code=="EMAIL_DUPLICADO"`; `autenticacao/UsuarioRepositoryIT.java:53`: DB constraint | ✅ PASS |
| AUTH-13: password shorter than 8 → 422 | 422 below 8, accepted at 8 | `autenticacao/UsuarioControllerIT.java:115-116`: 7 chars → 422 `SENHA_INVALIDA`; `autenticacao/UsuarioServiceTest.java:137-140`: exactly 8 chars accepted, `save` called (new in `442a190`, kills R2) | ✅ PASS |
| AUTH-11 (AC5): PUT /usuarios/{id}/senha stores new hash and unlocks | new hash, counter 0, lock null | `autenticacao/UsuarioControllerIT.java:149-156`: `matches("SenhaNova123")`, `assertEquals(0, …)`, `assertNull(bloqueadoAte)`, then login 200 | ✅ PASS |
| AUTH-14: empty DB + env vars → COORDENADOR created | COORDENADOR with those credentials; no-op when users exist | `autenticacao/AdminBootstrapTest.java:50-54`: lower-cased e-mail, `COORDENADOR`, null professorId, active, BCrypt matches; `:70-74`: `count()==1` → `save` never called (tightened in `442a190`, kills R3) | ✅ PASS |

**Status**: ✅ All ACs covered. 20/20 implementable criteria match the spec outcome. AUTH-08 is an accepted, documented deferral. 2 spec-precision gaps are flagged.

**Spec-precision gaps flagged** (non-blocking, lessons L-007/L-008 already record them):
1. AUTH-03: the spec does not say whether the failure counter resets when the 15-min lock expires. The implementation keeps `tentativas_falhas=5`, so one wrong attempt after expiry locks the account again (`src/main/java/com/missio/fluencia_leitora/autenticacao/UsuarioRepository.java:29`). Defensible, but unspecified.
2. AUTH-15: `/actuator/health` is listed as public but returns 404, because actuator is not a project dependency.

---

## Discrimination Sensor

The sensor ran in an isolated `git worktree` at `442a190`, one mutation at a time, with the file restored (`git checkout -- .`) inside the worktree after each run. Unit mutants ran `./mvnw test -Dtest=<Class>`. IT mutants ran `./mvnw verify -Dit.test=<Class>` against Testcontainers MySQL. Each kill below was confirmed by a named failing test, not only a non-zero exit. The worktree was removed afterward. `git status --porcelain` on the real tree matched the baseline before and after.

Source paths are relative to `src/main/java/com/missio/fluencia_leitora/`.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| R1 (iter-1 M11) | `common/security/JwtService.java:38` | `segredo.length < 32` → `<= 32` | ✅ Killed by `JwtServiceTest.segredoComExatamente32BytesNaoFalhaAoSubir:114` |
| R2 (iter-1 M12) | `autenticacao/UsuarioService.java:62` | `senha.length() < 8` → `<= 8` | ✅ Killed by `UsuarioServiceTest.senhaComExatamente8CaracteresEAceita:137` |
| R3 (iter-1 M13) | `autenticacao/AdminBootstrap.java:40` | `count() > 0` → `count() > 1` | ✅ Killed by `AdminBootstrapTest.bancoComUsuariosNaoFazNada:74` |
| N1 | `cadastros/aluno/AlunoController.java:72` | Removed the `PertencimentoProfessorGuard.verificar` call (AUTH-09 bypass) | ✅ Killed by `AlunoControllerIT.getPorIdComProfessorQueNaoEDonoRetorna404` |
| N2 | `common/security/JwtAuthenticationFilter.java:50` | Authority `"ROLE_" + perfil` → hard-coded `"ROLE_COORDENADOR"` (privilege escalation) | ✅ Killed by `JwtAuthenticationFilterTest.tokenValidoDeUsuarioAtivoPopulaContextoComDadosDoBanco:72` |
| N3 | `common/security/SecurityConfig.java:45` | Removed `/api/v1/auth/login` from `permitAll` | ✅ Killed by all 4 `AuthControllerIT` tests |
| N4 | `cadastros/turma/TurmaController.java:41` | Removed `@PreAuthorize` from `PUT /turmas/{id}` | ✅ Killed by `TurmaControllerIT.professorRecebe403EmPostPutEDeleteSemAlterarNada` |
| N5 | `autenticacao/AuthService.java:66` | Login success returns `professorId=null` | ✅ Killed by `AuthServiceTest.loginCertoRetornaTokenExpiresIn28800PerfilEProfessorId:71` |
| N6 | `autenticacao/UsuarioController.java:20` | Removed class-level `@PreAuthorize` on `/usuarios` | ✅ Killed by `UsuarioControllerIT.professorRecebe403EmPostEPutDeUsuarios` |
| N7 | `cadastros/aluno/MatriculaController.java:40` | Removed `@PreAuthorize` from `PATCH /matriculas/{id}` | ✅ Killed by `MatriculaControllerIT.professorRecebe403EmPostEPatchSemAlterarNada` |

**Sensor depth**: P0-full (10 manual mutations: the 3 iteration-1 survivors plus 7 new ones over ownership scoping, authority mapping, the public-route list, per-endpoint and class-level role checks, and the login response)
**Result**: 10/10 killed - PASS ✅

Process note: a first attempt at N1/N3 passed the Maven args as a single quoted string, so Maven failed on an unknown lifecycle phase and no test ran. Those two runs were discarded and re-run correctly. The table above records only the valid runs.

---

## Interactive UAT Results

Skipped. Backend-only feature with no user-facing UI.

---

## Code Quality

Reviewed every changed file in `2f21a61..442a190`, including the fix commit.

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ Small, focused classes. The sealed `LoginResult` maps 1:1 to the 3 HTTP outcomes |
| Surgical changes | ✅ Retrofits add only `@PreAuthorize` and one javadoc line per controller. `GET /alunos/{id}` and `AlunoService.buscarPorId` are justified in design.md (Tech Decisions) |
| No scope creep | ✅ No refresh token, IP rate limit or extra roles |
| Matches patterns | ✅ `BusinessException`/ProblemDetail with `code`, Mockito `*ServiceTest`, `*IT` on `IntegrationTestBase` |
| Spec-anchored outcome check (asserted values match spec) | ✅ Status codes, messages, `expiresIn=28800`, 15-min window, error codes and both boundaries (32 bytes, 8 chars) are asserted exactly |
| Per-layer Coverage Expectation met (domain 1:1 ACs; routes happy+edge+error) | ✅ Service ACs are 1:1; every write route has a 403 case; login covers 200/401/429; usuarios covers 201/409/422/403 |
| Every test maps to a spec requirement - no unclaimed tests | ✅ Each test class javadoc cites its AUTH-xx or edge case |
| Documented guidelines followed: `.specs/STATE.md` AD-007 (JaCoCo ≥85% lines, Testcontainers MySQL) | ✅ "All coverage checks have been met" |

**Fix commit `442a190` review**: the additions are minimal and non-shallow. `JwtServiceTest.java:106-115` pins the byte length (`assertEquals(32, …)`) before asserting startup succeeds. `UsuarioServiceTest.java:128-141` asserts the save happened, not just "no exception". The `AdminBootstrapTest.java:70` change from `3L` to `1L` tightens the boundary and weakens nothing. All three were proven discriminating by R1-R3.

**Minor observations (non-blocking)**:
- The spec's observability row asks for e-mail **and IP** in login logs. `src/main/java/com/missio/fluencia_leitora/autenticacao/AuthService.java:42,50,56,61` log the e-mail only. The numbered AC (AUTH-05, no password in logs) is met.
- Success criterion "one parameterized security test covers every endpoint": not met literally. Per-endpoint 403 tests exist for every write route. 401-without-token is asserted on one route, backed by the single global `anyRequest().authenticated()` rule. Style, not a spec violation.
- `.specs/features/avaliacao/spec.md:188` points at AUTH-08 only through the Auth boundaries dimension row, not as a numbered AC (audio-avaliacao has AC9). Promote it to a numbered AC when `avaliacao` reaches Design. `audio-avaliacao/spec.md:61` says the mechanism is "comprovado por AUTH-07/AUTH-08", which is circular; AUTH-07 alone proves it.

---

## Edge Cases

- [x] Tampered JWT signature → 401: `common/security/JwtServiceTest.java:69` (`Optional.empty()`), feeding the 401 path at `common/security/SecurityConfigIT.java:56-60`
- [x] Startup with no users and no `APP_ADMIN_EMAIL` → WARN "Nenhum usuário cadastrado", startup continues: `autenticacao/AdminBootstrapTest.java:61-65`
- [x] Lock expired → correct password accepted: `autenticacao/AuthServiceTest.java:130-141`
- [x] `APP_JWT_SECRET` under 32 bytes → startup fails: `common/security/JwtServiceTest.java:91-103` (31 bytes). Exactly 32 bytes is accepted: `common/security/JwtServiceTest.java:106-115`

---

## Gate Check

- **Gate command**: `./mvnw verify` (Full gate from tasks.md). Docker and Testcontainers MySQL 8.0 worked.
- **Result**: 143 passed (73 unit via Surefire + 70 integration via Failsafe), 0 failed, 0 skipped. JaCoCo "All coverage checks have been met". BUILD SUCCESS, exit 0.
- **Test count before feature**: 90 (44 unit + 46 IT, per `cadastros-base/validation.md`)
- **Test count at iteration 1**: 141
- **Test count now**: 143
- **Delta**: +53 over the feature, +2 since iteration 1 (the two boundary tests). The count only went up. `ContextoUsuarioHeaderAdapterTest` was removed on purpose with the spoofable adapter (design.md Tech Decisions, T7).
- **Skipped tests**: none
- **Failures**: none

---

## Fix Plans

None required. Optional follow-ups for the orchestrator:

1. **Login log IP** (Minor): add the client IP to the `AuthService` log lines and assert it in `AuthServiceTest.nenhumaLinhaDeLogContemASenha`, or drop "e IP" from the spec's observability row.
2. **AUTH-08 in `avaliacao`** (Minor, traceability): add a numbered AC to `.specs/features/avaliacao/spec.md` requiring COORDENADOR → 403 on create, status transitions and mark-word, with a test per endpoint.
3. **Housekeeping**: two stale worktrees from the iteration-1 session are still registered (`git worktree list` shows `…/3fced886-…/scratchpad/verifier-mut-wt` and `…/scratchpad/wt` at `d83e9b2`). Run `git worktree prune` or remove them.

---

## Requirement Traceability Update

The orchestrator should copy these statuses into `spec.md` (the Verifier writes only this report).

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| AUTH-01 | Pending | ✅ Verified |
| AUTH-02 | Pending | ✅ Verified |
| AUTH-03 | Pending | ✅ Verified (spec-precision gap: counter after expiry) |
| AUTH-04 | Pending | ✅ Verified |
| AUTH-05 | Pending | ✅ Verified |
| AUTH-06 | Pending | ✅ Verified |
| AUTH-07 | Pending | ✅ Verified (all existing write endpoints) |
| AUTH-08 | Pending | ⏭️ Deferred → `avaliacao` / `audio-avaliacao` (documented) |
| AUTH-09 | Pending | ✅ Verified (aluno scope) |
| AUTH-10 | Pending | ✅ Verified |
| AUTH-11 | Pending | ✅ Verified |
| AUTH-12 | Pending | ✅ Verified |
| AUTH-13 | Pending | ✅ Verified |
| AUTH-14 | Pending | ✅ Verified |
| AUTH-15 | Pending | ✅ Verified (spec-precision gap: actuator absent) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 20/20 implementable ACs matched the spec outcome. AUTH-08 is an accepted deferral. 2 spec-precision gaps are flagged.
**Sensor**: 10/10 mutations killed (the 3 iteration-1 survivors are now killed).
**Gate**: 143 passed, 0 failed.

**What works**: login with a generic 401, lockout with 429 and Retry-After, counter reset, stateless JWT with per-request revocation, ProblemDetail 401/403, COORDENADOR-only writes on all 5 cadastros controllers and usuarios, 404 ownership scoping on `GET /alunos/{id}`, user CRUD with 409/422 and exact boundaries, and admin bootstrap.

**Issues found**: none blocking. Minor: IP missing from login logs, no single parameterized all-endpoints test, AUTH-08 pointer in `avaliacao` is a dimension row rather than a numbered AC.

**Next steps**: Mark `autenticacao-perfis` done, update spec.md traceability, and optionally route the minor follow-ups.

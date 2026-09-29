# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

### L-003 - Assert every literal default value stated in an AC's outcome, not just the fields that come directly from user input.
- signal: `ac_gap` · recurrence: 3 feature(s) · scope: `service-tests` · harmful: 0
- features: cadastros-base, banco-palavras, historico-evolucao
- evidence: src/main/java/com/missio/fluencia_leitora/cadastros/aluno/Matricula.java:45 (CAD-11 AC1) (service-tests) (+2 more)
- last seen: 2026-09-29T13:11:19Z

### L-006 - When an AC cannot be exercised because its endpoints belong to a later feature, record the deferral in spec.md traceability and add the AC to the owning feature spec in the same change.
- signal: `ac_gap` · recurrence: 2 feature(s) · scope: `traceability` · harmful: 0
- features: autenticacao-perfis, banco-palavras
- evidence: AUTH-08 (no endpoint exists; design.md Risks promised spec.md deferral note) (traceability) (+1 more)
- last seen: 2026-09-28T09:30:50Z

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - For a read-only endpoint that relies on the framework default 405, add a test for every write verb on every path, not just one verb on one path.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `controller-tests` · harmful: 0
- features: cadastros-base
- evidence: src/test/java/com/missio/fluencia_leitora/cadastros/dominio/DominioFixoControllerIT.java:51-55 (controller-tests)
- last seen: 2026-09-27T19:41:55Z

### L-002 - When a spec edge case names an HTTP status explicitly, add a controller/integration-level test for it even if the same behavior is already covered at the repository layer.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `controller-tests` · harmful: 0
- features: cadastros-base
- evidence: spec.md edge case: busca vazia -> 200 (no AlunoControllerIT coverage) (controller-tests)
- last seen: 2026-09-27T19:41:55Z

### L-004 - When a DTO field's exact values or format are not defined by the spec, document the chosen values inline in the DTO javadoc as a spec-precision gap so a later feature can align instead of silently redefining it.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `dto` · harmful: 0
- features: cadastros-base
- evidence: src/main/java/com/missio/fluencia_leitora/cadastros/aluno/dto/AlunoBuscaItemResponse.java:12-18 (CAD-16 AC4) (dto)
- last seen: 2026-09-27T19:41:55Z

### L-005 - For every numeric threshold in a spec rule, test the exact boundary value on the accepted side, not only a value clearly below and one clearly above.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `boundary-tests` · harmful: 0
- features: autenticacao-perfis
- evidence: validation.md M11/M12/M13: JwtService.java:38, UsuarioService.java:62, AdminBootstrap.java:40 (boundary-tests)
- last seen: 2026-09-28T03:19:27Z

### L-007 - For a lockout rule, state whether the failure counter resets when the lock expires.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `auth` · harmful: 0
- features: autenticacao-perfis
- evidence: AUTH-03: UsuarioRepository.java:29 counter not reset on lock expiry (auth)
- last seen: 2026-09-28T03:19:27Z

### L-008 - Before listing a framework endpoint as public in a spec, confirm the dependency that serves it is in the build.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `security-config` · harmful: 0
- features: autenticacao-perfis
- evidence: AUTH-15: SecurityConfigIT.java:86-87 /actuator/health returns 404, actuator not a dependency (security-config)
- last seen: 2026-09-28T03:19:27Z

### L-009 - When a task adds a fail-fast required config property, register a test value in the shared integration test base in the same task.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `test-infra` · harmful: 0
- features: autenticacao-perfis
- evidence: tasks.md:145 T3 SPEC_DEVIATION (test APP_JWT_SECRET in IntegrationTestBase) (test-infra)
- last seen: 2026-09-28T03:19:27Z

### L-010 - When a service task precedes the task that creates its request DTO, plan the service signature with primitive parameters in tasks.md instead of naming the DTO.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `task-planning` · harmful: 0
- features: autenticacao-perfis
- evidence: tasks.md:344 T11 SPEC_DEVIATION (UsuarioService.criar loose params before T12 DTO) (task-planning)
- last seen: 2026-09-28T03:19:27Z

### L-011 - For every conditional field in a response DTO, add a read-back test asserting both its presence in the matching case and its absence otherwise.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `controller-tests` · harmful: 0
- features: banco-palavras
- evidence: M3 ListaPalavrasResponse.java:28 (controller-tests)
- last seen: 2026-09-28T09:30:49Z

### L-012 - When a spec requires input normalization such as trimming, add a test whose input actually contains the unnormalized form and assert the stored value.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `dto` · harmful: 0
- features: banco-palavras
- evidence: M5 ItemPalavraRequest.java:19 (dto)
- last seen: 2026-09-28T09:30:49Z

### L-013 - When an AC says an error returns an item position, state whether the position is 0-based or 1-based and the field that carries it.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: banco-palavras
- evidence: PAL-03 (spec)
- last seen: 2026-09-28T09:30:50Z

### L-014 - When a response field is only populated for one variant of a type/enum, add a test asserting both the populated and the null branches of that field.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `dto` · harmful: 0
- features: banco-palavras
- evidence: PAL-11 (dto)
- last seen: 2026-09-28T09:33:22Z

### L-015 - Cover the controller happy path for every distinct request variant of an endpoint, not only its error branches, even when the domain logic is already unit-tested.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `controller` · harmful: 0
- features: banco-palavras
- evidence: PAL-07 (controller)
- last seen: 2026-09-28T09:33:22Z

### L-016 - Add a dedicated test for each validation branch the spec lists explicitly (each bound, each format rule), not just one representative case.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `validation` · harmful: 0
- features: banco-palavras
- evidence: PAL-03, PAL-05 (validation)
- last seen: 2026-09-28T09:33:22Z

### L-017 - When an acceptance criterion's guarantee depends on a feature that does not exist yet, mark it explicitly as deferred/untestable in the spec instead of leaving it as a silently-passing requirement.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: banco-palavras
- evidence: PAL-06 (spec)
- last seen: 2026-09-28T09:33:22Z

### L-018 - In a boundary test, keep every other input valid so the boundary under test is the only rule that can reject the request.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `controller-tests` · harmful: 0
- features: banco-palavras
- evidence: ListaPalavrasControllerIT.java:475-487 (mutant M3, iteration 2) (controller-tests)
- last seen: 2026-09-28T09:46:50Z

### L-019 - When the same validation constraint is declared separately on create and update request DTOs, test it through every route that uses each DTO.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `controller-tests` · harmful: 0
- features: banco-palavras
- evidence: validation.md iteration 3, mutant M5 (AtualizarListaPalavrasRequest.java:25) (controller-tests)
- last seen: 2026-09-28T09:56:49Z

### L-020 - For every field an AC names as a required input, add a test that omits it and asserts a 4xx, so a DB NOT NULL constraint never surfaces as a 500.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `request-dtos` · harmful: 0
- features: banco-palavras
- evidence: src/main/java/com/missio/fluencia_leitora/bancopalavras/dto/ItemPalavraRequest.java:16 (probes C/D: missing tipoPalavra -> DataIntegrityViolationException 500) (request-dtos)
- last seen: 2026-09-28T10:07:45Z

### L-021 - Tick a Done-when box only when a test exercises that exact behavior; each checked box needs a test that would fail without it.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `tasks` · harmful: 0
- features: banco-palavras
- evidence: tasks.md:358 (T10 Done-when: list tipoPalavra on PALAVRA -> 422; probe B returned 201) (tasks)
- last seen: 2026-09-28T10:07:45Z

### L-022 - Validate server-derived values such as tokens against the same column constraints as user-supplied fields before persisting them.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `service-validation` · harmful: 0
- features: banco-palavras
- evidence: ListaPalavrasService.java:167-182 (TEXTO_CURTO token >60 chars -> 500) (service-validation)
- last seen: 2026-09-28T10:38:38Z

### L-023 - Annotate collection elements with @NotNull alongside @Valid, since container-level @Valid silently skips null elements.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `dto-validation` · harmful: 0
- features: banco-palavras
- evidence: ListaPalavrasService.java:189 (itens:[null] -> NPE 500) (dto-validation)
- last seen: 2026-09-28T10:38:38Z

### L-024 - When content is derived from free text, apply the spec's count bounds to the derived items too, including the empty result.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: banco-palavras
- evidence: PAL-05 vs TEXTO_CURTO zero tokens -> 201 (spec)
- last seen: 2026-09-28T10:38:38Z

### L-025 - When proving a pessimistic lock with two real transactions, capture each transaction's completion timestamp from inside its own worker thread, not from the main thread's sequential Future.get() calls - per-thread nanoTime monotonicity makes back-to-back get()-then-nanoTime() comparisons pass regardless of whether blocking occurred.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `concurrency-tests` · harmful: 0
- features: regras-classificacao
- evidence: src/test/java/com/missio/fluencia_leitora/regrasclassificacao/RegraClassificacaoRepositoryIT.java:130,133,138 (concurrency-tests)
- last seen: 2026-09-28T14:25:26Z

### L-026 - When a numeric field's negative-value rejection is delegated entirely to a Bean Validation annotation instead of custom service logic, still add an explicit end-to-end test proving the framework enforces it for that exact field.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `dto-validation` · harmful: 0
- features: regras-classificacao
- evidence: spec.md Edge Cases - quantidadeMinimaAcertos negativo (dto-validation)
- last seen: 2026-09-28T14:25:26Z

### L-027 - When two integration tests in the same class mutate the same shared row/identifier without a guaranteed run order, assert a relative before/after delta instead of an absolute total, and confirm no parallel-execution config exists that could actually race them.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `integration-tests` · harmful: 0
- features: regras-classificacao
- evidence: tasks.md T14 Deviations found during implementation - historicoAposDuasSubstituicoesGanhaDoisGruposNovosComOCorrentePrimeiro (integration-tests)
- last seen: 2026-09-28T14:25:26Z

### L-028 - When a lazy state-transition helper can fire inside a write method whose own later validation may still throw, annotate that method's transaction with the same noRollbackFor used on the primary transition methods, not just the methods added first.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `transactions` · harmful: 0
- features: avaliacao
- evidence: AvaliacaoService.java:228-237 (marcarPalavra/marcarPalavras lack noRollbackFor) (transactions)
- last seen: 2026-09-29T00:01:05Z

### L-029 - When design.md documents a shared pre-check as applying to every action of a service, grep every public method of that service for the helper call before marking the task done - do not assume it propagated from the methods it was first written for.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `avaliacao` · harmful: 0
- features: avaliacao
- evidence: AvaliacaoService.java:365-394 (enviarAudio never calls finalizarSeTempoEsgotado) (avaliacao)
- last seen: 2026-09-29T00:01:05Z

### L-030 - When a query's ORDER BY decides which row per group the caller keeps, test it with two rows in the same group whose tie-break values differ.
- signal: `surviving_mutant` · recurrence: 1 feature(s) · scope: `repo-layer` · harmful: 0
- features: historico-evolucao
- evidence: M4 src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoRepository.java:57 (repo-layer)
- last seen: 2026-09-29T13:11:19Z

### L-031 - When a spec says an out-of-domain id returns 400, validate the id against its table, since type conversion only rejects non-numeric input.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `controller` · harmful: 0
- features: historico-evolucao
- evidence: HIST-22 src/main/java/com/missio/fluencia_leitora/historicoevolucao/HistoricoEvolucaoController.java:46 (controller)
- last seen: 2026-09-29T13:11:19Z

### L-032 - When a spec compares consecutive periods, state whether a missing period breaks the comparison or falls back to the last available one.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `spec` · harmful: 0
- features: historico-evolucao
- evidence: spec.md Anual AC3 (HistoricoEvolucaoService.java:136-152) (spec)
- last seen: 2026-09-29T13:11:19Z

### L-033 - Use an explicit @Query to project a single column in Spring Data JPA, since find<Property>By derived names return whole entities.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `repo-layer` · harmful: 0
- features: historico-evolucao
- evidence: SPEC_DEVIATION src/main/java/com/missio/fluencia_leitora/avaliacao/AvaliacaoAudioRepository.java:17 (repo-layer)
- last seen: 2026-09-29T13:11:19Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_

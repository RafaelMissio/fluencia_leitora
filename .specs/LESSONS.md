# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

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

### L-003 - Assert every literal default value stated in an AC's outcome, not just the fields that come directly from user input.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `service-tests` · harmful: 0
- features: cadastros-base
- evidence: src/main/java/com/missio/fluencia_leitora/cadastros/aluno/Matricula.java:45 (CAD-11 AC1) (service-tests)
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

### L-006 - When an AC cannot be exercised because its endpoints belong to a later feature, record the deferral in spec.md traceability and add the AC to the owning feature spec in the same change.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `traceability` · harmful: 0
- features: autenticacao-perfis
- evidence: AUTH-08 (no endpoint exists; design.md Risks promised spec.md deferral note) (traceability)
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

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_

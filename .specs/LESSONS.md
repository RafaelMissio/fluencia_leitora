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

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_

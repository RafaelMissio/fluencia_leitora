# STATE

## Decisions

### AD-001
- **Decision**: As regras de classificação ficam no banco (`regra_classificacao`), com faixas contíguas que cobrem de 0 acertos até "sem limite" para cada série de 1 a 5, e são as mesmas para os três tipos de leitura.
- **Reason**: SDD §12 exige que o coordenador altere as regras sem nova versão da aplicação; SDD §2 diz que as regras valem para os três tipos. As lacunas e sobreposições do SDD §11 foram resolvidas com o seed contíguo aprovado pelo usuário.
- **Trade-off**: Não há regra específica por tipo de leitura; se isso for necessário depois, a tabela ganha uma coluna `tipo_leitura_id` e o validador de faixas muda.
- **Scope**: regras-classificacao, avaliacao, historico-evolucao
- **Date**: 2026-09-27
- **Status**: active

### AD-002
- **Decision**: O professor marca manualmente cada palavra como CORRETA, INCORRETA ou NAO_LIDA; não há reconhecimento de voz no MVP.
- **Reason**: Decisão do usuário (SDD §24 item 5). O reconhecimento automático já está listado como evolução futura no SDD §25.
- **Trade-off**: Mais trabalho para o professor; a precisão depende de ele ouvir com atenção ou reproduzir o áudio.
- **Scope**: avaliacao, frontend-web
- **Date**: 2026-09-27
- **Status**: active

### AD-003
- **Decision**: Os áudios ficam em disco local, num diretório configurável, atrás de uma porta de armazenamento; não expiram.
- **Reason**: Decisão do usuário (SDD §24 item 6). A porta permite trocar para armazenamento de objetos (S3) sem mudar o domínio.
- **Trade-off**: Sem retenção automática, o disco cresce sem limite; é preciso fazer backup do diretório separado do banco.
- **Scope**: audio-avaliacao
- **Date**: 2026-09-27
- **Status**: active

### AD-004
- **Decision**: Um aluno pode ter várias avaliações FINALIZADAS do mesmo tipo, no mesmo ciclo e no mesmo ano letivo; histórico e evolução usam a mais recente (maior `finalizado_em`).
- **Reason**: Decisão do usuário (SDD §24 item 7).
- **Trade-off**: As consultas de evolução precisam escolher a "última por grupo"; as avaliações anteriores continuam no histórico, mas não contam na evolução.
- **Scope**: avaliacao, historico-evolucao
- **Date**: 2026-09-27
- **Status**: active

### AD-005
- **Decision**: O aluno tem identidade estável (`aluno`) e um vínculo anual (`matricula`: ano letivo, turma, série, professor, ano finalizado). Cada avaliação guarda uma cópia (snapshot) de turma, série e professor do momento em que foi criada.
- **Reason**: O DDL do SDD §19 prende o aluno a um único ano letivo, o que impede a comparação entre anos (RF014). As cópias garantem o RNF005: trocar professor ou turma não altera o histórico.
- **Trade-off**: Desvio consciente do DDL do SDD, com uma tabela a mais e colunas duplicadas em `avaliacao`.
- **Scope**: cadastros-base, avaliacao, historico-evolucao
- **Date**: 2026-09-27
- **Status**: active

### AD-006
- **Decision**: O backend é uma API REST Spring Boot em `/api/v1`, com erros no formato RFC 7807 (ProblemDetail). O frontend é React + TypeScript (Vite) no diretório `frontend/` do mesmo repositório.
- **Reason**: O usuário pediu backend + frontend; o SDD §23 sugere "React ou Angular", e React foi escolhido por aparecer primeiro e por ser mais simples de usar com a MediaRecorder API.
- **Trade-off**: Monorepo com duas toolchains (Maven + npm).
- **Scope**: todas as features
- **Date**: 2026-09-27
- **Status**: active

### AD-007
- **Decision**: O build falha se a cobertura de linhas ficar abaixo de 85% (JaCoCo no backend, Vitest no frontend). Os testes de integração usam MySQL real via Testcontainers.
- **Reason**: SDD §27 e RNF001 (MySQL).
- **Trade-off**: Os testes de integração precisam de Docker.
- **Scope**: todas as features
- **Date**: 2026-09-27
- **Status**: active

### AD-008
- **Decision**: `BusinessException` ganha um construtor `(HttpStatus status, String code, String message, Map<String, Object> details)`; `GlobalExceptionHandler` copia cada entrada de `details` para o `ProblemDetail` como propriedade extra, além do `code`. O construtor antigo (sem `details`) continua existindo.
- **Reason**: `banco-palavras` (AC `NAO_CANONICA_PROIBIDA_1_ANO`) precisa devolver, além do `code`, as posições dos itens inválidos - dado estruturado que um `code`+`message` simples não carrega.
- **Trade-off**: `common.error` passa a ter duas formas de construir o erro; features futuras devem preferir `details` só quando o AC exigir dado estruturado extra, não como padrão.
- **Scope**: common.error (todas as features que usam `BusinessException`)
- **Date**: 2026-09-28
- **Status**: active

### AD-009
- **Decision**: O frontend (`frontend/`) usa TanStack Query para estado de servidor e Context API só para sessão (token/perfil/professorId) - sem Redux/Zustand/lib de formulário. Em desenvolvimento, o Vite faz proxy de `/api` para o backend em vez de configurar CORS no `SecurityConfig`.
- **Reason**: Decisão do usuário (pergunta de arquitetura, feature `frontend-web`). TanStack Query cobre cache/retry que os requisitos de sincronização (FE-15, 409) e retry de áudio (FE-20/21) já pedem; Context é suficiente para o escopo de sessão; o proxy do Vite evita tocar num arquivo de segurança do backend já fechado (`historico-evolucao`/`avaliacao` Verifier PASS) só por causa do frontend.
- **Trade-off**: Sem uma lib de formulário, telas de cadastro (P2) com muitos campos ficam mais verbosas; se o volume de formulários crescer muito, reconsiderar react-hook-form. Deploy de produção do frontend (origem final, CORS real) fica em aberto - fora do escopo de `frontend-web`.
- **Scope**: frontend-web (e qualquer feature futura de frontend)
- **Date**: 2026-09-29
- **Status**: active

## Handoff

- **Feature**: `cadastros-base` - **Done**. `autenticacao-perfis` - **Done**. `banco-palavras` - **Done**. `regras-classificacao` - **Done**. `audio-avaliacao` - **Done**. `avaliacao` - **Done**. `historico-evolucao` - **Done**. Todo o backend planejado está fechado. `frontend-web` - **In Progress**: Specify (spec.md já existia no esqueleto inicial do repo, closure gate confirmado), Discuss (context.md criado 2026-09-29, 3 decisões: arquitetura TanStack Query+Context, cálculo client-side de "ciclo atual", acesso de evolução por perfil), Design (design.md Approved) e Tasks (tasks.md Approved, 36 tasks T1-T36, `validate_tasks.py` 0 erros) concluídos. Execute ainda não começou.
- **Phase / Task**: `frontend-web` pronta para Execute a partir de `T1` (Fase 1: Fundação). Nenhuma task `[x]` ainda.
- **Completed**: `cadastros-base` (29/29 tasks, Verifier PASS). `autenticacao-perfis` (17/17 tasks, Verifier PASS - `.specs/features/autenticacao-perfis/validation.md`). `banco-palavras` (15/15 tasks, Verifier PASS - `.specs/features/banco-palavras/validation.md`; PAL-06 deferido para `avaliacao`, AC já no spec de lá). `regras-classificacao` (14/14 tasks, Verifier PASS - `.specs/features/regras-classificacao/validation.md`; REG-14 deferido para `avaliacao`, AC já no spec de lá). `audio-avaliacao` (3/3 tasks, Verifier PASS - `.specs/features/audio-avaliacao/validation.md`; deferiu o endpoint HTTP e a tabela `avaliacao_audio` para `avaliacao`, corrigido no Design de lá - AVA-27..AVA-32). `avaliacao` (27/27 tasks, executadas em 4 batches de sub-agentes + 1 commit de fix pós-Verifier `02ddaf3`, 543 testes/96.79% cobertura, Verifier PASS - `.specs/features/avaliacao/validation.md`; entrega o `AvaliacaoService` único (state machine sem State pattern, decisão aprovada pelo usuário), as tabelas `avaliacao`/`avaliacao_palavra`/`avaliacao_auditoria`/`avaliacao_audio` - V9 migration -, o adapter real `HistoricoAvaliacaoAdapter` (`@Primary`, substitui o stub de `cadastros-base`) e o primeiro `@Scheduled` do projeto (finalização de avaliações inativas há 24h); lições L-028/L-029 sobre `noRollbackFor` e finalização preguiçosa em `.specs/LESSONS.md`). `historico-evolucao` (8/8 tasks, executadas inline (feature ≤8 tasks, sem sub-agentes) + 1 commit de fix pós-Verifier `5cec54a`, 614 testes/98.03% cobertura, Verifier PASS - `.specs/features/historico-evolucao/validation.md`; entrega os 3 endpoints somente leitura de `/alunos/{alunoId}` - `historico-avaliacoes`, `evolucao-ciclos`, `evolucao-anos` -, sem tabela nova; lições L-030..L-033 sobre teste de ordenação em grupo com empate, validação de ids fora do domínio, definição de "período consecutivo" e projeção de coluna única via `@Query` em `.specs/LESSONS.md`; nota não-bloqueante: `spec.md`'s Mapping note diverge da rotulagem HIST- usada em `tasks.md`/javadocs para a história "Comparação anual" - ver `validation.md`, "Traceability observation").
- **In-progress** (file:line): none - Planning concluído para `frontend-web`; nenhum arquivo de código escrito ainda (`frontend/` não existe).
- **Next step**: Executar `frontend-web` a partir de `tasks.md` (T1). 36 tasks → oferecer sub-agentes em batches (~7 tasks/batch, fases inteiras) antes de começar, conforme Sub-Agent Delegation da skill. Follow-ups menores e não bloqueantes, ainda pendentes de features já fechadas: (1) de `autenticacao-perfis` - log de login sem IP; promover o AUTH-08 a AC numerado em `.specs/features/avaliacao/spec.md` (ainda não feito - a versão atual do spec cita "AUTH-08"/"AUTH-09" só em prosa, não na tabela de traceability); (2) de `banco-palavras` - dois testes opcionais de 1 linha, ver `.specs/features/banco-palavras/validation.md` e lições L-005/L-019 em `.specs/LESSONS.md`; (3) de `avaliacao` - `criar` aceita uma `lista_palavras` inativa como fonte de conteúdo, spec.md não resolve esse caso (ver `.specs/features/avaliacao/context.md`, Deferred Ideas); (4) de `historico-evolucao` - `spec.md`'s AC3 da história "Comparar desempenho entre anos letivos" não define o que fazer quando um ano fica sem avaliação no meio de uma sequência (spec-precision note, ver `validation.md`); a Mapping note de `spec.md` e a rotulagem HIST- em `tasks.md`/código para essa mesma história estão descasadas (não bloqueia comportamento, só documentação).
- **Blockers**: none
- **Uncommitted files**: none
- **Branch**: `master` (repo git local, sem remoto)

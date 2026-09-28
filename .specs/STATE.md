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

## Handoff

- **Feature**: `cadastros-base` - **Done**. `autenticacao-perfis` - **Done** (Verifier PASS na iteração 2/3, 2026-09-28). Próxima feature: `banco-palavras` (ainda não iniciada - só tem `spec.md`).
- **Phase / Task**: `autenticacao-perfis` fechada - todas as 17 tasks `[x]`, `validate_state.py autenticacao-perfis` exit 0. `banco-palavras` não tem Design/Tasks ainda.
- **Completed**: `cadastros-base` (29/29 tasks, Verifier PASS). `autenticacao-perfis` (17/17 tasks + 1 commit de fix pós-Verifier `442a190`, 143 testes, Verifier PASS - `.specs/features/autenticacao-perfis/validation.md`). Ordem de dependência do restante do backend: `autenticacao-perfis` → `banco-palavras` → `regras-classificacao` → `audio-avaliacao` → `avaliacao` → `historico-evolucao`; `frontend-web` por último (decisão do usuário, 2026-09-27).
- **In-progress** (file:line): none
- **Next step**: Iniciar `banco-palavras` pela skill `tlc-spec-driven` - `spec.md` já existe, falta Design/Tasks/Execute. Follow-ups menores e não bloqueantes registrados no validation.md de `autenticacao-perfis` (opcionais, ver lições L-005..L-010 em `.specs/LESSONS.md`): (1) log de login sem IP; (2) promover o AUTH-08 a AC numerado em `.specs/features/avaliacao/spec.md` quando essa feature chegar ao Design.
- **Blockers**: none
- **Uncommitted files**: none
- **Branch**: `master` (repo git local, sem remoto)

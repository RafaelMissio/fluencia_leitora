# Frontend Web Context

**Gathered:** 2026-09-29
**Spec:** `.specs/features/frontend-web/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Interface web (React + TypeScript + Vite, `frontend/`) que consome as APIs REST já implementadas (`autenticacao-perfis`, `cadastros-base`, `regras-classificacao`, `banco-palavras`, `avaliacao`, `audio-avaliacao`, `historico-evolucao`). P1 entrega o fluxo do professor de ponta a ponta (login → busca → configurar → executar com cronômetro/gravação → marcar palavras → resultado/áudio → histórico e evolução) e a navegação do coordenador. P2 entrega as telas de cadastro do coordenador.

---

## Implementation Decisions

### Arquitetura de estado/dados

- TanStack Query para todo estado de servidor (cache, loading/error, retry). O `retry`/`retryDelay` do próprio React Query cobre o backoff de reenvio de comandos; o envio de áudio (FE-20/FE-21) usa sua própria lógica de tentativas (1s/2s/4s) porque não é uma query/mutation comum — é um `fetch` de `multipart/form-data` acompanhado de um estado de "áudio pendente em memória".
- `Context` API só para sessão (token, perfil, `professorId`) — nada de Redux/Zustand.
- Cronômetro e gravação (MediaRecorder) ficam em estado local do hook da tela de execução (`useReducer`), não em estado global.
- Sem lib de formulário (react-hook-form etc.): inputs controlados simples; erros 422 (`{code, errors: [{field, message}]}`) mapeados por `field` para o campo correspondente.
- Testes: Vitest + React Testing Library (unit/componente, gate ≥85% linhas — AD-007), Playwright para o E2E do SDD §20 exigido no Success Criteria do spec (microfone falso via `--use-fake-device-for-media-stream`).

### "Ciclo atual" (resumo do aluno e valor padrão do formulário de configuração)

O backend não tem o conceito de "ciclo em vigor por data" — `Ciclo` é só um domínio fixo (ENTRADA/ACOMPANHAMENTO/SAÍDA), sem relação com datas. Decisão: o frontend deriva o "ciclo atual" buscando as avaliações `FINALIZADA` do aluno no ano `ATIVO` (via `GET /alunos/{id}/historico-avaliacoes`, sem filtro de tipo, olhando os `ciclo` presentes) e escolhe o primeiro ciclo, na ordem ENTRADA → ACOMPANHAMENTO → SAÍDA, que ainda não aparece em nenhuma avaliação finalizada. Se os três já aparecem, usa SAÍDA. Essa é a mesma regra usada tanto no resumo (exibição) quanto no valor padrão do campo `ciclo` do formulário de configuração (spec.md, P1 "Configurar a avaliação", AC1).

### Acesso a evolução (resumo do PROFESSOR + abas do histórico)

`GET /alunos/{id}/evolucao-ciclos` e `GET /alunos/{id}/evolucao-anos` (feature `historico-evolucao`, já com Verifier PASS) são `hasRole('COORDENADOR')` — PROFESSOR recebe 403. Só `GET /alunos/{id}/historico-avaliacoes` é aberto a PROFESSOR e COORDENADOR. Decisão: o frontend segue o backend como fonte da verdade, sem reabrir essa feature.

- As abas "Evolução por ciclo" e "Comparação anual" da tela de Histórico e evolução (spec.md, P1) ficam visíveis só para COORDENADOR; PROFESSOR vê apenas a aba "Histórico".
- O campo "evolução no ano" do resumo do aluno (spec.md, P1 "Buscar aluno e ver resumo", AC2) é reinterpretado, para o perfil PROFESSOR, como uma comparação simples calculada no cliente: para o ciclo mais recente com 2 avaliações `FINALIZADA` do mesmo tipo de leitura (a mais recente e a anterior a ela, dentro dos dados já retornados por `historico-avaliacoes`), mostra a variação de `quantidadeCorretas` (mesma fórmula de HIST-14/17/18/19, replicada no cliente só para esse dado — não chama `evolucao-ciclos`). Quando não há par de avaliações do mesmo ciclo/tipo, mostra "—". Para COORDENADOR, o resumo pode usar `evolucao-ciclos` diretamente (dado já autorizado).
- Esta reinterpretação relaxa a leitura literal de spec.md P1 "Buscar aluno e ver resumo" AC2 (que não distinguia por perfil); ela é registrada aqui como a leitura vinculante para o Design e as Tasks.

### CORS / integração dev

Sem `CorsConfigurationSource` no backend hoje (`SecurityConfig.java` não configura CORS). Agent's Discretion: em desenvolvimento, o Vite (`vite.config.ts`) usa `server.proxy` para encaminhar `/api` ao backend (`http://localhost:8080`), tornando as chamadas same-origin do ponto de vista do navegador — não é necessário mexer no `SecurityConfig` do backend. Empacotamento/deploy de produção do frontend (onde ele é servido a partir de qual origem) não está no escopo desta spec (SDD não define topologia de deploy); fica como assunção registrada em spec.md, não uma tarefa desta feature.

### Agent's Discretion

- Estrutura de pastas do `frontend/` (ver design.md, Architecture Overview).
- Nomes de componentes, hooks e arquivos.
- Biblioteca de ícones/estilo visual concreto (cores/ícones do spec — verde/vermelho/cinza — são obrigatórios; o resto do design visual é discrição do agente, dentro do WCAG 1.4.1 exigido).

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma - as três áreas de maior risco (arquitetura de estado, "ciclo atual", acesso a evolução) foram discutidas acima. As assunções já registradas em `spec.md` (Assumptions & Open Questions) continuam válidas e não foram reabertas.

---

## Specific References

Nenhuma referência visual específica foi trazida pelo usuário nesta rodada - o layout segue os requisitos funcionais do spec (cores/ícones de status, tabelas para histórico/evolução) com liberdade de composição visual.

---

## Deferred Ideas

None - discussion stayed within feature scope.

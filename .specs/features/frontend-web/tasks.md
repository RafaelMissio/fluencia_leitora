# Frontend Web Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/frontend-web/design.md`
**Context**: `.specs/features/frontend-web/context.md`
**Status**: Approved

---

## Test Coverage Matrix

> Gerada a partir do spec.md (Success Criteria: "Cobertura de linhas no Vitest ≥ 85%", "Teste E2E (Playwright com microfone falso) cobre o fluxo completo do SDD §20"), de `design.md` (Tech Decisions: Vitest + React Testing Library, Playwright) e de `AD-007`/`AD-009` (`.specs/STATE.md`). `frontend/` é greenfield - não há testes existentes para amostrar; os comandos abaixo são definidos pela T1 (scaffold), que cria os scripts `npm run test`/`test:e2e`/`build`/`lint` referenciados aqui.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Hooks de dados (TanStack Query: busca, resumo, listas, avaliação, histórico/evolução) | unit | Todos os branches (loading/sucesso/erro/filtros), 1:1 com os FE-xx da story que o hook serve | `src/**/*.test.ts` co-localizado ao hook | `npm run test` |
| Helpers puros (`calcularCicloAtual`, `calcularEvolucaoCliente`, `pickSupportedMimeType`, mapeamento de erro) | unit | Todos os branches, incluindo casos-limite (0 ciclos preenchidos, 3 ciclos preenchidos, MIME não suportado) | `src/**/*.test.ts` | `npm run test` |
| `apiClient` (parse de erro RFC 7807, interceptor 401) | unit | Happy path + cada forma de erro (422 com `errors[]`, 401, 429 com `Retry-After`, 409, falha de rede) | `src/api/client.test.ts` | `npm run test` |
| Componentes com lógica de interação (formulários, `GradePalavras`, `CronometroDisplay`, páginas com estado) | unit (component, React Testing Library) | Cada Acceptance Criteria do spec ligado ao componente + cada edge case listado (SDD Edge Cases) | `src/**/*.test.tsx` co-localizado | `npm run test` |
| Páginas de composição simples (CRUDs finos do coordenador, P2) | unit (component, smoke) | Renderiza, dispara a mutation correta no submit, mostra "Salvo com sucesso"/erro 409/422 - não repete o que já está coberto no hook | `src/features/cadastros/**/*.test.tsx` | `npm run test` |
| Fluxo completo do professor (SDD §20) | e2e | login → busca → configuração → iniciar → marcar → tempo zerado → resultado → áudio, com microfone falso (`--use-fake-device-for-media-stream`) | `e2e/*.spec.ts` | `npm run test:e2e` |
| Config/tooling (`vite.config.ts`, `tsconfig`, `eslint config`, `types.ts`) | none | - (build gate only) | - | `npm run build` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefas que só adicionam hooks/helpers/testes unitários, sem novo componente visual | `npm run test` |
| Full | Tarefas que adicionam/alteram componente ou página (RTL) | `npm run lint && npm run test` |
| Build | Fechamento de fase, ou tarefa que só mexe em config/scaffold | `npm run build && npm run lint && npm run test` |
| E2E | Tarefa da Fase 9 (`T29`) | `npm run build && npm run test:e2e` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Fundação
```
T1 → T2 → T3 → T4 → T5 → T6
T1 → T3
```

### Phase 2: Busca e resumo do aluno
```
T2 → T7
T3 → T7
T7 → T8
T2 → T9
T9 → T10
T8 → T11
T10 → T11
```

### Phase 3: Configurar avaliação
```
T2 → T12
T3 → T12
T2 → T13
T3 → T13
T12 → T14
T13 → T14
T9 → T14
```

### Phase 4: Execução (cronômetro + gravação)
```
T1 → T15
T15 → T16
T16 → T17
T17 → T18
T16 → T19
```

### Phase 5: Marcar palavras
```
T2 → T20
```

### Phase 6: Resultado e áudio
```
T3 → T21
T21 → T22
```

### Phase 7: Tela de execução (composição)
```
T16 → T23
T17 → T23
T18 → T23
T19 → T23
T20 → T23
```

### Phase 8: Histórico e evolução
```
T2 → T24
T3 → T24
T24 → T25
T2 → T26
T3 → T26
T2 → T27
T3 → T27
T25 → T28
T26 → T28
T27 → T28
```

### Phase 8b: Fechamento do shell (composição raiz + logout)

> Fase adicionada após o Batch 1 (Phase 1) reportar, no fechamento, que nenhuma task de T1-T36 faz o boot real da app (`main.tsx`/`App.tsx` seguem o boilerplate do Vite) nem oferece um controle de logout visível. Ambos são pré-requisitos reais para o E2E (T29) e para o uso manual da app; nenhum dos dois amplia o escopo do spec - T37 é infraestrutura implícita em qualquer feature de frontend, T38 é um complemento de 1 linha à história "Login e navegação por perfil" (P1) sem AC numerado próprio.

```
T6 → T37
T11 → T37
T14 → T37
T22 → T37
T23 → T37
T28 → T37
T5 → T38
```

### Phase 9: E2E do fluxo do professor (P1)
```
T37 → T29
```

### Phase 10: Telas de cadastro do coordenador (P2)
```
T5 → T30
T5 → T31
T5 → T32
T5 → T33
T5 → T34
T5 → T35
T5 → T36
```
(As 7 tarefas de Phase 10 são independentes entre si - cada uma cria e conecta sua própria tela; todas rodam nesta fase, sem ordem obrigatória entre elas além da execução sequencial padrão.)

---

## Task Breakdown

### T1: Scaffold do projeto `frontend/` (Vite + React + TS + tooling)

**What**: Criar `frontend/` com Vite (`react-ts` template), instalar `react-router-dom`, `@tanstack/react-query`; configurar ESLint+Prettier, Vitest + React Testing Library (`@testing-library/react`, `@testing-library/user-event`, `jsdom`), Playwright (`@playwright/test`, `npx playwright install --with-deps`); criar `vite.config.ts` com `server.proxy['/api'] → http://localhost:8080` (design.md, Tech Decisions); criar os scripts `dev`, `build`, `lint`, `test`, `test:e2e` em `package.json`; criar a árvore de pastas vazia de `src/` (design.md, Architecture Overview): `app/`, `auth/`, `api/`, `features/{alunos,avaliacoes,historico,cadastros}/`, `components/`, `layout/`, `media/`, `test/`, e `e2e/` na raiz de `frontend/`.
**Where**: `frontend/` (new directory tree)
**Depends on**: None
**Reuses**: N/A (greenfield)
**Requirement**: Infra (habilita todas as FE-xx)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `frontend/package.json` tem os scripts `dev`, `build`, `lint`, `test`, `test:e2e`
- [x] `vite.config.ts` proxeia `/api` para `http://localhost:8080` em dev
- [x] `npm run build` compila sem erros (projeto vazio, só o boilerplate do template + pastas)
- [x] `npm run lint` roda sem erros
- [x] `npm run test` roda (mesmo sem nenhum teste ainda, o comando existe e retorna sucesso)
- [x] Árvore de pastas de `src/` criada conforme design.md
- [x] Gate check passes: `npm run build && npm run lint && npm run test`

**Tests**: none
**Gate**: build

**Commit**: `chore(frontend-web): scaffold Vite + React + TS project with tooling`

---

### T2: `api/types.ts` - tipos TS espelhando os DTOs do backend

**What**: Criar todos os tipos definidos em design.md (Data Models): `Perfil`, `LoginResponse`, `TipoLeituraCodigo`, `Ciclo`, `AlunoBuscaItem`, `StatusAvaliacao`, `StatusPalavra`, `PalavraAvaliacao`, `AvaliacaoResponse`, `NovaAvaliacaoRequest`, `HistoricoAvaliacaoItem`, `ResultadoCiclo`, `EvolucaoCiclosResponse`, `EvolucaoValor`, `CicloAnual`, `EvolucaoAnualLinha`, `EvolucaoAnualResponse`, `Page<T>`, `ApiError`.
**Where**: `frontend/src/api/types.ts` (new)
**Depends on**: T1
**Reuses**: N/A (espelha os DTOs Java listados em design.md, Integration Points)
**Requirement**: Infra (habilita todas as FE-xx que trocam dados com a API)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Todos os tipos de design.md (Data Models) existem em `types.ts`, com os mesmos nomes de campo do backend (inclusive em português)
- [x] `npm run build` compila sem erro de tipo
- [x] Gate check passes: `npm run build`

**Tests**: none
**Gate**: build

**Commit**: `feat(frontend-web): add TS types mirroring backend DTOs`

---

### T3: `api/client.ts` - `apiClient` (fetch, erro RFC 7807, interceptor 401)

**What**: Implementar `request<T>(path, init?)` (injeta `Authorization: Bearer <token>` lido de um getter passado por `AuthContext` via um "token provider" registrado em runtime; monta URL relativa `/api/v1${path}` para usar o proxy do Vite; em resposta não-2xx, faz parse do `ProblemDetail` e lança `ApiError` com `status`, `code`, `detail`, `errors?`; em `status === 401`, chama um callback de logout registrado, sem lançar de volta silenciosamente - o chamador ainda recebe o `ApiError` para poder decidir a UI, exceto na tela de login, que trata 401 separadamente por não vir de uma sessão) e `uploadAudio(avaliacaoId, blob, mimeType)` (via `FormData`, `POST /api/v1/avaliacoes/{id}/audio`).
**Where**: `frontend/src/api/client.ts` (new)
**Depends on**: T1, T2
**Reuses**: `ApiError` (T2)
**Requirement**: FE-03 (base do interceptor 401)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `request` injeta o header `Authorization` quando há token registrado, e não injeta quando não há (rota de login)
- [x] Resposta 2xx com corpo JSON retorna o objeto tipado; 2xx sem corpo (204) retorna `undefined`
- [x] Resposta não-2xx lança `ApiError` com `status`/`code`/`detail`/`errors` extraídos do `ProblemDetail` (`{code, errors: [{field, message}]}` para 422; `{code, detail}` para os demais)
- [x] Resposta `401` dispara o callback de logout registrado antes de rejeitar a promise
- [x] `uploadAudio` envia `multipart/form-data` com o campo `audio` e retorna sem erro em `201`
- [x] Novos testes unitários em `client.test.ts`: header presente/ausente, parse de 422 com `errors[]`, parse de 401/409/429 (com `Retry-After`), callback de 401 chamado, falha de rede (fetch rejeita) vira `ApiError` com `status: 0`, `uploadAudio` monta `FormData` corretamente
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 8 testes novos (13 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add apiClient with RFC 7807 error parsing and 401 interceptor`

---

### T4: `auth/AuthContext.tsx` + `useAuth`

**What**: `AuthProvider` guarda `{ token, perfil, professorId }` em `sessionStorage` (chave única, ex. `fluencia.session`) e reidrata no boot; expõe `login(email, senha): Promise<void>` (chama `POST /auth/login` via `apiClient`, sem token no header, e ao suceder grava a sessão e registra o token no `apiClient`) e `logout(): void` (limpa `sessionStorage`, desregistra o token, e é o callback registrado no interceptor 401 de T3).
**Where**: `frontend/src/auth/AuthContext.tsx` (new)
**Depends on**: T3
**Reuses**: `apiClient` (T3), `LoginResponse`/`LoginRequest`-shaped call (T2)
**Requirement**: FE-01, FE-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `login` bem-sucedido grava `token`/`perfil`/`professorId` em `sessionStorage` e no estado do contexto
- [x] Ao montar, se `sessionStorage` tem uma sessão válida, `isAuthenticated` já começa `true` (sem esperar nenhuma chamada)
- [x] `logout` limpa `sessionStorage` e zera o estado
- [x] `logout` é o callback chamado pelo `apiClient` em qualquer 401 (FE-03)
- [x] Novos testes unitários em `AuthContext.test.tsx`: login grava sessão, reidratação no boot, logout limpa tudo, 401 via `apiClient` dispara logout
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 4 testes novos

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add AuthContext with sessionStorage persistence and 401 logout`

---

### T5: `app/router.tsx` (`ProtectedRoute`, `RoleGate`) + `layout/AppLayout.tsx`

**What**: `ProtectedRoute` redireciona para `/login` quando `!isAuthenticated`, preservando a rota de origem (`state.from`) para retorno pós-login (FE-03 AC4). `RoleGate` renderiza os filhos só quando `perfil` está na lista permitida, senão redireciona (ou 404 visual) - usado para restringir rotas/abas ao COORDENADOR (context.md). `AppLayout` mostra o menu lateral: PROFESSOR vê Avaliar, Meus alunos, Histórico; COORDENADOR vê Alunos, Turmas, Professores, Anos letivos, Regras de classificação, Listas de palavras, Usuários, Avaliações, e nunca vê Avaliar (spec.md, P1 "Login e navegação", AC5/AC6). O `<Routes>` inicial só tem `/login` (pública) e um layout raiz protegido; as páginas de features adicionam suas próprias `<Route>` filhas nas tasks seguintes (T8, T14, T22, T23, T28, T30..T36).
**Where**: `frontend/src/app/router.tsx`, `frontend/src/layout/AppLayout.tsx` (new)
**Depends on**: T4
**Reuses**: `useAuth` (T4)
**Requirement**: FE-01, FE-04

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Rota não autenticada acessando qualquer rota protegida é redirecionada para `/login`, e após login volta para a rota original
- [x] `AppLayout` mostra exatamente os 3 itens do PROFESSOR e exatamente os 8 itens do COORDENADOR (sem "Avaliar" para COORDENADOR)
- [x] `RoleGate` esconde/bloqueia conteúdo fora do perfil permitido
- [x] Novos testes de componente em `AppLayout.test.tsx`/`router.test.tsx`: menu por perfil, redirecionamento não autenticado, `RoleGate` bloqueando PROFESSOR de conteúdo COORDENADOR
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 5 testes novos (6 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add protected router, role gate and profile-based layout menu`

---

### T6: `auth/LoginPage.tsx`

**What**: Formulário de e-mail/senha; ao enviar, chama `useAuth().login`; em sucesso navega para `/avaliar` (PROFESSOR) ou `/alunos` (COORDENADOR) - ou para a rota preservada por `ProtectedRoute`, se houver (spec.md P1 "Login", AC1). Trata `ApiError` do login separadamente do interceptor 401 genérico: `status === 401` → "E-mail ou senha inválidos", sem limpar o campo de e-mail (AC2); `status === 429` → "Conta bloqueada. Tente novamente em N minutos", com N calculado a partir do header `Retry-After` (AC3, em segundos, convertido para minutos arredondando para cima).
**Where**: `frontend/src/auth/LoginPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/login`)
**Depends on**: T5
**Reuses**: `useAuth` (T4), `apiClient` `ApiError` (T3)
**Requirement**: FE-01, FE-02

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Login válido de PROFESSOR abre `/avaliar`; de COORDENADOR abre `/alunos`
- [x] Login válido com rota de retorno preservada abre essa rota em vez do padrão
- [x] 401 mostra "E-mail ou senha inválidos" e mantém o e-mail digitado
- [x] 429 mostra "Conta bloqueada. Tente novamente em N minutos" com N derivado de `Retry-After`
- [x] Novos testes de componente em `LoginPage.test.tsx`: os 4 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 4 testes novos (5 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add LoginPage with 401/429 handling and profile redirect`

---

### T7: `features/alunos/useAlunoBusca.ts`

**What**: Hook TanStack Query que chama `GET /alunos?nome=&page=`, com o termo de busca *debounced* em 300ms (spec.md P1 "Buscar aluno", AC1) e `enabled: nome.trim().length >= 2` (query não dispara com menos de 2 caracteres).
**Where**: `frontend/src/features/alunos/useAlunoBusca.ts` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`, `AlunoBuscaItem`/`Page<T>` (T2)
**Requirement**: FE-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Digitar menos de 2 caracteres não dispara a chamada
- [x] A chamada só é feita 300ms após a última tecla (debounce testado com fake timers)
- [x] Resultado tipado como `Page<AlunoBuscaItem>`
- [x] Novos testes unitários em `useAlunoBusca.test.ts`: menos de 2 chars não busca, debounce de 300ms, resultado propagado
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 3 testes novos (3 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useAlunoBusca hook with 300ms debounce`

---

### T8: `features/alunos/AlunoBuscaPage.tsx`

**What**: Campo de busca + lista de resultados (nome, turma, série); seleção de um aluno guarda o `alunoId` selecionado em estado local da página (o painel de resumo é adicionado em T11); mensagem "Nenhum aluno encontrado" quando a busca não retorna nada (AC3).
**Where**: `frontend/src/features/alunos/AlunoBuscaPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/alunos`)
**Depends on**: T7
**Reuses**: `useAlunoBusca` (T7)
**Requirement**: FE-05

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Digitar "jo" lista os alunos com "jo" no nome, mostrando nome/turma/série
- [x] Selecionar um item marca esse aluno como selecionado
- [x] Busca sem resultado mostra "Nenhum aluno encontrado"
- [x] Rota `/alunos` protegida renderiza a página
- [x] Novos testes de componente em `AlunoBuscaPage.test.tsx`: os 3 pontos funcionais acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos (4 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add AlunoBuscaPage with search and empty state`

---

### T9: `features/alunos/cicloAtual.ts` - helper `calcularCicloAtual`

**What**: Função pura `calcularCicloAtual(itens: HistoricoAvaliacaoItem[]): 'ENTRADA' | 'ACOMPANHAMENTO' | 'SAIDA'` que aplica a regra de context.md: primeiro ciclo, na ordem ENTRADA → ACOMPANHAMENTO → SAÍDA, que não aparece em nenhum item da lista (avaliações `FINALIZADA` do ano ATIVO); se os três aparecem, retorna `SAIDA`.
**Where**: `frontend/src/features/alunos/cicloAtual.ts` (new)
**Depends on**: T2
**Reuses**: `HistoricoAvaliacaoItem` (T2)
**Requirement**: Suporte a FE-06, FE-07 (context.md)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Lista vazia → `ENTRADA`
- [x] Só ENTRADA presente → `ACOMPANHAMENTO`
- [x] ENTRADA + ACOMPANHAMENTO presentes → `SAIDA`
- [x] Os 3 presentes → `SAIDA`
- [x] Novos testes unitários em `cicloAtual.test.ts`: os 4 casos acima
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 4 testes novos (4 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add calcularCicloAtual helper`

---

### T10: `features/alunos/useAlunoResumo.ts`

**What**: Hook que compõe o resumo (spec.md P1 "Buscar aluno", AC2 + context.md): busca `GET /alunos/{id}` (dados cadastrais), `GET /alunos/{id}/historico-avaliacoes?page=0` (para `cicloAtual` via T9, "última classificação" = item mais recente, e as 5 avaliações mais recentes = os 5 primeiros itens da página), e, só quando `perfil === 'COORDENADOR'`, `GET /alunos/{id}/evolucao-ciclos` (evolução real); para PROFESSOR, calcula a evolução no cliente (context.md: compara `quantidadeCorretas` das 2 avaliações mais recentes do mesmo ciclo+tipo dentro do histórico já buscado; "—" se não houver par).
**Where**: `frontend/src/features/alunos/useAlunoResumo.ts` (new)
**Depends on**: T9
**Reuses**: `calcularCicloAtual` (T9), `apiClient`, tipos de T2
**Requirement**: FE-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Retorna `{ aluno, cicloAtual, ultimasAvaliacoes (≤5), ultimaClassificacao, evolucao, isLoading, error }`
- [x] PROFESSOR nunca chama `evolucao-ciclos` (evita 403); evolução vem do cálculo no cliente
- [x] COORDENADOR usa `evolucao-ciclos` diretamente
- [x] Evolução do PROFESSOR sem par de avaliações do mesmo ciclo/tipo retorna `"—"` (sem erro) - o hook retorna `null` (spec-precision gap: a formatação exata "—" é responsabilidade da apresentação, feita em T11/`ResumoAlunoPanel`)
- [x] Novos testes unitários em `useAlunoResumo.test.ts`: composição para PROFESSOR, composição para COORDENADOR, evolução sem par, 5 mais recentes cortadas corretamente
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 5 testes novos (5 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useAlunoResumo composing summary from existing endpoints`

---

### T11: `features/alunos/ResumoAlunoPanel.tsx`

**What**: Renderiza o painel de resumo (nome, turma, professor, ano letivo, série, ciclo atual, última classificação, as 5 avaliações mais recentes, evolução no ano) usando `useAlunoResumo`; embutido em `AlunoBuscaPage` (T8) quando há um aluno selecionado, com um botão/link "Configurar avaliação" que navega para `/avaliacoes/nova?alunoId=` (rota criada em T14).
**Where**: `frontend/src/features/alunos/ResumoAlunoPanel.tsx` (new), `frontend/src/features/alunos/AlunoBuscaPage.tsx` (modify: embute o painel)
**Depends on**: T8, T10
**Reuses**: `useAlunoResumo` (T10)
**Requirement**: FE-06

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Selecionar "João" na busca mostra o painel com todos os campos de AC2 preenchidos, incluindo o ciclo atual
- [x] Painel mostra as 5 avaliações mais recentes (ou menos, se o aluno tiver menos)
- [x] Painel mostra "—" quando a evolução não tem par de comparação (PROFESSOR)
- [x] Novos testes de componente em `ResumoAlunoPanel.test.tsx`: renderização completa, evolução "—", navegação para configurar avaliação
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos (3 testes; `AlunoBuscaPage.test.tsx` também ajustado para mockar `ResumoAlunoPanel` e manter o isolamento do teste da página)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ResumoAlunoPanel embedded in AlunoBuscaPage`

---

### T12: `features/avaliacoes/useListasPalavras.ts`

**What**: Hook TanStack Query para `GET /listas-palavras?serie=&tipoLeitura=`, usado pelo formulário de configuração para listar as opções de lista de palavras da série do aluno e do tipo de leitura escolhido (spec.md P1 "Configurar avaliação", AC2).
**Where**: `frontend/src/features/avaliacoes/useListasPalavras.ts` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`, `ListaPalavrasResumoResponse`-shaped type (adicionar a `types.ts` nesta task, já que design.md não a detalhou por completo - `{id, nome, quantidadePalavras}`)
**Requirement**: FE-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Chamada só ocorre quando `serie` e `tipoLeitura` estão definidos (`enabled`)
- [x] Resultado tipado `{id, nome, quantidadePalavras}[]`
- [x] Novos testes unitários em `useListasPalavras.test.ts`: chamada com filtros, `enabled` falso sem os 2 parâmetros
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 2 testes novos (3 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useListasPalavras hook`

---

### T13: `features/avaliacoes/useCriarAvaliacao.ts`

**What**: `useMutation` para `POST /avaliacoes`, tipado com `NovaAvaliacaoRequest`/`AvaliacaoResponse`; expõe o `ApiError` (incluindo `errors[]` de 422) para o formulário mapear por campo.
**Where**: `frontend/src/features/avaliacoes/useCriarAvaliacao.ts` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`
**Requirement**: FE-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Sucesso retorna `AvaliacaoResponse` com `status: 'CRIADA'`
- [x] Erro 422 propaga `errors: [{field, message}]` sem perder nenhum item
- [x] Novos testes unitários em `useCriarAvaliacao.test.ts`: sucesso, erro 422 com múltiplos campos
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 2 testes novos (3 testes; o 3º verifica o payload/POST enviado, base causal do teste de sucesso)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useCriarAvaliacao mutation hook`

---

### T14: `features/avaliacoes/ConfigurarAvaliacaoPage.tsx`

**What**: Formulário de configuração (spec.md P1 "Configurar avaliação"): ao abrir, preenche `ciclo` com `calcularCicloAtual` (via o histórico do aluno, T9/T10), `data` com hoje, `tempo` com 60 (AC1); ao escolher o tipo de leitura, lista as listas de palavras da série+tipo (T12) e oferece "Digitar palavras" (ou "Digitar texto" em `TEXTO_CURTO`) (AC2); enquanto digitando palavras, mostra "N palavras (mín. X, máx. Y)" e desabilita "Criar avaliação" fora do intervalo (AC3 - os limites X/Y vêm de `ConfiguracaoAvaliacaoResponse` da série, buscados via um hook simples `useConfiguracaoAvaliacao(serie)` criado nesta mesma task, `GET /anos-letivos/configuracao?serie=` conforme `AnoLetivoController`); erro 422 mostra a mensagem de cada campo ao lado dele (AC4); ao criar com sucesso, navega para `/avaliacoes/{id}/executar` (rota criada em T23) com as palavras em grade, na ordem (AC5).
**Where**: `frontend/src/features/avaliacoes/ConfigurarAvaliacaoPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/avaliacoes/nova`)
**Depends on**: T12, T13, T9
**Reuses**: `useListasPalavras` (T12), `useCriarAvaliacao` (T13), `calcularCicloAtual` (T9)
**Requirement**: FE-07, FE-08, FE-09, FE-10

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Formulário abre com ciclo/data/tempo preenchidos conforme AC1
- [x] Trocar o tipo de leitura atualiza a lista de listas de palavras disponíveis e o rótulo "Digitar texto" aparece só em `TEXTO_CURTO`
- [x] Com 14 palavras numa série de mínimo 15, "Criar avaliação" fica desabilitado; com 15, habilita
- [x] Erro 422 do backend mostra a mensagem ao lado de cada campo citado em `errors[]`
- [x] Criar com sucesso navega para a tela de execução com o id da avaliação criada
- [x] Novos testes de componente em `ConfigurarAvaliacaoPage.test.tsx`: os 5 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 5 testes novos (5 testes)

**SPEC_DEVIATION (endpoint)**: o texto original desta task citava `GET /anos-letivos/configuracao?serie=`, que não existia no backend (bloqueio reportado ao orquestrador). Resolvido pelo backend com `GET /api/v1/anos-letivos/ativo/configuracoes/{serie}` (`hasAnyRole('PROFESSOR','COORDENADOR')`, resolve o ano ATIVO no servidor), commit `72df217` (`cadastros-base`). `useConfiguracaoAvaliacao(serie)` foi implementado consumindo esse endpoint real, dentro do próprio `ConfigurarAvaliacaoPage.tsx` (Where da task só lista esse arquivo).
**Nota (agent's discretion)**: `tipoLeitura` (3 valores fixos) e o mapeamento `cicloAtualCodigo -> cicloId` (via `GET /ciclos`) não têm hook próprio listado na task - implementados inline no mesmo arquivo. Palavras digitadas manualmente são sempre enviadas com `tipoPalavra: 'CANONICA'` (campo opcional no backend, sem AC que exija selecionar o tipo na tela de criação).

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ConfigurarAvaliacaoPage with defaults, word counter and 422 mapping`

---

### T15: `media/recorder.ts`

**What**: `isMediaRecorderSupported(): boolean` (checa `window.MediaRecorder` existe); `pickSupportedMimeType(): string | null` (testa, na ordem, `audio/webm;codecs=opus`, `audio/ogg;codecs=opus`, `audio/mp4` via `MediaRecorder.isTypeSupported`, retorna o primeiro suportado ou `null`); `createRecorder(stream: MediaStream, mimeType: string): RecorderHandle` (`{start(), stop(): Promise<Blob>, pause(), resume()}`, wrapper fino sobre `MediaRecorder` para permitir mock em teste).
**Where**: `frontend/src/media/recorder.ts` (new)
**Depends on**: T1
**Reuses**: N/A
**Requirement**: FE-25, edge case "MIME na ordem X, Y, Z"

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `isMediaRecorderSupported` retorna `false` quando `window.MediaRecorder` é `undefined`
- [x] `pickSupportedMimeType` respeita a ordem `webm/opus` → `ogg/opus` → `mp4`, retornando o primeiro que `isTypeSupported` aceita
- [x] `pickSupportedMimeType` retorna `null` quando nenhum é suportado
- [x] `createRecorder` inicia, pausa, resume e para retornando um `Blob` com o `mimeType` escolhido
- [x] Novos testes unitários em `recorder.test.ts` (com `MediaRecorder`/`isTypeSupported` mockados): os 4 pontos acima
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 5 testes novos (7 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add MediaRecorder wrapper with MIME fallback selection`

---

### T16: `useAvaliacaoExecucao` - estado base + `iniciar`

**What**: Criar o hook com um `useReducer` cobrindo o estado `{status, tempoRestanteMs, gravando, erroMicrofone}` e a função `iniciar()`: primeiro chama `isMediaRecorderSupported` (se `false`, define `erroMicrofone` = "Navegador sem suporte à gravação. Use Chrome, Edge, Firefox ou Safari 17+" e desabilita o botão via estado, sem chamar a API - FE-25); senão pede `getUserMedia({audio: true})` **antes** de chamar `POST /avaliacoes/{id}/iniciar` (FE-11); se negado/sem microfone, define `erroMicrofone` = "Permita o acesso ao microfone para iniciar a avaliação" e a avaliação continua `CRIADA`, sem chamar `iniciar` (FE-12); se liberado, chama `iniciar`, e ao suceder inicia `createRecorder`/`MediaRecorder` e a contagem regressiva (`performance.now()`) ao mesmo tempo (FE-13). No mount, se a avaliação carregada já vier com `status: 'EM_ANDAMENTO'` do servidor (página recarregada), define um estado especial "gravação anterior interrompida" (FE-24, botões tratados em T18).
**Where**: `frontend/src/features/avaliacoes/useAvaliacaoExecucao.ts` (new)
**Depends on**: T15
**Reuses**: `media/recorder.ts` (T15), `apiClient`
**Requirement**: FE-11, FE-12, FE-13, FE-24, FE-25

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Sem suporte a `MediaRecorder` → `erroMicrofone` definido, `iniciar` da API nunca chamado
- [x] Microfone negado → `erroMicrofone` = "Permita o acesso ao microfone para iniciar a avaliação"; `iniciar` da API nunca chamado; status continua `CRIADA`
- [x] Microfone liberado e `iniciar` retorna 200 → gravação e cronômetro começam juntos
- [x] Mount com avaliação já `EM_ANDAMENTO` no servidor → flag de "interrompida" ativa
- [x] Novos testes unitários em `useAvaliacaoExecucao.test.ts` (com `getUserMedia`/`MediaRecorder` mockados): os 4 pontos acima
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 4 testes novos (4 testes)

**Nota (agent's discretion)**: o hook faz `GET /avaliacoes/{id}` no mount (não listado explicitamente no "What", mas necessário para saber o `status`/`tempoConfiguradoSegundos`/`palavras` iniciais e detectar FE-24) e expõe `interrompida`/`palavras` no retorno, além dos 4 campos citados no "What" - ambos usados pelos próprios pontos do "Done when" desta task (FE-24) e por T17 (palavras `PENDENTE` após `resetar`).

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useAvaliacaoExecucao core state and iniciar flow`

---

### T17: `useAvaliacaoExecucao` - `pausar`/`continuar`/`resetar`

**What**: Adicionar `pausar()` (chama `POST .../pausar`, pausa o `MediaRecorder` e o cronômetro), `continuar()` (caminho inverso, `POST .../continuar`), `resetar()` (pede confirmação via callback injetado pela UI - o hook expõe `resetar(confirmado: boolean)` e só age se `true` -, chama `POST .../resetar`, descarta a gravação (`stop()` sem usar o Blob), volta o cronômetro ao tempo configurado e marca todas as palavras como `PENDENTE` no estado local).
**Where**: `frontend/src/features/avaliacoes/useAvaliacaoExecucao.ts` (modify)
**Depends on**: T16
**Reuses**: Reducer/estado de T16
**Requirement**: FE-13 (continuação), spec.md P1 "Executar avaliação" AC4/AC5

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `pausar` chama a API e pausa cronômetro+gravação juntos
- [x] `continuar` chama a API e retoma cronômetro+gravação juntos
- [x] `resetar(true)` chama a API, descarta a gravação, zera o cronômetro para o tempo configurado, e todas as palavras voltam a `PENDENTE`
- [x] `resetar(false)` não faz nada
- [x] Novos testes unitários: os 4 pontos acima
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 4 testes novos (4 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add pausar, continuar and resetar to useAvaliacaoExecucao`

---

### T18: `useAvaliacaoExecucao` - `finalizar`, botões por status, resync em 409

**What**: Adicionar `finalizar(motivo?: 'TEMPO_ESGOTADO')`: para a gravação, chama `POST .../finalizar` (com `motivo=TEMPO_ESGOTADO` quando o cronômetro chega em 0, sem `motivo` quando é clique manual); dispara automaticamente quando o cronômetro local chega a 00:00 (spec.md AC6). Adicionar a derivação `botoesHabilitados` a partir do `status` (CRIADA→Iniciar; EM_ANDAMENTO→Pausar/Resetar/Finalizar; PAUSADA→Continuar/Resetar/Finalizar; FINALIZADA/CANCELADA→nenhum - AC7). Em qualquer chamada de transição (`iniciar`/`pausar`/`continuar`/`resetar`/`finalizar`) que responder `409 TRANSICAO_INVALIDA`, recarregar `GET /avaliacoes/{id}` e resincronizar `status`/cronômetro/botões com o servidor, sem mostrar erro ao professor (AC9, FE-15).
**Where**: `frontend/src/features/avaliacoes/useAvaliacaoExecucao.ts` (modify)
**Depends on**: T17
**Reuses**: Reducer/estado de T16/T17
**Requirement**: FE-14, FE-15

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Cronômetro chegando a 0 chama `finalizar(motivo='TEMPO_ESGOTADO')` automaticamente
- [x] Clique manual em "Finalizar" chama `finalizar()` sem `motivo`
- [x] `botoesHabilitados` corresponde exatamente à tabela da AC7 para os 5 status
- [x] Uma resposta 409 `TRANSICAO_INVALIDA` em qualquer transição recarrega a avaliação e resincroniza o estado, sem lançar erro visível
- [x] Novos testes unitários: os 4 pontos acima, cobrindo os 5 valores de `status`
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 6 testes novos (8 testes)

**SPEC_DEVIATION (endpoint)**: `AvaliacaoController.finalizar` (`POST /avaliacoes/{id}/finalizar`) não tem `@RequestBody` - o backend decide sozinho se o motivo foi tempo esgotado (`AvaliacaoService.finalizarSeTempoEsgotado`, chamado antes de qualquer transição, não pelo `finalizar` explicitamente). `finalizar(motivo?)` mantém a assinatura pedida pelo design.md/task (o chamador ainda distingue "automático" de "clique manual"), mas `motivo` não é enviado no corpo da requisição - não há corpo a enviar. Documentado também como comentário no código (`useAvaliacaoExecucao.ts`, função `finalizar`).

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add finalizar, status-driven buttons and 409 resync`

---

### T19: `features/avaliacoes/CronometroDisplay.tsx`

**What**: Componente puramente visual: mostra `tempoRestanteMs` no formato `mm:ss`, e enquanto `gravando === true`, mostra o indicador "Gravando" com um ponto vermelho pulsante (spec.md AC8).
**Where**: `frontend/src/features/avaliacoes/CronometroDisplay.tsx` (new)
**Depends on**: T16
**Reuses**: Formato de estado exposto por `useAvaliacaoExecucao` (T16)
**Requirement**: FE-13 (exibição)

**Tools**:
- MCP: NONE
- Skill: `web-accessibility`

**Done when**:
- [x] 65000ms renderiza "01:05"
- [x] `gravando: true` mostra o indicador "Gravando"; `false` não mostra
- [x] `aria-label` presente para leitores de tela (SDD Edge Cases: navegação/rótulos acessíveis)
- [x] Novos testes de componente em `CronometroDisplay.test.tsx`: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos (3 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add CronometroDisplay with mm:ss format and recording indicator`

---

### T20: `components/GradePalavras.tsx`

**What**: Grade das palavras em ordem; tocar numa palavra (quando o status da avaliação é `EM_ANDAMENTO`, `PAUSADA` ou `FINALIZADA`) avança o ciclo PENDENTE→CORRETA→INCORRETA→NAO_LIDA→CORRETA, atualiza a cor na hora (otimista) e chama `onMarcar(ordem, novoStatus)` (spec.md AC1); atalhos de teclado `C`/`I`/`N` marcam a palavra em foco diretamente para CORRETA/INCORRETA/NAO_LIDA (spec.md, Assumptions); CORRETA = verde + ✓, INCORRETA = vermelho + ✗, NAO_LIDA = cinza + –, PENDENTE = cinza + borda tracejada, contraste mínimo 4.5:1 (AC2); se `onMarcar` rejeitar, reverte a palavra ao status anterior e mostra "Não foi possível salvar a marcação" (AC3); com a avaliação `CRIADA`, a marcação fica desabilitada (AC4); marcar uma palavra de avaliação `FINALIZADA` mostra "Alteração registrada em auditoria" além de atualizar o resultado exibido (AC5, via callback `onAposMarcarFinalizada` opcional que o pai usa para re-buscar o resultado).
**Where**: `frontend/src/components/GradePalavras.tsx` (new)
**Depends on**: T2
**Reuses**: `StatusPalavra`/`PalavraAvaliacao` (T2)
**Requirement**: FE-16, FE-17, FE-18

**Tools**:
- MCP: NONE
- Skill: `web-accessibility`

**Done when**:
- [x] Tocar 2x numa palavra deixa ela INCORRETA (vermelho, ✗) e chama `onMarcar(ordem, 'INCORRETA')`
- [x] Atalho `I` na palavra em foco marca INCORRETA diretamente
- [x] `onMarcar` rejeitando reverte a cor/status e mostra a mensagem de erro
- [x] Avaliação `CRIADA` não permite tocar/atalho
- [x] Avaliação `FINALIZADA` permite marcar e mostra o aviso de auditoria
- [x] Cores usam contraste >= 4.5:1 (checado com um snapshot de estilo/CSS ou teste de acessibilidade, não "a olho")
- [x] Novos testes de componente em `GradePalavras.test.tsx`: os 6 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 8 testes novos (9 testes; inclui atalhos C e N além do I exigido, e o primeiro toque PENDENTE→CORRETA)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add GradePalavras with tap cycle, shortcuts and audit warning`

---

### T21: `features/avaliacoes/useEnvioAudio.ts`

**What**: Ao finalizar a gravação (recebe um `Blob | null`), envia automaticamente via `apiClient.uploadAudio` com até 3 tentativas (esperas de 1s, 2s, 4s - spec.md AC2/Assumptions); se as 3 falharem, mantém o `Blob` em memória (`status: 'falhou'`), expõe `reenviar()` para tentativa manual, e registra um listener de `beforeunload` que pede confirmação enquanto `status !== 'enviado'` (AC3, FE-21).
**Where**: `frontend/src/features/avaliacoes/useEnvioAudio.ts` (new)
**Depends on**: T3
**Reuses**: `apiClient.uploadAudio` (T3)
**Requirement**: FE-20, FE-21

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Envio bem-sucedido na 1ª tentativa → `status: 'enviado'`, sem esperar
- [x] Falha na 1ª e sucesso na 2ª → respeita a espera de 1s entre tentativas (fake timers)
- [x] Falha nas 3 tentativas → `status: 'falhou'`, `Blob` retido, `beforeunload` registrado
- [x] `reenviar()` após falha total tenta de novo e pode suceder
- [x] `beforeunload` não é mais bloqueado após `status: 'enviado'`
- [x] Novos testes unitários em `useEnvioAudio.test.ts`: os 5 pontos acima
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 5 testes novos (5 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useEnvioAudio with automatic retry and manual resend`

---

### T22: `features/avaliacoes/ResultadoAvaliacaoPage.tsx`

**What**: Mostra total, lidas, corretas, incorretas, não lidas, percentual de acerto, tempo utilizado, fase e nível (AC1); usa `useEnvioAudio` para o envio automático e o botão "Tentar enviar novamente" quando `status === 'falhou'` (AC2/AC3); quando o áudio está salvo (`GET /avaliacoes/{id}` retorna êxito no fetch de áudio, ou o próprio envio confirma), mostra `<audio controls src=".../avaliacoes/{id}/audio">` e um botão "Baixar áudio" com `?download=true` (AC4); se `classificacaoPendente === true`, mostra "Classificação pendente: nenhuma regra cobre este resultado" (AC5).
**Where**: `frontend/src/features/avaliacoes/ResultadoAvaliacaoPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/avaliacoes/:id/resultado`)
**Depends on**: T21
**Reuses**: `useEnvioAudio` (T21), `AvaliacaoResponse` (T2)
**Requirement**: FE-19, FE-20, FE-21, FE-22

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Painel mostra todos os campos de AC1 com os valores da `AvaliacaoResponse`
- [x] Falha simulada no envio mostra "O áudio não foi enviado" + botão de reenvio; reenviar com sucesso mostra o player
- [x] Player usa o endpoint de streaming; botão "Baixar áudio" usa `?download=true`
- [x] `classificacaoPendente: true` mostra o aviso correspondente
- [x] Novos testes de componente em `ResultadoAvaliacaoPage.test.tsx`: os 4 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 4 testes novos (3 testes; o 2º cobre em conjunto "falha + reenvio" e "player/download", que compartilham o mesmo fluxo de estado - não há como exercitar um sem o outro)

**SPEC_DEVIATION (`?download=true`)**: `AvaliacaoController.baixarAudio`/`AvaliacaoService.baixarAudio` (backend) não leem nenhum `download` query param nem definem `Content-Disposition` - `?download=true` não tem efeito no servidor hoje (não é um bloqueio como o gap de T14: o endpoint existe e devolve os bytes certos). Como `GET /avaliacoes/{id}/audio` exige `Authorization: Bearer` (AD-009) e um `<audio src>`/`<a href>` nativo não envia esse header, a página já busca os bytes via `fetch` autenticado e expõe um Object URL (`useAudioObjectUrl`, interno ao arquivo) tanto para o player quanto para o link - o download é forçado no cliente pelo atributo HTML `download`, não pelo servidor. A URL requisitada mantém `?download=true` (Done when literal), mesmo ignorada pelo backend.
**Nota (agent's discretion)**: sem hook próprio listado na task para buscar a `AvaliacaoResponse` - `useQuery` inline no próprio `ResultadoAvaliacaoPage.tsx`, mesmo padrão de T14 (`useConfiguracaoAvaliacao` inline em `ConfigurarAvaliacaoPage.tsx`).

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ResultadoAvaliacaoPage with audio player, download and retry`

---

### T23: `features/avaliacoes/ExecutarAvaliacaoPage.tsx`

**What**: Compõe `useAvaliacaoExecucao` (T16-T18) + `CronometroDisplay` (T19) + `GradePalavras` (T20); passa `onMarcar` conectado a `PUT /avaliacoes/{id}/palavras/{ordem}` via `apiClient`; ao `finalizar` resolver, navega para `/avaliacoes/{id}/resultado` (T22), passando o `Blob` gravado via estado de navegação para `useEnvioAudio` consumir na página seguinte.
**Where**: `frontend/src/features/avaliacoes/ExecutarAvaliacaoPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/avaliacoes/:id/executar`)
**Depends on**: T16, T17, T18, T19, T20
**Reuses**: `useAvaliacaoExecucao`, `CronometroDisplay`, `GradePalavras`
**Requirement**: FE-11..FE-18 (composição)

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Negar o microfone mantém a tela em `CRIADA` com a mensagem de erro visível
- [x] Fluxo feliz: iniciar → cronômetro e "Gravando" aparecem juntos → marcar palavras → tempo zera → navega automaticamente para o resultado
- [x] Botões habilitados batem com o status atual em cada etapa
- [x] Novos testes de componente em `ExecutarAvaliacaoPage.test.tsx`: os 3 pontos acima (com `getUserMedia`/`MediaRecorder`/`apiClient` mockados)
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos (4 testes; o 4º cobre o edge case FE-24 de recarregar a página com a avaliação `EM_ANDAMENTO`, listado em spec.md e reafirmado no "What" desta task via o campo `interrompida` do hook)

**Correção pós-commit (orquestrador, fora desta task, entre a Fase 6 e a Fase 8)**: o worker de T23 sinalizou corretamente que o `finalizar('TEMPO_ESGOTADO')` disparado pelo `useEffect` de `useAvaliacaoExecucao` (T18) é fire-and-forget - seu retorno nunca chega ao componente, então o áudio de qualquer avaliação encerrada por tempo esgotado (o caminho mais comum na prática) nunca seria encaminhado ao envio, violando spec.md AC2/FE-20. Corrigido expondo o `Blob` também via `blobGravado` no estado do hook (`useAvaliacaoExecucao.ts`, `TRANSICAO_OK` ganhou o campo `blob`), lido por `ExecutarAvaliacaoPage` no mesmo `useEffect` que já navegava por `status === 'FINALIZADA'` - unificando os dois caminhos (clique manual e tempo esgotado) em vez de tratá-los separado. Teste de regressão adicionado em `useAvaliacaoExecucao.test.ts` (dentro do teste existente de tempo esgotado). Gate completo (lint+test+build) verde depois da correção, 108 testes.

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ExecutarAvaliacaoPage composing timer, recording and word grid`

---

### T24: `features/historico/useHistorico.ts`

**What**: Hook TanStack Query para `GET /alunos/{id}/historico-avaliacoes?anoLetivoId=&tipoLeitura=&cicloId=&page=`, aberto a PROFESSOR e COORDENADOR.
**Where**: `frontend/src/features/historico/useHistorico.ts` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`, `HistoricoAvaliacaoItem`/`Page<T>` (T2)
**Requirement**: FE-23 (parte 1)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Filtros opcionais (`anoLetivoId`/`tipoLeitura`/`cicloId`) só são enviados quando definidos
- [x] Paginação (`page`) propagada corretamente
- [x] Novos testes unitários em `useHistorico.test.ts`: sem filtros, com cada filtro, paginação
- [x] Gate check passes: `npm run test`
- [x] Test count: >= 3 testes novos (3 testes)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add useHistorico hook`

---

### T25: `features/historico/HistoricoTab.tsx`

**What**: Tabela paginada com as colunas do SDD §16 (ano letivo, série, turma, professor, ciclo, tipo de leitura, data, quantidades, percentual, classificação, tempo) e um ícone de play nas avaliações com `temAudio: true` (spec.md P1 "Histórico e evolução", AC1).
**Where**: `frontend/src/features/historico/HistoricoTab.tsx` (new)
**Depends on**: T24
**Reuses**: `useHistorico` (T24)
**Requirement**: FE-23 (parte 1)

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Todas as colunas do SDD §16 aparecem, com paginação funcional
- [x] Avaliação com `temAudio: true` mostra o ícone de play; sem áudio, não mostra
- [x] Novos testes de componente em `HistoricoTab.test.tsx`: os 2 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 2 testes novos (3 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add HistoricoTab table with pagination and audio icon`

---

### T26: `features/historico/useEvolucaoCiclos.ts` + `EvolucaoCiclosTab.tsx`

**What**: Hook para `GET /alunos/{id}/evolucao-ciclos?anoLetivoId=&tipoLeitura=` (COORDENADOR only - `RoleGate`, context.md) + aba que mostra Entrada/Acompanhamento/Saída com corretas, classificação e evolução, usando ▲ verde para positivo, ▼ vermelho para negativo, "—" para nulo (spec.md AC2).
**Where**: `frontend/src/features/historico/useEvolucaoCiclos.ts`, `frontend/src/features/historico/EvolucaoCiclosTab.tsx` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`, `EvolucaoCiclosResponse` (T2)
**Requirement**: FE-23 (parte 2)

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Escolher um tipo de leitura mostra os 3 ciclos com corretas/classificação/evolução
- [x] Evolução positiva mostra ▲ verde; negativa mostra ▼ vermelho; nula mostra "—"
- [x] Ciclo sem avaliação (`null` do backend) é tratado sem quebrar a tabela
- [x] Novos testes de componente/hook: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 4 testes novos (5 testes)

**Nota (agent's discretion)**: `EvolucaoCiclosResponse`/`ResultadoCiclo` (backend, `types.ts`) não trazem um campo de evolução pronto por ciclo - só corretas/classificação. A evolução exigida pela AC2 é calculada no cliente (`calcularEvolucao` em `EvolucaoCiclosTab.tsx`), comparando cada ciclo com o ciclo anterior da mesma resposta (Entrada nunca tem anterior -> sempre "—"), replicando a fórmula de `HistoricoEvolucaoService.evolucao` (HIST-14/17-19). AC2 também não define o caso `absoluta === 0`; tratado como neutro ("0", sem seta) - spec-precision gap documentado no código.

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add EvolucaoCiclosTab with directional indicators`

---

### T27: `features/historico/useEvolucaoAnual.ts` + `ComparacaoAnualTab.tsx`

**What**: Hook para `GET /alunos/{id}/evolucao-anos?tipoLeitura=` (COORDENADOR only) + aba com a tabela do SDD §15 (uma linha por ano, evolução por ciclo) mostrando "sem base" quando não há ano anterior para comparar (spec.md AC3 usa `semBase` - como o DTO real não tem esse campo explícito, a tela deriva `semBase = evolucao.absoluta === null && evolucao.percentual === null`).
**Where**: `frontend/src/features/historico/useEvolucaoAnual.ts`, `frontend/src/features/historico/ComparacaoAnualTab.tsx` (new)
**Depends on**: T3, T2
**Reuses**: `apiClient`, `EvolucaoAnualResponse` (T2)
**Requirement**: FE-23 (parte 3)

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Uma linha por ano letivo, ordenada crescente, com os 3 ciclos
- [x] Aluno com dados de 2 anos consecutivos no mesmo ciclo mostra `absoluta`/`percentual` corretos (caso do spec: "+13 / 108,33% na Entrada")
- [x] Ciclo sem ano anterior mostra "sem base" (não "0%" nem erro)
- [x] Novos testes de componente/hook: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos (4 testes)

**Nota (agent's discretion)**: quando só `percentual` é `null` (ano anterior com 0 corretas, HIST-19 evita divisão por zero) mas `absoluta` está definido, a célula mostra `absoluta` normalmente e "—" no lugar do percentual - isso não é "sem base" (havia um ano anterior; só o percentual não é calculável), distinção feita explicitamente em `CelulaCiclo` (`ComparacaoAnualTab.tsx`) para não confundir os dois casos.

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ComparacaoAnualTab with "sem base" handling`

---

### T28: `features/historico/HistoricoPage.tsx`

**What**: Página com 3 abas ("Histórico", "Evolução", "Comparação anual"); "Histórico" sempre visível (PROFESSOR+COORDENADOR); as outras duas só renderizam para COORDENADOR via `RoleGate` (context.md) - PROFESSOR não vê essas abas (não é um 403 na tela, a aba simplesmente não existe para ele).
**Where**: `frontend/src/features/historico/HistoricoPage.tsx` (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/alunos/:id/historico`)
**Depends on**: T25, T26, T27
**Reuses**: `HistoricoTab` (T25), `EvolucaoCiclosTab` (T26), `ComparacaoAnualTab` (T27), `RoleGate` (T5)
**Requirement**: FE-23

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] PROFESSOR vê só a aba "Histórico"
- [x] COORDENADOR vê as 3 abas
- [x] Rota `/alunos/:id/historico` protegida renderiza a página
- [x] Novos testes de componente em `HistoricoPage.test.tsx`: os 2 primeiros pontos
- [x] Gate check passes: `npm run lint && npm run test` (última task da Fase 8 - também rodado `npm run build`, gate de fechamento de fase)
- [x] Test count: >= 2 testes novos (3 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add HistoricoPage with role-gated tabs`

---

### T37: Composição raiz (`main.tsx`/`App.tsx`)

**What**: Substituir o boilerplate do template Vite por `App.tsx`: `QueryClientProvider` (novo `QueryClient`, config padrão do TanStack Query - sem customização especial de `retry`, já que cada hook trata seus próprios erros) envolvendo `AuthProvider` (T4) envolvendo `BrowserRouter` + as rotas de `router.tsx` (T5); `main.tsx` monta `<App/>`. Confirma que o callback de logout registrado no `apiClient` (interceptor 401, T3) é de fato o `logout` do `AuthContext` real em tempo de boot (não só nos testes isolados de cada peça).
**Where**: `frontend/src/App.tsx` (new or modify), `frontend/src/main.tsx` (modify)
**Depends on**: T6, T11, T14, T22, T23, T28
**Reuses**: `AuthProvider`/`useAuth` (T4), `AppRoutes`/`ProtectedRoute` (T5), `apiClient` (T3)
**Requirement**: Infra - fecha o gap reportado pelo Batch 1 (Phase 1): nenhuma task de T1-T36 faz o boot real da app

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Renderizar `<App/>` sem sessão em `sessionStorage` mostra a `LoginPage`
- [x] Renderizar `<App/>` com uma sessão válida em `sessionStorage` mostra o layout protegido na rota correspondente, sem passar pelo login
- [x] O 401 do `apiClient` (simulado via mock de `fetch`) desloga a sessão real e leva à `LoginPage`
- [x] Novos testes em `App.test.tsx`: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test` (escopado a `App.tsx`/`App.test.tsx`/`main.tsx` - o Batch 2 ainda tinha T14 em andamento no mesmo diretório no momento desta task; o gate completo do projeto roda de novo no fechamento da Fase 8b)
- [x] Test count: >= 3 testes novos (3 testes)

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): wire AuthProvider, QueryClientProvider and router into the app composition root`

---

### T38: Controle de logout em `AppLayout`

**What**: Adicionar um botão/link "Sair" visível em `AppLayout` para os dois perfis, que chama `useAuth().logout()` e navega para `/login`. Gap reportado pelo Batch 1 (Phase 1): nenhuma task de T1-T36 oferece uma forma de encerrar a sessão pela UI.
**Where**: `frontend/src/layout/AppLayout.tsx` (modify)
**Depends on**: T5
**Reuses**: `useAuth` (T4)
**Requirement**: Complemento não-numerado de FE-01/FE-04 (não amplia o spec - fecha um buraco óbvio da própria história de login/menu)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] "Sair" visível tanto para PROFESSOR quanto para COORDENADOR
- [x] Clicar em "Sair" chama `logout()` e navega para `/login`
- [x] Novo teste em `AppLayout.test.tsx`: clique em "Sair" desloga e navega
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 1 teste novo (1 teste)

**Tests**: unit
**Gate**: quick

**Commit**: `feat(frontend-web): add logout control to AppLayout`

---

### T29: `e2e/fluxo-professor.spec.ts`

**What**: Teste Playwright do fluxo completo do SDD §20 (spec.md Success Criteria): login como PROFESSOR → busca do aluno → seleção → "Configurar avaliação" → preenche o formulário com uma lista de palavras válida → cria → tela de execução → concede o microfone falso (`launchOptions: {args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream']}`) → inicia → marca pelo menos uma palavra → espera o tempo configurado (usar `tempoSegundos` baixo, ex. 10s, só para o teste) zerar → tela de resultado aparece automaticamente com os totais e o player de áudio.
**Where**: `frontend/e2e/fluxo-professor.spec.ts` (new), `frontend/playwright.config.ts` (modify: adiciona os `launchOptions` de mídia falsa)
**Depends on**: T37
**Reuses**: Toda a stack de P1 (T1-T28, T37); precisa do backend real rodando (Testcontainers/dev) - documentar no próprio spec de teste o pré-requisito
**Requirement**: Success Criteria do spec.md (E2E)

**Ambiente conhecido (Batch 1)**: `npx playwright install --with-deps` falha em macOS 12 ("Playwright does not support chromium on mac12"); `@playwright/test`/`playwright.config.ts` já existem (T1), mas os browsers do Playwright não instalam nesta máquina. Esta task fica bloqueada em ambiente que não seja macOS 13+/CI até resolver - não é um problema do código.

**Tools**:
- MCP: NONE
- Skill: `playwright-skill`

**Done when**:
- [ ] O teste cobre login → busca → configuração → iniciar → marcar → tempo zerado → resultado → áudio, sem intervenção manual
- [ ] O microfone falso é aceito sem prompt real do SO
- [ ] Gate check passes: `npm run build && npm run test:e2e`

**Tests**: e2e
**Gate**: e2e (`npm run build && npm run test:e2e`)

**Commit**: `test(frontend-web): add Playwright E2E for the full professor flow`

---

### T30: `features/cadastros/anosletivos/AnoLetivoPage.tsx`

**What**: Tela CRUD fina para anos letivos: lista (`GET /anos-letivos`), criar (`CriarAnoLetivoRequest`), editar a configuração de avaliação por série (`AtualizarConfiguracaoRequest`/`ConfiguracaoAvaliacaoResponse` - mínimo/máximo de palavras); "Salvo com sucesso" no sucesso, erro 409/422 no topo/campo (spec.md P2, AC1/AC2).
**Where**: `frontend/src/features/cadastros/anosletivos/AnoLetivoPage.tsx` (+ `useAnosLetivos.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/anos-letivos`)
**Depends on**: T5 (fundação já disponível; independente das demais tasks P2)
**Reuses**: Mesmo padrão de mapeamento de erro 422 de `ConfigurarAvaliacaoPage` (T14), `apiClient`
**Requirement**: FE-26

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Criar um ano letivo válido mostra "Salvo com sucesso" e aparece na lista
- [x] 409 (ex. ano duplicado) mostra a mensagem do `code` no topo do formulário
- [x] 422 mostra a mensagem por campo
- [x] Novos testes de componente em `AnoLetivoPage.test.tsx`: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: full

**SPEC_DEVIATION (endpoint)**: `GET /anos-letivos` e `GET /anos-letivos/{id}/configuracoes` não existiam no backend; adicionados (COORDENADOR-only, aditivos) em `5287f54` por decisão do usuário. T31-T35 vão precisar do mesmo para turmas/professores/usuários.

**Commit**: `feat(frontend-web): add AnoLetivoPage CRUD screen`

---

### T31: `features/cadastros/turmas/TurmasPage.tsx`

**What**: Tela CRUD fina para turmas (`CriarTurmaRequest`, `AtualizarProfessorRequest`), mesmo padrão de T30.
**Where**: `frontend/src/features/cadastros/turmas/TurmasPage.tsx` (+ `useTurmas.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/turmas`)
**Depends on**: T5
**Reuses**: Mesmo padrão de T30
**Requirement**: FE-26

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Criar uma turma válida mostra "Salvo com sucesso" e aparece na lista
- [x] Trocar o professor de uma turma existente reflete na lista
- [x] 409/422 tratados como em T30
- [x] Novos testes de componente em `TurmasPage.test.tsx`: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: full

**SPEC_DEVIATION (endpoint)**: `GET /turmas` e `GET /professores` (com turmas ativas) não existiam; adicionados no backend (COORDENADOR-only) junto com esta task - o `GET /professores` já cobre a listagem de T32.

**Commit**: `feat(frontend-web): add TurmasPage CRUD screen`

---

### T32: `features/cadastros/professores/ProfessoresPage.tsx`

**What**: Tela CRUD fina para professores (`CriarProfessorRequest`, lista suas turmas via `TurmaResumoResponse`), mesmo padrão de T30.
**Where**: `frontend/src/features/cadastros/professores/ProfessoresPage.tsx` (+ `useProfessores.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/professores`)
**Depends on**: T5
**Reuses**: Mesmo padrão de T30
**Requirement**: FE-26

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Criar um professor válido mostra "Salvo com sucesso" e aparece na lista com suas turmas
- [x] 409/422 tratados como em T30
- [x] Novos testes de componente em `ProfessoresPage.test.tsx`: os 2 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 2 testes novos

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ProfessoresPage CRUD screen`

---

### T33: `features/cadastros/alunos/AlunosCadastroPage.tsx`

**What**: Tela CRUD fina para alunos + matrícula: criar (`CriarAlunoRequest`), atualizar nome (`AtualizarNomeAlunoRequest`), inativar, nova matrícula (`NovaMatriculaRequest`), atualizar matrícula (`AtualizarMatriculaRequest`) - mesmo padrão de T30. Distinta de `AlunoBuscaPage` (T8, uso do professor/coordenador para localizar um aluno) - esta é a tela de manutenção cadastral do coordenador.
**Where**: `frontend/src/features/cadastros/alunos/AlunosCadastroPage.tsx` (+ `useAlunosCadastro.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/alunos`)
**Depends on**: T5
**Reuses**: Mesmo padrão de T30
**Requirement**: FE-26

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Criar aluno+matrícula válidos mostra "Salvo com sucesso" e aparece na lista
- [x] Inativar um aluno remove/marca-o como inativo na lista
- [x] Nova matrícula para um aluno existente reflete a mudança
- [x] 409/422 tratados como em T30
- [x] Novos testes de componente em `AlunosCadastroPage.test.tsx`: os 4 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 4 testes novos

**Tests**: unit
**Gate**: full

**SPEC_DEVIATION (listagem)**: sem `GET /alunos` de listagem geral; por decisão do usuário a "lista" é a busca por nome (`useAlunoBusca`), e um aluno recém-criado aparece porque a busca passa a usar o nome dele.

**Commit**: `feat(frontend-web): add AlunosCadastroPage with matrícula management`

---

### T34: `features/cadastros/usuarios/UsuariosPage.tsx`

**What**: Tela CRUD fina para usuários (`CriarUsuarioRequest`, `AlterarSenhaRequest`, `UsuarioResponse`), mesmo padrão de T30.
**Where**: `frontend/src/features/cadastros/usuarios/UsuariosPage.tsx` (+ `useUsuarios.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/usuarios`)
**Depends on**: T5
**Reuses**: Mesmo padrão de T30
**Requirement**: FE-26

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [x] Criar um usuário válido mostra "Salvo com sucesso" e aparece na lista
- [x] Alterar senha de um usuário existente funciona e não expõe a senha em nenhum lugar da UI/log
- [x] 409/422 tratados como em T30
- [x] Novos testes de componente em `UsuariosPage.test.tsx`: os 3 pontos acima
- [x] Gate check passes: `npm run lint && npm run test`
- [x] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: full

**SPEC_DEVIATION (endpoint)**: `GET /usuarios` não existia; adicionado no backend (COORDENADOR-only, sem senha/hash) por decisão do usuário.

**Commit**: `feat(frontend-web): add UsuariosPage CRUD screen`

---

### T35: `features/cadastros/listas/ListasPalavrasPage.tsx`

**What**: Tela CRUD completa para listas de palavras (`CriarListaPalavrasRequest`, `AtualizarListaPalavrasRequest`, `ListaPalavrasResponse` com `ItemPalavraRequest`/`ItemPalavraResponse`); ao cadastrar uma lista do 1º ano, desabilita a opção `NAO_CANONICA` (spec.md P2, AC4).
**Where**: `frontend/src/features/cadastros/listas/ListasPalavrasPage.tsx` (+ `useListasPalavrasCadastro.ts`) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/listas-palavras`)
**Depends on**: T5
**Reuses**: `useListasPalavras` (T12, para a leitura - esta task adiciona create/update/inativar por cima)
**Requirement**: FE-26, FE-27 (parte NAO_CANONICA)

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [ ] Criar uma lista válida (série != 1) mostra "Salvo com sucesso" e permite escolher `NAO_CANONICA`
- [ ] Criar uma lista do 1º ano desabilita a opção `NAO_CANONICA` na UI
- [ ] 409/422 tratados como em T30
- [ ] Novos testes de componente em `ListasPalavrasPage.test.tsx`: os 3 pontos acima
- [ ] Gate check passes: `npm run lint && npm run test`
- [ ] Test count: >= 3 testes novos

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add ListasPalavrasPage with 1st-grade NAO_CANONICA restriction`

---

### T36: `features/cadastros/regras/RegrasClassificacaoPage.tsx`

**What**: Tela para editar as faixas de classificação de uma série (`SubstituirRegrasClassificacaoRequest`/`FaixaRequest`/`RegraClassificacaoResponse`); mostra uma pré-visualização da régua de 0 a 60 acertos com a cor de cada fase, destacando lacunas e sobreposições **antes do envio** (spec.md P2, AC3 - a régua é calculada no cliente: ordenar as faixas por `quantidadeMinimaAcertos`, pintar cada intervalo `[min, max ?? 60]`; um "buraco" entre o fim de uma faixa e o início da próxima é uma lacuna; um intervalo sobreposto é uma sobreposição - ambos destacados visualmente, sem bloquear o envio, já que a validação definitiva é do backend).
**Where**: `frontend/src/features/cadastros/regras/RegrasClassificacaoPage.tsx` (+ `useRegrasClassificacao.ts`, `reguaClassificacao.ts` helper puro) (new), `frontend/src/app/router.tsx` (modify: adiciona a rota `/cadastros/regras-classificacao`)
**Depends on**: T5
**Reuses**: Mesmo padrão de erro de T30
**Requirement**: FE-26, FE-27

**Tools**:
- MCP: NONE
- Skill: `react-best-practices`

**Done when**:
- [ ] Editar as faixas de uma série mostra a régua 0-60 com uma cor por fase
- [ ] Uma lacuna entre faixas é destacada visualmente antes do envio
- [ ] Uma sobreposição entre faixas é destacada visualmente antes do envio
- [ ] Salvar sem lacunas/sobreposições mostra "Salvo com sucesso"; 409/422 tratados como em T30
- [ ] Novos testes: `reguaClassificacao.test.ts` (helper: lacuna, sobreposição, régua completa sem problemas) + `RegrasClassificacaoPage.test.tsx` (integração visual básica)
- [ ] Gate check passes: `npm run lint && npm run test`
- [ ] Test count: >= 6 testes novos

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend-web): add RegrasClassificacaoPage with gap/overlap preview ruler`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 8b → Phase 9 → Phase 10

Phase 1:   T1 → T2 → T3 → T4 → T5 → T6
Phase 2:   T7 → T8 ------------------→ T11
           T9 → T10 -------------------↗
Phase 3:   T12 ------→ T14
           T13 ------↗
           T9  -------↗ (cross-phase, Phase 2)
Phase 4:   T15 → T16 → T17 → T18
                  T16 --------→ T19
Phase 5:   T2 (Phase 1) → T20
Phase 6:   T21 → T22
Phase 7:   T16,T17,T18,T19,T20 (Phases 4-5) → T23
Phase 8:   T24 → T25 ------→ T28
           T26 -------------↗
           T27 -------------↗
Phase 8b:  T6,T11,T14,T22,T23,T28 (Phases 1,2,3,6,7,8) → T37
           T5 (Phase 1) → T38
Phase 9:   T37 (Phase 8b) → T29
Phase 10:  T30  T31  T32  T33  T34  T35  T36  (independentes entre si)
```

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

**Batching**: 38 tasks total (T1-T38, after Batch 1 added T37/T38 to close a gap it reported) → packs into ~5 task-budgeted batches (~7-8 each, whole phases, never split a phase): `[Phase1]` (done), `[Phase2+Phase3]`, `[Phase4+Phase5+Phase6]`, `[Phase7+Phase8+Phase8b]`, `[Phase9+Phase10]`. Offer sub-agents before Execute, per the skill's Sub-Agent Delegation section.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: scaffold do projeto | 1 diretório novo, config/tooling | ✅ Granular (setup, não split significativo) |
| T2: `types.ts` | 1 arquivo | ✅ Granular |
| T3: `apiClient` | 1 arquivo, 2 funções relacionadas (`request`, `uploadAudio`) | ✅ Granular |
| T4: `AuthContext`/`useAuth` | 1 arquivo | ✅ Granular |
| T5: router + layout | 2 arquivos, mesma finalidade imediata (navegação por perfil) | ✅ Granular (cohesivo) |
| T6: `LoginPage` | 1 componente | ✅ Granular |
| T7: `useAlunoBusca` | 1 hook | ✅ Granular |
| T8: `AlunoBuscaPage` | 1 componente | ✅ Granular |
| T9: `calcularCicloAtual` | 1 função pura | ✅ Granular |
| T10: `useAlunoResumo` | 1 hook | ✅ Granular |
| T11: `ResumoAlunoPanel` | 1 componente (+ 1 modificação pontual) | ✅ Granular |
| T12: `useListasPalavras` | 1 hook | ✅ Granular |
| T13: `useCriarAvaliacao` | 1 hook | ✅ Granular |
| T14: `ConfigurarAvaliacaoPage` | 1 componente (formulário de 1 história) | ✅ Granular |
| T15: `media/recorder.ts` | 1 arquivo, 3 funções da mesma responsabilidade | ✅ Granular |
| T16: `useAvaliacaoExecucao` (base + iniciar) | 1 arquivo, 1 fatia coesa do reducer | ✅ Granular |
| T17: `useAvaliacaoExecucao` (pausar/continuar/resetar) | 1 arquivo (mesmo), 3 métodos relacionados | ✅ Granular (cohesivo) |
| T18: `useAvaliacaoExecucao` (finalizar + botões + 409) | 1 arquivo (mesmo), 1 fatia coesa final | ✅ Granular |
| T19: `CronometroDisplay` | 1 componente | ✅ Granular |
| T20: `GradePalavras` | 1 componente (1 história completa) | ✅ Granular |
| T21: `useEnvioAudio` | 1 hook | ✅ Granular |
| T22: `ResultadoAvaliacaoPage` | 1 componente | ✅ Granular |
| T23: `ExecutarAvaliacaoPage` | 1 componente de composição | ✅ Granular |
| T24: `useHistorico` | 1 hook | ✅ Granular |
| T25: `HistoricoTab` | 1 componente | ✅ Granular |
| T26: `useEvolucaoCiclos` + `EvolucaoCiclosTab` | 2 arquivos, 1 hook + 1 componente da mesma aba | ✅ Granular (cohesivo) |
| T27: `useEvolucaoAnual` + `ComparacaoAnualTab` | 2 arquivos, mesma cohesão de T26 | ✅ Granular |
| T28: `HistoricoPage` | 1 componente (container de abas) | ✅ Granular |
| T29: E2E do fluxo do professor | 1 spec Playwright | ✅ Granular |
| T30-T36: telas de cadastro (7x) | 1 tela + 1 hook por entidade, mesmo padrão mecânico repetido | ✅ Granular (cada uma é 1 CRUD fino independente) |
| T37: composição raiz | 1-2 arquivos, 1 finalidade (boot da app) | ✅ Granular |
| T38: logout em `AppLayout` | 1 arquivo, 1 controle | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | Nenhuma seta de entrada | ✅ Match |
| T2 | T1 | T1→T2 | ✅ Match |
| T3 | T1, T2 | T2→T3 (T1 implícito na cadeia) | ✅ Match |
| T4 | T3 | T3→T4 | ✅ Match |
| T5 | T4 | T4→T5 | ✅ Match |
| T6 | T5 | T5→T6 | ✅ Match |
| T7 | T3, T2 | Fase 1 (implícito) → T7 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T2 | Fase 1 (implícito) → T9 | ✅ Match |
| T10 | T9 | T9→T10 | ✅ Match |
| T11 | T8, T10 | T8→T11, T10→T11 | ✅ Match |
| T12 | T3, T2 | Fase 1 (implícito) → T12 | ✅ Match |
| T13 | T3, T2 | Fase 1 (implícito) → T13 | ✅ Match |
| T14 | T12, T13, T9 | T12→T14, T13→T14, T9→T14 | ✅ Match |
| T15 | T1 | Fase 1 (implícito) → T15 | ✅ Match |
| T16 | T15 | T15→T16 | ✅ Match |
| T17 | T16 | T16→T17 | ✅ Match |
| T18 | T17 | T17→T18 | ✅ Match |
| T19 | T16 | T16→T19 | ✅ Match |
| T20 | T2 | T2→T20 | ✅ Match |
| T21 | T3 | Fase 1 (implícito) → T21 | ✅ Match |
| T22 | T21 | T21→T22 | ✅ Match |
| T23 | T16, T17, T18, T19, T20 | Todos→T23 | ✅ Match |
| T24 | T3, T2 | Fase 1 (implícito) → T24 | ✅ Match |
| T25 | T24 | T24→T25 | ✅ Match |
| T26 | T3, T2 | Fase 1 (implícito) → T26 | ✅ Match |
| T27 | T3, T2 | Fase 1 (implícito) → T27 | ✅ Match |
| T28 | T25, T26, T27 | T25→T28, T26→T28, T27→T28 | ✅ Match |
| T29 | T37 | T37→T29 | ✅ Match |
| T30-T36 | T5 (cada uma) | Fase 1 (implícito) → cada uma | ✅ Match |
| T37 | T6, T11, T14, T22, T23, T28 | Todos→T37 | ✅ Match |
| T38 | T5 | T5→T38 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: scaffold | Config/tooling | none | none | ✅ OK |
| T2: `types.ts` | Config/tooling | none | none | ✅ OK |
| T3: `apiClient` | `apiClient` (parse de erro, 401) | unit | unit | ✅ OK |
| T4: `AuthContext` | Hook de dados (sessão) | unit | unit | ✅ OK |
| T5: router + layout | Componente com lógica | unit (component) | unit | ✅ OK |
| T6: `LoginPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T7: `useAlunoBusca` | Hook de dados | unit | unit | ✅ OK |
| T8: `AlunoBuscaPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T9: `calcularCicloAtual` | Helper puro | unit | unit | ✅ OK |
| T10: `useAlunoResumo` | Hook de dados | unit | unit | ✅ OK |
| T11: `ResumoAlunoPanel` | Componente com lógica | unit (component) | unit | ✅ OK |
| T12: `useListasPalavras` | Hook de dados | unit | unit | ✅ OK |
| T13: `useCriarAvaliacao` | Hook de dados | unit | unit | ✅ OK |
| T14: `ConfigurarAvaliacaoPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T15: `media/recorder.ts` | Helper puro | unit | unit | ✅ OK |
| T16: `useAvaliacaoExecucao` (base) | Hook de dados/estado | unit | unit | ✅ OK |
| T17: `useAvaliacaoExecucao` (transições) | Hook de dados/estado | unit | unit | ✅ OK |
| T18: `useAvaliacaoExecucao` (finalizar) | Hook de dados/estado | unit | unit | ✅ OK |
| T19: `CronometroDisplay` | Componente com lógica | unit (component) | unit | ✅ OK |
| T20: `GradePalavras` | Componente com lógica | unit (component) | unit | ✅ OK |
| T21: `useEnvioAudio` | Hook de dados | unit | unit | ✅ OK |
| T22: `ResultadoAvaliacaoPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T23: `ExecutarAvaliacaoPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T24: `useHistorico` | Hook de dados | unit | unit | ✅ OK |
| T25: `HistoricoTab` | Componente com lógica | unit (component) | unit | ✅ OK |
| T26: `useEvolucaoCiclos`+`EvolucaoCiclosTab` | Hook + componente | unit | unit | ✅ OK |
| T27: `useEvolucaoAnual`+`ComparacaoAnualTab` | Hook + componente | unit | unit | ✅ OK |
| T28: `HistoricoPage` | Componente com lógica | unit (component) | unit | ✅ OK |
| T29: E2E | Fluxo completo | e2e | e2e | ✅ OK |
| T30-T36: telas de cadastro | Página de composição simples (P2) | unit (component, smoke) | unit | ✅ OK |
| T37: composição raiz | Componente de composição (smoke) | unit (component, smoke) | unit | ✅ OK |
| T38: logout em `AppLayout` | Componente com lógica | unit (component) | unit | ✅ OK |

Nenhuma violação - todas as tasks testam no mesmo commit em que o código é criado, no tipo exigido pela matriz.

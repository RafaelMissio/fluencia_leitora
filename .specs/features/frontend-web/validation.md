# Frontend Web Validation

**Date**: 2026-10-04
**Spec**: `.specs/features/frontend-web/spec.md`
**Diff range**: `ba99db9^..3c8691a` (45 commits `frontend-web`, mais os commits backend aditivos `72df217`, `5287f54`, `6527ce0`, `9761848`, `5a7f212`). Superfície: `frontend/` inteiro.
**Verifier**: sub-agente independente (autor != verificador)

## Veredito: FAIL (1 AC de P1 inalcançável pela UI, 1 defeito de dados em P1, 3 mutantes sobreviventes)

O gate de build está verde (lint 0 erros, 161 testes, build ok, cobertura de linhas 94,76%) e o E2E (2/2 em stack real, executado pelo orquestrador) é citado, não repetido. Mas a verificação ancorada na spec e a leitura do código encontraram defeitos de comportamento que os testes unitários não pegam, e o sensor deixou 3 mutantes vivos (de 9). Nenhum código foi alterado por este Verifier.

---

## Task Completion

| Task | Status | Notas |
| ---- | ------ | ----- |
| T1-T36 | Done | Todas as 238 caixas `Done when` de tasks.md estão marcadas `[x]`; 0 caixas `[ ]` |
| T37, T38 | Done | Adicionadas durante a execução (composição raiz; logout em `AppLayout`) |
| T29 (E2E) | Done | `frontend/e2e/fluxo-professor.spec.ts`. Executado pelo orquestrador 2/2 em 2026-10-04 contra backend, MySQL e Chrome reais. Não repetido aqui. Sem as variáveis `E2E_*` o teste é pulado (`test.skip`, linha 46) |

**SPEC_DEVIATION aceitos** (documentados em tasks.md, só listados):
1. T14: endpoint de limites `GET /anos-letivos/ativo/configuracoes/{serie}` criado no backend (`72df217`).
2. T18: `finalizar` não envia `motivo` (backend sem `@RequestBody`); `motivo=TEMPO_ESGOTADO` da spec P1 Executar AC6 não é enviado.
3. T22: `?download=true` ignorado pelo servidor; download forçado no cliente com o atributo `download`.
4. T30, T31, T32, T34: `GET` de listagem de anos-letivos, turmas, professores e usuários adicionados ao backend (COORDENADOR-only).
5. T33: sem `GET /alunos`; a "lista" é a busca por nome.
6. T35: `version` exposto em `ListaPalavrasResponse`.
7. context.md: abas Evolução e Comparação anual só para COORDENADOR; "evolução no ano" do PROFESSOR calculada no cliente.

---

## Spec-Anchored Acceptance Criteria

Legenda: PASS = asserção no valor exato da spec. PARCIAL = comportamento existe no código, mas o teste não cobre toda a frase do AC. GAP = o AC não é atendido pelo fluxo real do usuário.

### P1: Login e navegação por perfil

| Critério | Resultado definido na spec | `file:line` + asserção | Result |
| -------- | -------------------------- | ---------------------- | ------ |
| AC1 login válido guarda o token e abre Avaliar (PROFESSOR) / Alunos (COORDENADOR) | `/avaliar` / `/alunos` | `src/auth/LoginPage.test.tsx:55` - `getByText('Tela Avaliar')`; `:66` - `getByText('Tela Alunos')`. Token: `src/auth/AuthContext.test.tsx:47` (grava no contexto e no `sessionStorage`) | PASS |
| AC2 401 mostra "E-mail ou senha inválidos" sem limpar o e-mail | texto exato; campo preservado | `LoginPage.test.tsx:87` - `toHaveTextContent('E-mail ou senha inválidos')`; `:88` - `getByLabelText('E-mail')).toHaveValue('prof@escola.com')` | PASS |
| AC3 429 mostra "Conta bloqueada. Tente novamente em N minutos" a partir de `Retry-After` | N = ceil(125/60) = 3 | `LoginPage.test.tsx:99` - `toHaveTextContent('Conta bloqueada. Tente novamente em 3 minutos')` | PASS (arredondamento para cima é spec-precision: spec não diz "para cima") |
| AC4 401 em requisição autenticada descarta o token e volta ao login preservando a rota | token limpo; `state.from` | `AuthContext.test.tsx:112-113` (`isAuthenticated=false`, `sessionStorage` nulo); `src/app/router.test.tsx:18` (redireciona para `/login` preservando a origem); `LoginPage.test.tsx:77` (volta ao `from`) | PARCIAL: as 3 partes são testadas em arquivos separados, nenhum teste liga 401 -> login -> rota original |
| AC5 PROFESSOR vê só Avaliar, Meus alunos, Histórico | 3 itens | `src/layout/AppLayout.test.tsx:13` (teste "exactly the 3 PROFESSOR menu items") | PASS |
| AC6 COORDENADOR vê 8 itens, sem Avaliar | 8 itens, sem "Avaliar" | `AppLayout.test.tsx:36` (teste "exactly the 8 COORDENADOR menu items and never Avaliar") | PASS (menu). Ver gap de RoleGate nas rotas |

### P1: Buscar aluno e ver resumo

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 2+ caracteres, 300 ms após a última tecla, lista nome/turma/série | `enabled` com >=2; 1 chamada após 300 ms | `src/features/alunos/useAlunoBusca.test.ts:45` (`fetch` não chamado com 1 char); `:59,63` (não chamado antes de 300 ms); `:66` - `toHaveBeenCalledTimes(1)`; `:68` - url `/api/v1/alunos?nome=joa&page=0`. Lista: `AlunoBuscaPage.test.tsx:57` - `getByText('João - A - 2ª série')` | PASS |
| AC2 resumo com 9 campos (EVO-01) | nome, turma, professor, ano, série, ciclo atual, última classificação, 5 recentes, evolução | `ResumoAlunoPanel.test.tsx:61-69` (todos os campos); `useAlunoResumo.test.ts:194-195` (`toHaveLength(5)`, ids `[7,6,5,4,3]`); `:102` e `:148` (evolução PROFESSOR/COORDENADOR). Ver GAP-2 sobre "ciclo atual" | PARCIAL (GAP-2) |
| AC3 sem resultado mostra "Nenhum aluno encontrado" | texto exato | `AlunoBuscaPage.test.tsx:98` - `getByText('Nenhum aluno encontrado')` | PASS |

### P1: Configurar a avaliação

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 ciclo = ciclo atual, data = hoje, tempo = 60 | ENTRADA (id 1), hoje, 60 | `src/features/avaliacoes/ConfigurarAvaliacaoPage.test.tsx:101` (`value` do ciclo `'1'`); `:106` (data = hoje); `:107` (`'60'`) | PASS para a fórmula; a fórmula em si é defeituosa (GAP-2) |
| AC2 listas da série e do tipo + "Digitar palavras"/"Digitar texto" (TEXTO_CURTO) | rótulos e lista | `ConfigurarAvaliacaoPage.test.tsx:121-126`; filtro série+tipo: `useListasPalavras.test.ts:25` | PASS |
| AC3 contador "N palavras (mín. X, máx. Y)" e botão desabilitado fora do intervalo | 14 desabilitado, 15 habilitado | `ConfigurarAvaliacaoPage.test.tsx:134` (`mín. 15, máx. 20`); `:138` - `toBeDisabled()`; `:141` - `toBeEnabled()` | PARCIAL: o limite superior (N > máx) não é testado; o código o implementa em `ConfigurarAvaliacaoPage.tsx:145` |
| AC4 422 mostra a mensagem junto ao campo | mensagem ao lado do campo | `ConfigurarAvaliacaoPage.test.tsx:154` - `getByText('data inválida')`; mapeamento por `field` em `ConfigurarAvaliacaoPage.tsx:95-98` | PARCIAL: só o campo `dataAvaliacao`; erros com `field` aninhado (`palavras[0].palavra`) não casam com `erros.palavras` e ficam invisíveis |
| AC5 criada, abre a execução com a grade em ordem | rota `/avaliacoes/{id}/executar` | `ConfigurarAvaliacaoPage.test.tsx:176` - `getByText('Tela de execução')`; `:177-179` - payload `alunoId: 42, tipoLeitura: 'PALAVRA'` | PARCIAL: a ordem da grade não é asserida (o E2E passa pela grade) |

### P1: Executar a avaliação com cronômetro e gravação

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 pede microfone antes de `iniciar` | `getUserMedia({audio:true})` primeiro | `src/features/avaliacoes/useAvaliacaoExecucao.test.ts:110` - `toHaveBeenCalledWith({ audio: true })`; ordem em `useAvaliacaoExecucao.ts:202-208` | PARCIAL: a ordem (microfone antes da API) só é provada indiretamente pelo caso de negação |
| AC2 microfone negado: mensagem exata, `iniciar` não chamado, continua CRIADA | texto exato | `useAvaliacaoExecucao.test.ts:111-116`; `ExecutarAvaliacaoPage.test.tsx:97` - `toHaveTextContent('Permita o acesso ao microfone para iniciar a avaliação')` | PASS |
| AC3 microfone liberado e `iniciar` 200: grava e conta juntos | `start()` 1x, `gravando=true`, tempo decresce | `useAvaliacaoExecucao.test.ts:141-142,149`; `ExecutarAvaliacaoPage.test.tsx:127-128` | PASS |
| AC4 Pausar/Continuar chamam a API e pausam/retomam gravação e cronômetro | url, POST, `pause`/`resume` 1x, tempo congelado | `useAvaliacaoExecucao.test.ts:202-207,214` (pausar); `:229-234,241` (continuar) | PASS |
| AC5 Resetar com confirmação | API, descarta, tempo = configurado, palavras PENDENTE | `useAvaliacaoExecucao.test.ts:261-263,264`; `:271-281` (`resetar(false)` não faz nada) | PARCIAL: o diálogo `window.confirm` (`ExecutarAvaliacaoPage.tsx:47`) não tem teste de página |
| AC6 00:00 ou Finalizar para a gravação, chama `finalizar`, mostra o resultado | `finalizar` chamado; resultado na tela | `useAvaliacaoExecucao.test.ts:304,308` (tempo zerado); `:321-326` (manual); `ExecutarAvaliacaoPage.test.tsx:149` - `getByText('Tela de resultado')` | PARCIAL: `motivo=TEMPO_ESGOTADO` não é enviado (SPEC_DEVIATION aceita) |
| AC7 botões por status | 5 status | `useAvaliacaoExecucao.test.ts:329-343` (`it.each` com `toEqual(esperado)`); `ExecutarAvaliacaoPage.test.tsx:177,186,193` | PASS |
| AC8 "Gravando" com ponto vermelho pulsante em EM_ANDAMENTO | indicador visível | `src/features/avaliacoes/CronometroDisplay.test.tsx:12` (teste do indicador); `ExecutarAvaliacaoPage.test.tsx:127`. Animação em `CronometroDisplay.tsx:23` | PARCIAL: cor/animação não são asserida |
| AC9 409 `TRANSICAO_INVALIDA` recarrega e sincroniza botões e cronômetro | status = servidor | `useAvaliacaoExecucao.test.ts:357` - `expect(result.current.status).toBe('FINALIZADA')`; `:359` (último fetch é o GET) | PARCIAL: só `pausar`, não `finalizar`; cronômetro não asserido; o código `TRANSICAO_INVALIDA` não é discriminado (M6 sobreviveu) |

### P1: Marcar palavras

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 ciclo PENDENTE -> CORRETA -> INCORRETA -> NAO_LIDA -> CORRETA em EM_ANDAMENTO, PAUSADA ou FINALIZADA, atualiza a cor e envia à API | 4 transições | `src/components/GradePalavras.test.tsx:29` (1 toque = CORRETA); `:40` (2 toques = INCORRETA); atalhos C/I/N em `:55,66,77`; PUT em `ExecutarAvaliacaoPage.test.tsx:135` | PARCIAL: nenhum teste exercita INCORRETA -> NAO_LIDA -> CORRETA (M7 sobreviveu). Em FINALIZADA: ver GAP-1 |
| AC2 cores/ícones, contraste >= 4.5:1 | verde ✓, vermelho ✗, cinza –, PENDENTE tracejado | `GradePalavras.test.tsx:122` (teste de contraste das 4 cores contra branco); config em `GradePalavras.tsx:26-31` | PASS |
| AC3 falha no envio volta ao status anterior e mostra "Não foi possível salvar a marcação" | reverte + mensagem | `GradePalavras.test.tsx:88` (rejeição de `onMarcar`) | PASS |
| AC4 CRIADA desabilita a marcação | `onMarcar` não chamado | `GradePalavras.test.tsx:99` | PASS |
| AC5 alterar palavra de FINALIZADA mostra "Alteração registrada em auditoria" e atualiza o resultado | aviso + resultado atualizado | Componente: `GradePalavras.test.tsx:111`. Nenhuma página renderiza a grade numa avaliação FINALIZADA | GAP (GAP-1) |

### P1: Resultado, áudio e envio

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 resultado com total, lidas, corretas, incorretas, não lidas, percentual, tempo, fase, nível | 9 campos | `src/features/avaliacoes/ResultadoAvaliacaoPage.test.tsx:116` (teste "shows all AC1 fields"); render em `ResultadoAvaliacaoPage.tsx:90-107` | PASS |
| AC2 envio automático com 3 tentativas (1 s, 2 s, 4 s) | esperas 1/2/4 s | `src/features/avaliacoes/useEnvioAudio.test.ts:23` (1ª tentativa); `:34` (espera de 1 s com timers falsos) | PARCIAL: as esperas de 2 s e 4 s não são asseridas (M8 sobreviveu) |
| AC3 3 falhas: áudio em memória, "O áudio não foi enviado", "Tentar enviar novamente", `beforeunload` | mensagem e botão | `useEnvioAudio.test.ts:60` (falhou, Blob, `beforeunload`); `:88` (`reenviar`); `:116` (sem bloqueio após enviado); `ResultadoAvaliacaoPage.test.tsx:132` | PASS (ver GAP-3: `beforeunload` também dispara sem áudio pendente) |
| AC4 player `<audio controls>` e "Baixar áudio" com `?download=true` | streaming + download | `ResultadoAvaliacaoPage.test.tsx:132` (fluxo de envio). Implementação: `ResultadoAvaliacaoPage.tsx:38,123-125` | PASS com SPEC_DEVIATION aceita; sem `.catch` no fetch do áudio (`:38-46`) |
| AC5 `classificacaoPendente` mostra "Classificação pendente: nenhuma regra cobre este resultado" | texto exato | `ResultadoAvaliacaoPage.test.tsx:180` | PASS |

### P1: Histórico e evolução

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 tabela paginada com as colunas do SDD §16 e ícone de play em avaliações com áudio | 16 colunas; ▶ só com `temAudio` | `src/features/historico/HistoricoTab.test.tsx:71` (cabeçalhos); `:98-99` (ícone só na linha com áudio); `:112,115` (paginação, `useHistorico(42, {}, 1)`) | PASS (o ícone é decorativo, sem ação de tocar; a spec só pede o ícone) |
| AC2 Entrada/Acompanhamento/Saída com corretas, classificação e evolução; ▲ verde, ▼ vermelho, "—" nulo | glifos e valores | `src/features/historico/EvolucaoCiclosTab.test.tsx:77` ("—"); `:81` - `'▲ +6'`; `:85` - `'▼ -4'` | PARCIAL: o teste se chama "green/red" mas nenhuma cor é asserida |
| AC3 comparação anual (SDD §15), "sem base" quando `semBase` | `25 (+13 / 108,33%)`; `(sem base)` | `src/features/historico/ComparacaoAnualTab.test.tsx:81` - `toHaveTextContent('25 (+13 / 108,33%)')`; `:96` - `'12 (sem base)'`; `:97` - `not.toHaveTextContent('0%')` | PASS. `semBase` é derivado de `absoluta===null && percentual===null` (`ComparacaoAnualTab.tsx:30`) porque o DTO não tem o campo: spec-precision gap |

### P2: Telas de cadastro do coordenador

| Critério | Resultado | `file:line` + asserção | Result |
| -------- | --------- | ---------------------- | ------ |
| AC1 salvar mostra "Salvo com sucesso" | texto exato | Anos letivos `AnoLetivoPage.test.tsx:79`; turmas `TurmasPage.test.tsx:83`; professores `ProfessoresPage.test.tsx:68`; alunos `AlunosCadastroPage.test.tsx:88`; usuários `UsuariosPage.test.tsx:73`; listas `ListasPalavrasPage.test.tsx:74`; regras `RegrasClassificacaoPage.test.tsx:94` | PASS |
| AC2 409/422 mostra a mensagem do código no campo ou no topo | topo (409) e campo (422) | `AnoLetivoPage.test.tsx:83-102`; `TurmasPage.test.tsx:101-114`; `ProfessoresPage.test.tsx:72-86`; `AlunosCadastroPage.test.tsx:129-141`; `UsuariosPage.test.tsx:98-111`; `ListasPalavrasPage.test.tsx:96-109`; `RegrasClassificacaoPage.test.tsx:104-105` | PASS (a mensagem exibida é o `detail` do backend, não um texto do front) |
| AC3 régua 0-60 com cor por fase, destaca lacunas e sobreposições antes do envio | "Lacuna: 15–19", "Sobreposição: 10–14" | `RegrasClassificacaoPage.test.tsx:54-57` (legenda por fase); `:69` - `'Lacuna: 15–19 acertos'`; `:82` - `'Sobreposição: 10–14 acertos'`; unitário `reguaClassificacao.test.ts:22,28,34,40,46`. M3 e M4 mortos | PASS (a cor em si não é asserida) |
| AC4 lista do 1º ano desabilita NAO_CANONICA | opção desabilitada | `ListasPalavrasPage.test.tsx:82` (teste "disables NAO_CANONICA for the 1º ano"); código `ListasPalavrasPage.tsx:159`. M5 morto | PASS |

**Status**: 27 requisitos FE: 20 PASS, 6 PARCIAL, 1 GAP (Marcar palavras AC5 / FE-18, inalcançável na UI). Mais o defeito de "ciclo atual" em AC1/AC2 de P1 (GAP-2). Spec-precision gaps sinalizados: 429 "para cima", `semBase` sem campo no DTO, evolução com `absoluta===0` (exibida como "0" neutro, sem seta), ▲/▼ sem definição de contraste.

---

## Edge Cases

- [x] Navegador sem `MediaRecorder`: "Navegador sem suporte à gravação. Use Chrome, Edge, Firefox ou Safari 17+" e `iniciar` não chamado - `useAvaliacaoExecucao.test.ts:85-98`. Parcial: o botão "Iniciar avaliação" NÃO fica desabilitado (a spec pede "desabilitar"); o código só mostra a mensagem (`ExecutarAvaliacaoPage.tsx:88`, `useAvaliacaoExecucao.ts:195-198`).
- [~] Aba sem foco continua contando: o cronômetro usa `performance.now()` + `setInterval` de 250 ms (`useAvaliacaoExecucao.ts:149-158`); navegadores limitam timers em aba oculta e não há teste nem ressincronização com o servidor ao voltar ao foco. Não verificado empiricamente.
- [x] Recarregar com EM_ANDAMENTO: "A gravação anterior foi interrompida", só Resetar e Finalizar - `ExecutarAvaliacaoPage.test.tsx:199-202`; `useAvaliacaoExecucao.test.ts:157-159`.
- [x] Ordem de MIME webm/opus, ogg/opus, mp4 - `src/media/recorder.test.ts:71` (ordem), `:79` (primeiro suportado), `:86` (nenhum -> `null`).
- [~] Teclado e `aria-label`: botões da grade têm `aria-label` (`GradePalavras.tsx:114`) e o cronômetro tem `role="timer"` e `aria-label`; os botões Pausar/Resetar/Finalizar usam o texto visível como nome acessível, sem `aria-label` explícito. Sem teste de navegação completa por teclado.

---

## Discrimination Sensor

Worktree descartável `git worktree add --detach` em HEAD `3c8691a`, `node_modules` por `ln -s`. Cada mutante foi aplicado com `sed`, testado com `vitest run` nos arquivos pertinentes e revertido com `git checkout --`. `git status --porcelain` do repositório real: vazio antes e vazio depois (idêntico); worktree removido (`git worktree list` mostra só o principal). Sem `git stash`.

| # | File:line | Mutação | Killed? |
| - | --------- | ------- | ------- |
| M1 | `src/features/alunos/cicloAtual.ts:23` | `return 'SAIDA'` -> `return 'ACOMPANHAMENTO'` (3 ciclos preenchidos) | Killed (`cicloAtual.test.ts` "returns SAIDA when all three cycles are present") |
| M2 | `src/features/avaliacoes/useAvaliacaoExecucao.ts:181-182` | removido o `GET` + `dispatch` do resync em 409 | Killed (teste FE-15, `:346`) |
| M3 | `src/features/cadastros/regras/reguaClassificacao.ts:42` | lacuna `fim: faixa.min - 1` -> `faixa.min` | Killed (3 testes: `reguaClassificacao.test.ts` x2 e `RegrasClassificacaoPage.test.tsx`) |
| M4 | `reguaClassificacao.ts:43` | ramo de sobreposição `else if (false)` | Killed (2 testes) |
| M5 | `src/features/cadastros/listas/ListasPalavrasPage.tsx:159` | `disabled={serie === 1}` -> `disabled={false}` | Killed (`ListasPalavrasPage.test.tsx:82`) |
| M6 | `useAvaliacaoExecucao.ts:180` | resync em qualquer 409 (remove a checagem do código `TRANSICAO_INVALIDA`) | **Survived** (8 arquivos, 42/42 passam) |
| M7 | `src/components/GradePalavras.tsx:17` | `NAO_LIDA: 'CORRETA'` -> `'PENDENTE'` | **Survived** (9/9 passam; transição NAO_LIDA -> CORRETA não testada) |
| M8 | `src/features/avaliacoes/useEnvioAudio.ts:7` | `[1000, 2000, 4000]` -> `[1000, 2000, 1000]` | **Survived** (42/42 passam; só a espera de 1 s é asserida) |
| M9 | `src/features/historico/HistoricoPage.tsx:35,46` | `RoleGate allow` das abas de evolução passa a incluir PROFESSOR | Killed (`HistoricoPage.test.tsx:38`) |

**Sensor depth**: lightweight ampliado (9 mutações, funcionalidade de regra de negócio e segurança de perfil).
**Result**: 6/9 mortos - **FAIL** (3 sobreviventes viram fix tasks: Fix 4, Fix 5, Fix 6).

---

## Code Quality

Amostra: `useAvaliacaoExecucao.ts`, `useEnvioAudio.ts`, `GradePalavras.tsx`, `ResultadoAvaliacaoPage.tsx`, `ConfigurarAvaliacaoPage.tsx`, `reguaClassificacao.ts`, `client.ts`, `router.tsx`, `AppLayout.tsx`, `HistoricoPage.tsx`.

| Principle | Status |
| --------- | ------ |
| Minimum code / sem abstração de uso único | OK: hooks e helpers pequenos; `reguaClassificacao.ts` é puro e enxuto |
| Surgical changes / sem scope creep | OK no front. Backend: 5 commits aditivos, só `GET` de listagem e `version`, cada um com IT |
| Matches patterns | OK: Context só para sessão, TanStack Query para servidor, `apiClient` único. Exceção: `ResultadoAvaliacaoPage.tsx:38` usa `fetch` direto, fora do `apiClient` (sem interceptor de 401, sem `.catch`); idem `useAvaliacaoExecucao.ts:132` sem `.catch` no carregamento inicial |
| Spec-anchored outcome check | PARCIAL: ver tabelas (6 PARCIAL, 1 GAP) |
| Per-layer Coverage Expectation (domínio 1:1 AC; rotas happy+edge+error) | PARCIAL: helpers e `apiClient` cobertos ponta a ponta; falta teste de página para o `confirm` do reset, para o limite superior de palavras e para marcação em FINALIZADA |
| Todo teste mapeia um AC/edge/Done-when | OK: cada `it` cita AC/FE/Edge Case no título ou comentário |
| Guidelines seguidas | `.specs/STATE.md` AD-007 (Vitest, RTL, cobertura >= 85%): cumprido (94,76% linhas). `coding-principles.md`: sem `any` fora de testes (`as any` e `eslint-disable` só em `*.test.tsx`); 2 `eslint-disable react-hooks/exhaustive-deps` justificados em comentário (`useAvaliacaoExecucao.ts:312`, `useEnvioAudio.ts:57`) |
| Tratamento de erro | GAP: `chamarTransicao` relança qualquer erro que não seja 409 `TRANSICAO_INVALIDA`; os botões chamam `void exec.pausar()` etc. (`ExecutarAvaliacaoPage.tsx:67-80`), então falha de rede ou 5xx vira promessa rejeitada sem feedback ao professor |
| Would senior engineer approve? | Não antes de GAP-1 a GAP-3 |

---

## Gate Check

- **Gate command**: `cd frontend && npm run lint && npm run test -- --coverage && npm run build` (Build level de tasks.md)
- **Result**: exit 0
  - `eslint .`: 0 erros, 2 warnings `react-refresh/only-export-components` (`src/auth/AuthContext.tsx:95`, `src/features/avaliacoes/ConfigurarAvaliacaoPage.tsx:43`)
  - `vitest run`: **37 arquivos, 161 testes, 161 passed, 0 failed, 0 skipped**
  - `tsc -b && vite build`: sucesso, 114 módulos, `dist/assets/index-dZiYswjd.js` 356,62 kB (gzip 107,29 kB)
- **Cobertura (v8)**: linhas 94,76% (941/993), statements 93,38%, branches 81,3%, funções 91,89%. Meta AD-007 >= 85% de linhas: cumprida. Menores: `ExecutarAvaliacaoPage.tsx` 73,3% linhas (linhas 47-48,73-80 não cobertas), `RegrasClassificacaoPage.tsx` 82,97%, `HistoricoTab.tsx` 87,5%.
- **E2E** (`frontend/e2e/fluxo-professor.spec.ts`): 2/2 executado pelo orquestrador em 2026-10-04 com backend + MySQL + Chrome reais; citado, não repetido. Cobre login -> busca -> configuração -> iniciar -> marcar 1 palavra -> tempo zerado -> resultado -> `<audio>` visível. Não cobre pausa, reset, microfone negado, reload, marcação em FINALIZADA, falha de envio.
- **Test count before feature**: 0 (greenfield; só o boilerplate)
- **Test count after**: 161 (+161)
- **Test integrity**: sem testes removidos nem `skip`/`only` nos `*.test.*` (nenhum `it.skip` encontrado na amostra de execução: 0 skipped)
- **Backend**: os ITs dos 5 commits aditivos não foram reexecutados por este Verifier (fora do gate `npm`); foram validados pelo orquestrador junto com a stack ao vivo.

---

## Fix Plans

### Fix 1 (Major): marcação em avaliação FINALIZADA não é acessível (Marcar palavras AC1/AC5, FE-18)
- **Root cause**: `GradePalavras` só é renderizado em `ExecutarAvaliacaoPage.tsx:101`, que navega para `/avaliacoes/{id}/resultado` assim que `status === 'FINALIZADA'` (`:38-42`). `ResultadoAvaliacaoPage.tsx` não renderiza a grade. `onAposMarcarFinalizada` (`GradePalavras.tsx:9,75`) não tem chamador. O aviso "Alteração registrada em auditoria" e a atualização do resultado nunca aparecem para o usuário; só passam no teste do componente isolado.
- **Fix task**: renderizar `GradePalavras` com `avaliacaoStatus="FINALIZADA"` em `ResultadoAvaliacaoPage` e passar `onAposMarcarFinalizada` para `refetch` da query `['avaliacao', id]`.
- **Verify**: teste de página do resultado que toca uma palavra, espera o PUT, o aviso e o novo `quantidadeCorretas`; E2E opcional.

### Fix 2 (Major): "ciclo atual" ignora o ano letivo ATIVO (P1 Buscar AC2, Configurar AC1; context.md "Ciclo atual")
- **Root cause**: `useAlunoResumo.ts:103` e `ConfigurarAvaliacaoPage.tsx:67` buscam `historico-avaliacoes?page=0` sem `anoLetivoId` e passam tudo a `calcularCicloAtual`. Um aluno com ENTRADA finalizada em 2025 aparece com ciclo atual ACOMPANHAMENTO em 2026, e o formulário pré-seleciona o ciclo errado. A primeira página também é limitada a 20 itens. `calcularCicloAtual` documenta "no ano ATIVO" (`cicloAtual.ts:10`) mas recebe itens de todos os anos, e os testes (`cicloAtual.test.ts`) só usam um ano.
- **Fix task**: filtrar por ano ativo (passar `anoLetivoId` do ano ATIVO ou filtrar por `item.anoLetivo`) antes de chamar o helper, e testar com itens de dois anos.
- **Verify**: teste de `useAlunoResumo` com histórico {2025: ENTRADA, 2026: vazio} esperando `ENTRADA`.

### Fix 3 (Minor/Major UX): `beforeunload` ativo sem áudio pendente (P1 Resultado AC3)
- **Root cause**: `useEnvioAudio.ts:62-66` bloqueia a saída sempre que `status !== 'enviado'`, inclusive em `idle` sem `blob` (abrir o resultado de uma avaliação antiga, ou recarregar a página de resultado). O navegador pergunta "sair da página?" sem nenhum áudio em risco. Sem teste para `idle`/sem blob.
- **Fix task**: bloquear só quando `blob !== null && status !== 'enviado'`.

### Fix 4 (Minor): mutante M7 - ciclo NAO_LIDA -> CORRETA sem teste (`GradePalavras.tsx:17`)
- **Fix task**: teste de 3 e 4 toques seguidos (INCORRETA -> NAO_LIDA -> CORRETA) e atalho em PENDENTE.

### Fix 5 (Minor): mutante M8 - esperas de 2 s e 4 s sem asserção (`useEnvioAudio.ts:7`)
- **Fix task**: com timers falsos, avançar 1 s, 2 s e 4 s e asserir o número de chamadas a `uploadAudio` em cada passo.

### Fix 6 (Minor): mutante M6 - 409 de outro código não deveria ressincronizar (`useAvaliacaoExecucao.ts:180`)
- **Fix task**: teste com 409 `AVALIACAO_JA_FINALIZADA` (ou outro) e `iniciar`/`pausar` rejeitando, sem `GET` de resync. Decidir também o que mostrar ao professor.

### Fix 7 (Minor): falha de transição sem feedback (`ExecutarAvaliacaoPage.tsx:67-80`)
- Rede/5xx em Iniciar/Pausar/Continuar/Resetar/Finalizar viram `unhandledrejection`. Capturar e mostrar mensagem. Em `finalizar`, um 409 com resync zera `blobGravado` (`AVALIACAO_CARREGADA` em `useAvaliacaoExecucao.ts:86`) e o áudio gravado se perde silenciosamente (spec Goal 3: "nenhuma gravação perdida silenciosamente").

### Observações de comportamento (não bloqueiam)
- **Navegação**: "Avaliar", "Meus alunos" e "Histórico" do PROFESSOR levam à mesma busca (`router.tsx:75-79`), e "Avaliações" do COORDENADOR também (`:79`), sem diferença visível entre os itens. A busca em `/alunos` e `/avaliar` é a mesma tela.
- **Gate de perfil nas rotas**: `/avaliar`, `/avaliacoes/nova` e `/avaliacoes/:id/executar` não têm `RoleGate` (`router.tsx:77,80-82`); o COORDENADOR, que não deve ver "Avaliar", alcança a execução pela busca de `/alunos` ("Configurar avaliação" em `ResumoAlunoPanel.tsx:60`) ou por URL.
- **MediaRecorder ausente**: o botão Iniciar continua habilitado (Edge Case pede desabilitar).
- **Cronômetro**: nenhuma ressincronização do tempo restante com o servidor após `continuar` ou reload; usa só o relógio local. O Goal "diverge <= 1 s do servidor" não é medido por teste algum.
- **Acessibilidade**: botões do cronômetro sem `aria-label` explícito; ▲/▼ do histórico com cor só no estilo inline e sem teste de contraste.
- **Lint**: 2 warnings `react-refresh/only-export-components`.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| FE-01, FE-02, FE-04, FE-05, FE-08, FE-11, FE-12, FE-14, FE-17, FE-19, FE-22, FE-23, FE-25, FE-26, FE-27 | Pending | Verified |
| FE-03, FE-09, FE-10, FE-13, FE-15, FE-16, FE-20, FE-21, FE-24 | Pending | Verified com ressalva (asserção parcial, ver tabelas e fixes 4-7) |
| FE-06, FE-07 | Pending | Needs Fix (Fix 2) |
| FE-18 | Pending | Needs Fix (Fix 1) |

---

## Summary

**Overall**: FAIL (Not Ready)

**Spec-anchored check**: 20 PASS, 6 PARCIAL, 1 GAP de critério; mais 1 defeito de dados no "ciclo atual"; 4 spec-precision gaps sinalizados
**Sensor**: 6/9 mutações mortas (3 sobreviventes: M6, M7, M8)
**Gate**: lint 0 erros (2 warnings), 161 passed / 0 failed / 0 skipped, build ok, cobertura 94,76% de linhas; E2E 2/2 citado

**What works**: login, 401/429, menus por perfil, busca com debounce, formulário de configuração, fluxo microfone -> iniciar -> gravar/contar -> finalizar por tempo, envio de áudio com reenvio, histórico paginado, comparação anual, telas de cadastro com 409/422, régua 0-60 com lacuna/sobreposição, gating de NAO_CANONICA e das abas de evolução por perfil.

**Issues found**: Fix 1 (marcação em FINALIZADA inalcançável), Fix 2 (ciclo atual sem filtro de ano ativo), Fix 3 (`beforeunload` sem áudio pendente), Fix 4-6 (mutantes), Fix 7 (falhas de transição sem feedback e áudio perdido em 409 no `finalizar`).

**Next steps**: rotear Fix 1 a Fix 7 para um implementador e reverificar (iteração 2 de 3).

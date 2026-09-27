# Frontend Web Specification

> Origem: SDD §6, §7, §8, §9.1, §13–§17, §20, §23, RNF003. Decisões: AD-002, AD-006. Consome as APIs das features `autenticacao-perfis`, `cadastros-base`, `regras-classificacao`, `banco-palavras`, `avaliacao`, `audio-avaliacao` e `historico-evolucao`.

## Problem Statement

O professor aplica a avaliação diante do aluno, com cronômetro visível, microfone gravando e marcação rápida das palavras. O coordenador mantém cadastros e regras. Sem uma interface web, o fluxo do SDD §20 não é executável: gravação de áudio (MediaRecorder) e pedido de permissão do microfone só acontecem no navegador.

## Goals

- [ ] O professor vai da busca do aluno ao resultado com áudio sem sair da tela de avaliação.
- [ ] O cronômetro exibido diverge no máximo 1 segundo do tempo contado pelo servidor ao finalizar.
- [ ] Nenhuma gravação é perdida silenciosamente: se o envio falha, o professor sempre recebe um aviso e a opção de tentar de novo.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Gráficos e dashboards | Evolução futura (SDD §25); o MVP usa tabelas |
| App mobile nativo | Não pedido; a interface web responsiva cobre tablets |
| Modo offline | Exige sincronização complexa; a avaliação precisa de conexão |
| Internacionalização | O sistema é só em pt-BR |
| Reconhecimento de fala no navegador | AD-002 |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Incluir o frontend nas specs | Sim, com React + TypeScript + Vite em `frontend/` | Opção escolhida pelo usuário; React é a primeira sugestão do SDD §23 (AD-006) | y |
| Navegadores suportados | Últimas 2 versões de Chrome, Edge e Firefox no desktop; Safari 17+ com MediaRecorder em `audio/mp4` | São os navegadores com MediaRecorder estável | n |
| Resolução mínima | 768 px de largura (tablet em paisagem ou desktop) | O professor aplica a avaliação em computador ou tablet | n |
| Interação para marcar palavras | Um toque alterna o status em ciclo: PENDENTE → CORRETA → INCORRETA → NAO_LIDA → CORRETA; atalhos de teclado C, I e N marcam a palavra em foco | Marcação rápida com uma mão | n |
| Representação visual (SDD §9.1) | Verde = CORRETA, vermelho = INCORRETA, cinza neutro = NAO_LIDA; PENDENTE também é neutro, mas com borda tracejada; cada status tem ícone e texto além da cor | Acessibilidade (WCAG 1.4.1: não usar só cor) | n |
| Armazenamento do token | Em memória + `sessionStorage` (sobrevive a recarregar a página, some ao fechar a aba) | Equilíbrio entre segurança e usabilidade | n |
| Contagem do cronômetro | Local, com `performance.now()`, e ressincronizada com a resposta do servidor a cada transição de status | Contagem fluida sem depender de polling | n |
| Envio do áudio | Automático após finalizar; até 3 tentativas com espera de 1 s, 2 s e 4 s; depois disso, botão manual "Tentar enviar novamente" | Cobre falhas passageiras de rede | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Login e navegação por perfil ⭐ MVP

**User Story**: Como usuário, quero entrar e ver só os menus do meu perfil.

**Why P1**: É a porta de entrada.

**Acceptance Criteria**:
1. WHEN o usuário envia e-mail e senha válidos na tela de login THEN a aplicação SHALL guardar o token e abrir "Avaliar" (PROFESSOR) ou "Alunos" (COORDENADOR).
2. IF a API retornar 401 no login THEN a aplicação SHALL mostrar "E-mail ou senha inválidos" sem limpar o campo de e-mail.
3. IF a API retornar 429 no login THEN a aplicação SHALL mostrar "Conta bloqueada. Tente novamente em N minutos", com N calculado a partir de `Retry-After`.
4. WHEN qualquer requisição autenticada retornar 401 THEN a aplicação SHALL descartar o token e voltar para a tela de login, preservando a rota de retorno.
5. WHILE o perfil for PROFESSOR, a aplicação SHALL mostrar apenas os menus Avaliar, Meus alunos e Histórico.
6. WHILE o perfil for COORDENADOR, a aplicação SHALL mostrar os menus Alunos, Turmas, Professores, Anos letivos, Regras de classificação, Listas de palavras, Usuários e Avaliações, e SHALL NOT mostrar Avaliar.

**Independent Test**: Um professor faz login e não vê o menu "Regras de classificação".

---

### P1: Buscar aluno e ver resumo ⭐ MVP

**User Story**: Como professor, quero buscar o aluno e ver o resumo dele antes de configurar a avaliação (SDD §6.1).

**Why P1**: É o primeiro passo do SDD §20.

**Acceptance Criteria**:
1. WHEN o professor digita 2 ou mais caracteres no campo de busca THEN a aplicação SHALL chamar a busca 300 ms após a última tecla e listar nome, turma e série.
2. WHEN o professor seleciona um aluno THEN a aplicação SHALL mostrar os campos do resumo (EVO-01): nome, turma, professor, ano letivo, série, ciclo atual, última classificação, as 5 avaliações mais recentes e a evolução no ano.
3. WHEN a busca não encontra nada THEN a aplicação SHALL mostrar "Nenhum aluno encontrado".

**Independent Test**: Digitar "jo" e selecionar João; o painel de resumo aparece com o ciclo atual.

---

### P1: Configurar a avaliação ⭐ MVP

**User Story**: Como professor, quero escolher tipo, ciclo, data, tempo e palavras, para preparar a avaliação (SDD §6.2).

**Why P1**: É obrigatório antes de iniciar.

**Acceptance Criteria**:
1. WHEN o formulário abre THEN a aplicação SHALL preencher `ciclo` com o ciclo atual do resumo, `data` com hoje e `tempo` com 60 segundos.
2. WHEN o professor escolhe o tipo de leitura THEN a aplicação SHALL listar as listas de palavras da série do aluno e daquele tipo, e oferecer a opção "Digitar palavras" (ou "Digitar texto" em TEXTO_CURTO).
3. WHILE o professor estiver digitando palavras, a aplicação SHALL mostrar o contador "N palavras (mín. X, máx. Y)" com os limites da configuração da série e SHALL desabilitar "Criar avaliação" enquanto N estiver fora do intervalo.
4. IF a API retornar 422 THEN a aplicação SHALL mostrar a mensagem de cada campo inválido ao lado do campo correspondente.
5. WHEN a avaliação é criada THEN a aplicação SHALL abrir a tela de execução com as palavras em grade, na ordem.

**Independent Test**: Com 14 palavras numa série de mínimo 15, o botão fica desabilitado; com 15, cria a avaliação.

---

### P1: Executar a avaliação com cronômetro e gravação ⭐ MVP

**User Story**: Como professor, quero iniciar a avaliação e ter o cronômetro e a gravação funcionando juntos, controlando pausa, reset e fim (SDD §7, §8).

**Why P1**: É o núcleo da experiência (RF007, RF008, RNF003).

**Acceptance Criteria**:
1. WHEN o professor clica em "Iniciar avaliação" THEN a aplicação SHALL pedir acesso ao microfone (`getUserMedia({audio: true})`) antes de chamar a API `iniciar`.
2. IF o acesso ao microfone for negado ou não houver microfone THEN a aplicação SHALL mostrar "Permita o acesso ao microfone para iniciar a avaliação" e SHALL NOT chamar `iniciar` (a avaliação continua CRIADA).
3. WHEN o microfone é liberado e `iniciar` retorna 200 THEN a aplicação SHALL iniciar a gravação (MediaRecorder) e a contagem regressiva no formato mm:ss, ao mesmo tempo.
4. WHEN o professor clica em "Pausar" THEN a aplicação SHALL chamar `pausar` e pausar o cronômetro e a gravação; "Continuar" SHALL fazer o caminho inverso com `continuar`.
5. WHEN o professor clica em "Resetar" e confirma na caixa de diálogo THEN a aplicação SHALL chamar `resetar`, descartar a gravação, voltar o cronômetro ao tempo configurado e mostrar todas as palavras como PENDENTE.
6. WHEN o cronômetro chega a 00:00 ou o professor clica em "Finalizar" THEN a aplicação SHALL parar a gravação, chamar `finalizar` (com `motivo=TEMPO_ESGOTADO` no primeiro caso) e mostrar o resultado.
7. The system SHALL habilitar os botões de acordo com o status: CRIADA → Iniciar; EM_ANDAMENTO → Pausar, Resetar, Finalizar; PAUSADA → Continuar, Resetar, Finalizar; FINALIZADA e CANCELADA → nenhum botão do cronômetro.
8. WHILE a avaliação estiver EM_ANDAMENTO, a aplicação SHALL mostrar um indicador "Gravando" com um ponto vermelho pulsante.
9. IF a API responder 409 `TRANSICAO_INVALIDA` THEN a aplicação SHALL recarregar a avaliação e sincronizar os botões e o cronômetro com o status do servidor.

**Independent Test**: Negar o microfone mantém CRIADA; liberar, esperar o tempo zerar e ver o resultado aparecer automaticamente.

---

### P1: Marcar palavras ⭐ MVP

**User Story**: Como professor, quero tocar nas palavras para marcá-las enquanto o aluno lê (SDD §9, AD-002).

**Why P1**: Sem marcação não há resultado.

**Acceptance Criteria**:
1. WHILE a avaliação estiver EM_ANDAMENTO, PAUSADA ou FINALIZADA, WHEN o professor toca numa palavra THEN a aplicação SHALL avançar o status no ciclo PENDENTE → CORRETA → INCORRETA → NAO_LIDA → CORRETA, atualizar a cor na hora e enviar o novo status à API.
2. The system SHALL exibir CORRETA em verde com ícone ✓, INCORRETA em vermelho com ícone ✗, NAO_LIDA em cinza com ícone –, e PENDENTE em cinza com borda tracejada, com contraste mínimo de 4.5:1.
3. IF o envio de uma marcação falhar THEN a aplicação SHALL voltar a palavra ao status anterior e mostrar "Não foi possível salvar a marcação".
4. WHILE a avaliação estiver CRIADA, a aplicação SHALL desabilitar a marcação das palavras.
5. WHEN o professor altera uma palavra de uma avaliação FINALIZADA THEN a aplicação SHALL mostrar o aviso "Alteração registrada em auditoria" e atualizar o resultado exibido.

**Independent Test**: Tocar duas vezes numa palavra deixa ela vermelha com ✗, e a API recebe INCORRETA.

---

### P1: Resultado, áudio e envio ⭐ MVP

**User Story**: Como professor, quero ver o resultado, ouvir e baixar o áudio logo após finalizar (SDD §7.4, §8, §13).

**Why P1**: É o fechamento do fluxo (RF010, RF015).

**Acceptance Criteria**:
1. WHEN a avaliação é finalizada THEN a aplicação SHALL mostrar total, lidas, corretas, incorretas, não lidas, percentual de acerto, tempo utilizado, fase e nível.
2. WHEN a gravação termina THEN a aplicação SHALL enviar o áudio automaticamente, com até 3 tentativas (esperas de 1 s, 2 s e 4 s).
3. IF as 3 tentativas falharem THEN a aplicação SHALL manter o áudio em memória, mostrar "O áudio não foi enviado" com o botão "Tentar enviar novamente" e pedir confirmação ao sair da página (`beforeunload`).
4. WHEN o áudio está salvo THEN a aplicação SHALL mostrar um player (`<audio controls>`) que usa o endpoint de streaming e um botão "Baixar áudio" que usa `?download=true`.
5. IF o resultado vier com `classificacaoPendente = true` THEN a aplicação SHALL mostrar "Classificação pendente: nenhuma regra cobre este resultado".

**Independent Test**: Simular uma falha de rede no envio e ver o botão "Tentar enviar novamente"; ao restaurar a rede, o envio funciona e o player aparece.

---

### P1: Histórico e evolução do aluno ⭐ MVP

**User Story**: Como professor ou coordenador, quero ver o histórico, a evolução por ciclo e a comparação anual em tabelas (SDD §14–§16).

**Why P1**: Entrega o valor longitudinal do sistema.

**Acceptance Criteria**:
1. WHEN o usuário abre o histórico de um aluno THEN a aplicação SHALL mostrar uma tabela paginada com as colunas do SDD §16 e um ícone de play nas avaliações com áudio.
2. WHEN o usuário escolhe um tipo de leitura na aba "Evolução" THEN a aplicação SHALL mostrar Entrada, Acompanhamento e Saída com corretas, classificação e evolução, usando ▲ verde para positivo, ▼ vermelho para negativo e "—" para nulo.
3. WHEN o usuário abre a aba "Comparação anual" THEN a aplicação SHALL mostrar a tabela do SDD §15 com as evoluções e exibir "sem base" quando `semBase = true`.

**Independent Test**: Um aluno com dados de 2026 e 2027 mostra a comparação com +13 / 108,33% na Entrada.

---

### P2: Telas de cadastro do coordenador

**User Story**: Como coordenador, quero telas para manter anos letivos e limites, professores, turmas, alunos e matrículas, usuários, regras de classificação e listas de palavras.

**Why P2**: A API já permite o cadastro; as telas dão autonomia, mas o fluxo do professor tem prioridade.

**Acceptance Criteria**:
1. WHEN o coordenador salva um formulário válido THEN a aplicação SHALL chamar o endpoint correspondente e mostrar "Salvo com sucesso".
2. IF a API retornar 409 ou 422 THEN a aplicação SHALL mostrar a mensagem do código de erro retornado ao lado do campo ou no topo do formulário.
3. WHEN o coordenador edita as faixas de classificação de uma série THEN a aplicação SHALL mostrar uma pré-visualização da régua de 0 a 60 acertos com a cor de cada fase e SHALL destacar lacunas e sobreposições antes do envio.
4. WHEN o coordenador cadastra uma lista do 1º ano THEN a aplicação SHALL desabilitar a opção NAO_CANONICA.

**Independent Test**: Criar uma turma e um aluno pelas telas e encontrá-los na busca do professor.

---

## Edge Cases

- IF o navegador não suportar `MediaRecorder` THEN a aplicação SHALL mostrar "Navegador sem suporte à gravação. Use Chrome, Edge, Firefox ou Safari 17+" e desabilitar "Iniciar avaliação".
- WHEN a aba perde o foco durante a avaliação THEN a aplicação SHALL continuar contando, porque o tempo real vem do servidor.
- IF a página for recarregada com a avaliação EM_ANDAMENTO THEN a aplicação SHALL recarregar o status do servidor, mostrar "A gravação anterior foi interrompida" e oferecer apenas Resetar ou Finalizar.
- The system SHALL escolher o MIME da gravação na ordem `audio/webm;codecs=opus`, `audio/ogg;codecs=opus`, `audio/mp4`, usando o primeiro suportado.
- The system SHALL garantir navegação completa por teclado e rótulos acessíveis (aria-label) em todos os botões do cronômetro e da grade de palavras.

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | Espelha as validações do servidor (FE-10); o servidor continua sendo a referência |
| Failure / partial-failure | Envio de áudio com repetição automática e manual (FE-20, FE-21); marcação desfeita se falhar (FE-17) |
| Idempotency / duplicates | Os comandos de transição são idempotentes no servidor (AVA-14); os botões ficam desabilitados enquanto a requisição está em andamento |
| Auth boundaries | FE-01..FE-06; o menu segue o perfil |
| Concurrency / ordering | Sincronização em 409 (FE-15) |
| Data lifecycle | Token em `sessionStorage`; áudio só em memória até o envio |
| Observability | N/A because não há telemetria de frontend no MVP |
| External-dependency failure | Microfone negado ou ausente (FE-12); navegador sem MediaRecorder |
| State-transition integrity | Botões por status (FE-14) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| FE-01 | P1: Login e redirecionamento por perfil | - | Pending |
| FE-02 | P1: Mensagens de 401 e 429 no login | - | Pending |
| FE-03 | P1: Expiração do token leva ao login | - | Pending |
| FE-04 | P1: Menus por perfil | - | Pending |
| FE-05 | P1: Busca com espera de 300 ms | - | Pending |
| FE-06 | P1: Painel de resumo | - | Pending |
| FE-07 | P1: Valores padrão do formulário | - | Pending |
| FE-08 | P1: Listas filtradas por série e tipo | - | Pending |
| FE-09 | P1: Contador de palavras e limites | - | Pending |
| FE-10 | P1: Erros 422 por campo | - | Pending |
| FE-11 | P1: Pedido de permissão do microfone antes de iniciar | - | Pending |
| FE-12 | P1: Microfone negado | - | Pending |
| FE-13 | P1: Cronômetro e gravação sincronizados | - | Pending |
| FE-14 | P1: Botões por status | - | Pending |
| FE-15 | P1: Sincronização em 409 | - | Pending |
| FE-16 | P1: Marcação por toque em ciclo | - | Pending |
| FE-17 | P1: Cores, ícones, contraste e desfazer em falha | - | Pending |
| FE-18 | P1: Aviso de auditoria após finalização | - | Pending |
| FE-19 | P1: Painel de resultado | - | Pending |
| FE-20 | P1: Envio automático do áudio com repetição | - | Pending |
| FE-21 | P1: Envio manual e proteção ao sair da página | - | Pending |
| FE-22 | P1: Player e download | - | Pending |
| FE-23 | P1: Tabelas de histórico, evolução e comparação | - | Pending |
| FE-24 | P1: Recarregar a página durante a avaliação | - | Pending |
| FE-25 | P1: Navegador sem MediaRecorder | - | Pending |
| FE-26 | P2: Telas de cadastro | - | Pending |
| FE-27 | P2: Editor de faixas com pré-visualização | - | Pending |

**Coverage:** 27 total, 0 mapped to tasks, 27 unmapped ⚠️

---

## Success Criteria

- [ ] Teste E2E (Playwright com microfone falso) cobre o fluxo completo do SDD §20: login → busca → configuração → iniciar → marcar → tempo zerado → resultado → áudio.
- [ ] Cobertura de linhas no Vitest ≥ 85% (AD-007).
- [ ] Um professor novo conclui uma avaliação de 60 s sem instruções em menos de 3 minutos no total.

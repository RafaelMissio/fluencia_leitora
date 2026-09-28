# Avaliação de Leitura Specification

> Origem: SDD §6, §7, §8, §9, §13, §20, RF006, RF007, RF008, RF009, RF010, RF011, RF015, RNF003, RNF004, RNF005. Decisões: AD-001, AD-002, AD-003, AD-004, AD-005.

## Problem Statement

O professor precisa aplicar uma avaliação cronometrada de leitura (palavras, pseudopalavras ou texto curto), marcar cada palavra como correta, incorreta ou não lida e obter na hora o resultado e a classificação do aluno. A avaliação passa por vários status (criada, em andamento, pausada, finalizada, cancelada), e alterações depois da finalização precisam ficar registradas.

## Goals

- [ ] O professor cria, executa e finaliza uma avaliação só pela API, seguindo o fluxo do SDD §20.
- [ ] Ao finalizar, o resultado (SDD §13) e a classificação ficam gravados sem cálculo manual.
- [ ] Toda alteração numa avaliação FINALIZADA gera um registro de auditoria com usuário, data/hora, valor anterior e novo.
- [ ] Nenhuma transição de status inválida é aceita.
- [ ] Depois de finalizar, o professor consegue baixar o áudio gravado pelo navegador (RF015), sem cálculo manual de onde o arquivo está.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Gravação em si (captura do microfone) e reprodução no navegador | Feature `frontend-web`; esta feature só recebe os bytes já gravados e os devolve de volta |
| Armazenamento físico dos bytes de áudio (disco, formato, limite de tamanho) | Feature `audio-avaliacao` (`AudioStoragePort`); esta feature só chama `armazenar`/`recuperar` e persiste a referência em `avaliacao_audio` (deferido de `audio-avaliacao/spec.md`, Out of Scope) |
| Cronômetro visual e microfone no navegador | Feature `frontend-web` |
| Reconhecimento automático de fala | AD-002; é evolução futura (SDD §25) |
| Histórico, evolução e comparação anual | Feature `historico-evolucao` |
| Avaliação feita pelo COORDENADOR | O SDD §17 dá essa ação só ao professor |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Marcação das palavras (SDD §24 item 5) | Manual, pelo professor | Opção escolhida pelo usuário (AD-002) | y |
| Mais de uma avaliação por tipo e ciclo (SDD §24 item 7) | Permitida; a evolução usa a mais recente | Opção escolhida pelo usuário (AD-004) | y |
| Origem das palavras | Exatamente uma entre: `listaPalavrasId`, `palavras[]` digitadas ou `texto` digitado (só em TEXTO_CURTO); o conteúdo é copiado para a avaliação | Opção "banco + digitação" escolhida pelo usuário | y |
| Quem controla o tempo | O servidor soma o tempo de execução pelos timestamps de iniciar, pausar e continuar; `tempoUtilizado = min(tempo somado, tempoConfigurado)` | O cliente não é confiável para o tempo; o frontend só exibe a contagem regressiva | y |
| Finalização automática no tempo zero (SDD §7.4) | Finalização preguiçosa: se qualquer comando chegar numa avaliação EM_ANDAMENTO com tempo somado ≥ tempo configurado, o servidor finaliza antes de processar o comando; além disso, uma rotina agendada de hora em hora finaliza avaliações EM_ANDAMENTO paradas há mais de 24h | Garante o encerramento mesmo se o navegador fechar ou perder a conexão e nenhum novo comando chegar (decisão do usuário) | y |
| Regra do Resetar (SDD §7.3, "conforme regra definida") | Volta para CRIADA, zera o tempo somado e volta todas as palavras para PENDENTE | É a interpretação mais simples de "reiniciar o estado" (decisão do usuário) | y |
| Palavras PENDENTE na finalização | Viram NAO_LIDA | O SDD §13 conta "não lidas" como tudo o que não foi marcado | y |
| Marcação depois de finalizar (SDD §9, "durante ou após") | Permitida em FINALIZADA; cada mudança gera auditoria e recalcula o resultado | Atende o SDD §9 e o RNF004 ao mesmo tempo (decisão do usuário) | y |
| Regra usada no recálculo depois de finalizar | As faixas ativas no momento do recálculo; a auditoria guarda a classificação anterior e a nova | O modelo não guarda cópia da versão da regra (decisão do usuário) | y |
| Percentual de acerto | `corretas / total × 100`, com 2 casas decimais e arredondamento HALF_UP | Bate com o exemplo do SDD §13 (9/20 = 45%) | y |
| Tempo configurável | De 10 a 600 segundos, padrão 60 | O SDD dá 60 s como exemplo; o limite evita valores absurdos | y |
| Data da avaliação | Dentro do período do ano letivo ATIVO e nunca no futuro | Coerência do histórico | y |
| Cópias na avaliação | `turmaId`, `turmaNome`, `serie`, `professorId`, `professorNome` e `anoLetivoId` copiados da matrícula ao criar | RNF005 e AD-005 | y |
| Palavras digitadas sem `tipoPalavra` | Aceitas com `tipoPalavra = null`; a restrição do 1º ano só vale quando o tipo é informado | Exigir o tipo em cada palavra digitada seria trabalho demais para o professor | y |
| Cancelamento | Permitido em qualquer status, exceto CANCELADA, com justificativa de 10 a 500 caracteres; cancelar uma FINALIZADA gera auditoria | Permite corrigir uma avaliação feita por engano (RNF004; decisão do usuário) | y |
| Quando o áudio pode ser enviado (RF015, deferido de `audio-avaliacao/spec.md`) | Só numa avaliação `FINALIZADA`, um único `POST` por avaliação | O SDD §8 grava continuamente durante a leitura e só oferece reproduzir/baixar depois de finalizar; um único arquivo por avaliação mantém o modelo simples (decisão do usuário) | y |
| Reenvio do áudio | Não permitido; um segundo `POST` para a mesma avaliação retorna 409 | Mesmo padrão de imutabilidade do AD-003 (áudio não expira) e do RNF006 (decisão do usuário) | y |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Criar avaliação ⭐ MVP

**User Story**: Como professor, quero configurar uma avaliação (aluno, tipo, ciclo, data, tempo e palavras), para aplicá-la ao aluno (SDD §6.2).

**Why P1**: É o início do fluxo principal.

**Acceptance Criteria**:
1. WHEN o professor envia `POST /api/v1/avaliacoes` com `alunoId`, `tipoLeitura`, `ciclo`, `dataAvaliacao`, `tempoSegundos` e uma única fonte de conteúdo THEN o sistema SHALL criar a avaliação com status `CRIADA`, as palavras copiadas com ordem 1..n e status `PENDENTE`, as cópias da matrícula ativa e retornar 201.
2. IF o aluno não tiver matrícula ativa no ano letivo ATIVO, ou a matrícula estiver com `anoFinalizado=true`, ou o aluno estiver inativo THEN o sistema SHALL retornar 422 com código `ALUNO_NAO_AVALIAVEL`.
3. IF a quantidade de palavras estiver fora do mín./máx. configurado para o ano letivo e a série da matrícula THEN o sistema SHALL retornar 422 com código `QUANTIDADE_PALAVRAS_FORA_DO_LIMITE` e os valores `minimo`, `maximo` e `informado`.
4. IF nenhuma fonte de conteúdo, ou mais de uma, for informada, ou se `texto` for usado com tipo diferente de TEXTO_CURTO THEN o sistema SHALL retornar 422 com código `CONTEUDO_INVALIDO`.
5. IF a lista informada for de série ou tipo de leitura diferentes dos da avaliação THEN o sistema SHALL retornar 422 com código `LISTA_INCOMPATIVEL`.
6. IF `tempoSegundos` estiver fora de 10–600 THEN o sistema SHALL retornar 422.
7. IF `dataAvaliacao` for futura ou estiver fora do período do ano letivo ATIVO THEN o sistema SHALL retornar 422.
8. IF a série for 1 e alguma palavra digitada tiver `tipoPalavra = NAO_CANONICA` THEN o sistema SHALL retornar 422 com código `NAO_CANONICA_PROIBIDA_1_ANO`.
9. WHEN o conteúdo é um `texto` digitado THEN o sistema SHALL tokenizá-lo com a mesma regra da feature `banco-palavras` (PAL-08).
10. IF alguma palavra digitada estiver vazia, tiver mais de 60 caracteres ou tiver caracteres diferentes de letras e hífen THEN o sistema SHALL retornar 422 com a posição da palavra.

**Independent Test**: Criar uma avaliação do 1º ano com 15 palavras digitadas (201); com 14 palavras, receber 422 com mínimo 15.

---

### P1: Controlar a execução (status) ⭐ MVP

**User Story**: Como professor, quero iniciar, pausar, continuar, resetar e finalizar a avaliação, para controlar o tempo de leitura (SDD §7).

**Why P1**: É o núcleo do RF007 e do fluxo do SDD §20.

**Acceptance Criteria**:
1. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/iniciar` numa avaliação `CRIADA` THEN o sistema SHALL mudar para `EM_ANDAMENTO`, gravar `iniciadoEm` e começar a contar o tempo.
2. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/pausar` numa avaliação `EM_ANDAMENTO` THEN o sistema SHALL mudar para `PAUSADA` e somar o trecho de tempo em andamento ao total.
3. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/continuar` numa avaliação `PAUSADA` THEN o sistema SHALL voltar para `EM_ANDAMENTO` sem contar o tempo em que ficou pausada.
4. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/resetar` numa avaliação `EM_ANDAMENTO` ou `PAUSADA` THEN o sistema SHALL voltar para `CRIADA`, zerar o tempo somado, limpar `iniciadoEm` e voltar todas as palavras para `PENDENTE`.
5. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/finalizar` numa avaliação `EM_ANDAMENTO` ou `PAUSADA` THEN o sistema SHALL mudar para `FINALIZADA`, gravar `finalizadoEm`, gravar `tempoUtilizado = min(tempo somado, tempoConfigurado)` em segundos inteiros, converter as palavras `PENDENTE` em `NAO_LIDA`, calcular o resultado e a classificação e retornar 200 com o resultado.
6. IF uma transição for pedida a partir de um status que não a permite (fora dos ACs 1–5 e da tabela de status) THEN o sistema SHALL retornar 409 com código `TRANSICAO_INVALIDA`, o `statusAtual` e a ação pedida.
7. WHEN uma ação é repetida e a avaliação já está no status que ela produziria (ex.: `finalizar` numa FINALIZADA, `pausar` numa PAUSADA) THEN o sistema SHALL retornar 200 com o estado atual, sem alterá-lo.
8. WHILE uma avaliação estiver `EM_ANDAMENTO` com tempo somado maior ou igual a `tempoConfigurado`, WHEN qualquer comando chegar para ela THEN o sistema SHALL finalizá-la antes com `tempoUtilizado = tempoConfigurado` e só então processar o comando.
9. The system SHALL registrar em log INFO cada transição com `avaliacaoId`, status de origem, status de destino e `usuarioId`.

**Independent Test**: Iniciar, pausar por 5 s, continuar e finalizar; conferir que `tempoUtilizado` não inclui os 5 s pausados.

---

### P1: Marcar palavras ⭐ MVP

**User Story**: Como professor, quero marcar cada palavra como correta, incorreta ou não lida enquanto o aluno lê, para registrar o desempenho (RF009, SDD §9).

**Why P1**: O resultado depende dessa marcação.

**Acceptance Criteria**:
1. WHILE a avaliação estiver `EM_ANDAMENTO` ou `PAUSADA`, WHEN o professor envia `PUT /api/v1/avaliacoes/{id}/palavras/{ordem}` com `status` ∈ {PENDENTE, CORRETA, INCORRETA, NAO_LIDA} THEN o sistema SHALL gravar o status da palavra e retornar 200.
2. WHEN o professor envia `PUT /api/v1/avaliacoes/{id}/palavras` com uma lista de `{ordem, status}` THEN o sistema SHALL gravar tudo numa única transação, ou nada se algum item for inválido.
3. IF a avaliação estiver `CRIADA` ou `CANCELADA` THEN o sistema SHALL retornar 409 com código `MARCACAO_NAO_PERMITIDA`.
4. IF `ordem` não existir na avaliação THEN o sistema SHALL retornar 404.
5. IF a avaliação estiver `FINALIZADA` e o status pedido for `PENDENTE` THEN o sistema SHALL retornar 422.
6. WHILE a avaliação estiver `FINALIZADA`, WHEN o professor muda o status de uma palavra THEN o sistema SHALL gravar a mudança, gerar um registro de auditoria (`usuarioId`, data/hora, `ordem`, status anterior, status novo), recalcular resultado e classificação e registrar a classificação anterior e a nova na auditoria.
7. IF o status enviado for igual ao atual THEN o sistema SHALL retornar 200 sem gerar auditoria.

**Independent Test**: Numa avaliação finalizada, mudar a palavra 3 de NAO_LIDA para CORRETA; `corretas` aumenta em 1 e aparece 1 registro de auditoria.

---

### P1: Calcular resultado e classificar ⭐ MVP

**User Story**: Como professor, quero ver o resultado e a classificação logo após finalizar (RF010, RF011, SDD §13).

**Why P1**: É o principal valor entregue ao professor.

**Acceptance Criteria**:
1. The system SHALL calcular `total` = número de palavras, `corretas`, `incorretas`, `naoLidas`, `lidas = corretas + incorretas` e `percentualAcerto = corretas / total × 100` com 2 casas e HALF_UP.
2. WHEN uma avaliação com total 20, corretas 9, incorretas 4 e não lidas 7 é finalizada THEN o sistema SHALL gravar lidas 13 e percentualAcerto 45.00.
3. WHEN a avaliação é finalizada THEN o sistema SHALL gravar a `fase` e o `nivel` obtidos pela classificação (feature `regras-classificacao`) usando a `serie` copiada e as `corretas`.
4. WHEN a avaliação do exemplo do AC 2 é da série 1 THEN o sistema SHALL gravar a fase LEITOR_INICIANTE com nível nulo.
5. IF a classificação retornar "sem classificação" THEN o sistema SHALL finalizar mesmo assim, com `fase` e `nivel` nulos e `classificacaoPendente = true` na resposta.
6. WHEN o professor envia `GET /api/v1/avaliacoes/{id}` THEN o sistema SHALL retornar os dados de configuração, o status, as cópias, as palavras com status e, se FINALIZADA, o resultado completo do SDD §13.

**Independent Test**: Finalizar o exemplo do SDD §13 e comparar o resultado campo a campo.

---

### P1: Cancelar avaliação ⭐ MVP

**User Story**: Como professor, quero cancelar uma avaliação feita por engano, deixando registrado o motivo (RNF004).

**Why P1**: Sem cancelamento, avaliações erradas iriam para a evolução.

**Acceptance Criteria**:
1. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/cancelar` com `justificativa` de 10 a 500 caracteres numa avaliação que não está `CANCELADA` THEN o sistema SHALL mudar para `CANCELADA` e gerar um registro de auditoria com o status anterior e a justificativa.
2. IF a justificativa faltar ou estiver fora de 10–500 caracteres THEN o sistema SHALL retornar 422.
3. The system SHALL NOT aceitar nenhuma transição a partir de `CANCELADA`, retornando 409 `TRANSICAO_INVALIDA`.

**Independent Test**: Cancelar uma avaliação FINALIZADA e conferir a auditoria.

---

### P1: Enviar e baixar o áudio ⭐ MVP

**User Story**: Como professor, quero enviar o áudio gravado pelo navegador ao finalizar a avaliação e poder baixá-lo depois, para ouvir a leitura do aluno (SDD §8, RF008, RF015).

**Why P1**: RF015 é requisito funcional explícito do SDD; sem este endpoint o `AudioStoragePort` de `audio-avaliacao` fica sem consumidor.

**Acceptance Criteria**:
1. WHEN o professor envia `POST /api/v1/avaliacoes/{id}/audio` com o conteúdo binário e o `mimeType` numa avaliação `FINALIZADA` que ainda não tem áudio THEN o sistema SHALL chamar `AudioStoragePort.armazenar`, gravar um registro em `avaliacao_audio` (`avaliacaoId`, referência retornada, `mimeType`, `tamanhoBytes`) e retornar 201.
2. IF a avaliação não estiver `FINALIZADA` THEN o sistema SHALL retornar 409 com código `AUDIO_ENVIO_NAO_PERMITIDO` e o `statusAtual`.
3. IF a avaliação já tiver um áudio gravado THEN o sistema SHALL retornar 409 com código `AUDIO_JA_ENVIADO`.
4. IF `AudioStoragePort.armazenar` lançar `AudioFormatoInvalidoException` THEN o sistema SHALL retornar 422 com código `AUDIO_FORMATO_INVALIDO`.
5. IF `AudioStoragePort.armazenar` lançar `AudioTamanhoInvalidoException` THEN o sistema SHALL retornar 422 com código `AUDIO_TAMANHO_INVALIDO`.
6. WHEN o professor dono da avaliação ou um COORDENADOR envia `GET /api/v1/avaliacoes/{id}/audio` numa avaliação com áudio gravado THEN o sistema SHALL chamar `AudioStoragePort.recuperar` e retornar os bytes com `Content-Type` igual ao `mimeType` gravado e status 200.
7. IF a avaliação não tiver áudio gravado THEN o sistema SHALL retornar 404.

**Independent Test**: Finalizar uma avaliação, enviar um áudio (201), baixá-lo e comparar os bytes; enviar de novo para a mesma avaliação retorna 409.

---

### P2: Consultar auditoria

**User Story**: Como coordenador, quero ver o que mudou numa avaliação finalizada, para garantir a confiabilidade dos dados (RNF004).

**Why P2**: A auditoria é gravada no P1; a consulta pode vir depois.

**Acceptance Criteria**:
1. WHEN um COORDENADOR ou o professor da avaliação envia `GET /api/v1/avaliacoes/{id}/auditoria` THEN o sistema SHALL retornar os registros em ordem cronológica com `usuario`, `dataHora`, `acao`, `valorAnterior`, `valorNovo` e `justificativa`.

**Independent Test**: Depois de 2 alterações, a consulta retorna 2 registros em ordem.

---

## Edge Cases

- IF duas requisições alterarem a mesma avaliação ao mesmo tempo THEN o sistema SHALL aceitar a primeira e retornar 409 com código `CONFLITO_DE_VERSAO` para a segunda (lock otimista via `version`).
- IF o professor tentar criar ou operar uma avaliação de um aluno que não é dele THEN o sistema SHALL retornar 404 (AUTH-09).
- WHEN o professor da matrícula muda depois de a avaliação ter sido criada THEN o sistema SHALL manter a avaliação com a cópia do professor original, e ela SHALL continuar acessível ao professor original e ao COORDENADOR.
- WHEN todas as palavras estão PENDENTE na finalização THEN o sistema SHALL gravar corretas 0, não lidas = total e percentualAcerto 0.00.
- IF uma avaliação ficar EM_ANDAMENTO por mais de 24 horas sem nenhum comando THEN o sistema SHALL finalizá-la numa rotina agendada de hora em hora, com `tempoUtilizado = tempoConfigurado`.
- The system SHALL NOT permitir excluir fisicamente uma avaliação (RNF006); a única forma de removê-la é o cancelamento.
- WHEN o coordenador altera ou inativa uma `lista_palavras` depois que uma avaliação já copiou as palavras dela (AVA-01) THEN o sistema SHALL manter as palavras da avaliação inalteradas (PAL-06, `banco-palavras`; `.specs/features/banco-palavras/spec.md`) - a cópia feita na criação é a única fonte usada por essa avaliação.
- WHEN o coordenador substitui as faixas de uma série em `regras-classificacao` THEN o sistema SHALL manter `fase` e `nivel` já gravados numa avaliação `FINALIZADA` inalterados, a menos que um recálculo seja disparado explicitamente por uma mudança de palavra (AC 6 acima) - a substituição de faixas, sozinha, não escreve em `avaliacao` (REG-14, `regras-classificacao`; `.specs/features/regras-classificacao/spec.md`).
- WHEN uma avaliação `FINALIZADA` com áudio é `CANCELADA` THEN o sistema SHALL manter o registro de `avaliacao_audio` e o arquivo continuam recuperáveis - cancelar não aciona `AudioStoragePort` de forma alguma (AD-003: áudio não expira).
- IF `AudioStoragePort.armazenar` lançar `AudioArmazenamentoException` (falha de escrita em disco) THEN o sistema SHALL retornar 500 sem gravar `avaliacao_audio` - é uma falha de infraestrutura, não um erro de negócio.

### Tabela de status

| De \ Ação | iniciar | pausar | continuar | resetar | finalizar | cancelar |
| --------- | ------- | ------ | --------- | ------- | --------- | -------- |
| CRIADA | EM_ANDAMENTO | 409 | 409 | 409 | 409 | CANCELADA |
| EM_ANDAMENTO | 200 idempotente | PAUSADA | 200 idempotente | CRIADA | FINALIZADA | CANCELADA |
| PAUSADA | 409 | 200 idempotente | EM_ANDAMENTO | CRIADA | FINALIZADA | CANCELADA |
| FINALIZADA | 409 | 409 | 409 | 409 | 200 idempotente | CANCELADA |
| CANCELADA | 409 | 409 | 409 | 409 | 409 | 200 idempotente |

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | AVA-02..AVA-08 |
| Failure / partial-failure | A finalização (status + conversão das PENDENTE + resultado) acontece numa única transação; a marcação em lote é tudo ou nada (AVA-15) |
| Idempotency / duplicates | Transições repetidas são idempotentes (AVA-14); a duplicidade por ciclo é permitida (AD-004) |
| Auth boundaries | Só PROFESSOR executa, e só nos próprios alunos; COORDENADOR lê (AUTH-08, AUTH-09) |
| Concurrency / ordering | Lock otimista com 409 `CONFLITO_DE_VERSAO` |
| Data lifecycle | Sem exclusão física; cancelamento com justificativa; finalização automática das esquecidas (> 24h) |
| Observability | Log de transições (AVA-16) |
| External-dependency failure | `AudioStoragePort.armazenar`/`recuperar` (disco local via `audio-avaliacao`) podem falhar (`AudioArmazenamentoException` → 500, `AudioNaoEncontradoException` → 500 já que a referência gravada deveria sempre existir); a classificação é local, sem chamada externa |
| State-transition integrity | Tabela de status + AVA-13 + AVA-17 |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AVA-01 | P1: Criar avaliação com cópias e palavras copiadas | T12, T13 | Done |
| AVA-02 | P1: Aluno avaliável | T12, T13 | Done |
| AVA-03 | P1: Limite de palavras por ano e série | T12, T13 | Done |
| AVA-04 | P1: Fonte de conteúdo única e compatível | T12, T13 | Done |
| AVA-05 | P1: Tempo 10–600 s | T12, T13 | Done |
| AVA-06 | P1: Data válida | T12, T13 | Done |
| AVA-07 | P1: 1º ano sem não canônicas | T12, T13 | Done |
| AVA-08 | P1: Validação de palavra e tokenização do texto digitado | T12, T13 | Done |
| AVA-09 | P1: Iniciar | T14, T16 | Done |
| AVA-10 | P1: Pausar / continuar sem contar a pausa | T14, T16 | Done |
| AVA-11 | P1: Resetar | T14, T16 | Done |
| AVA-12 | P1: Finalizar | T15, T16 | Done |
| AVA-13 | P1: Transição inválida 409 | T14, T16 | In Progress |
| AVA-14 | P1: Transições idempotentes | T14, T15, T16 | In Progress |
| AVA-15 | P1: Marcar palavra individual e em lote | T17, T18 | Done |
| AVA-16 | P1: Log de transições | T14 | Done |
| AVA-17 | P1: Finalização automática pelo servidor (tempo esgotado e > 24h) | T14, T15, T16 | In Progress |
| AVA-18 | P1: Marcação bloqueada em CRIADA ou CANCELADA | T17, T18 | Done |
| AVA-19 | P1: Alteração depois de finalizar com auditoria e recálculo | T17, T18 | Done |
| AVA-20 | P1: Cálculo do resultado | T15, T16 | Done |
| AVA-21 | P1: Classificação na finalização | T15, T16 | Done |
| AVA-22 | P1: Classificação pendente | T15, T16 | Done |
| AVA-23 | P1: Consulta da avaliação | T19 | In Progress |
| AVA-24 | P1: Cancelamento com justificativa | - | Pending |
| AVA-25 | P1: Lock otimista | T16 | Done |
| AVA-26 | P2: Consulta da auditoria | - | Pending |
| AVA-27 | P1: Envio do áudio (201, grava `avaliacao_audio`) | - | Pending |
| AVA-28 | P1: Envio bloqueado fora de FINALIZADA | - | Pending |
| AVA-29 | P1: Envio duplicado bloqueado | - | Pending |
| AVA-30 | P1: Formato/tamanho de áudio inválido | - | Pending |
| AVA-31 | P1: Download do áudio | - | Pending |
| AVA-32 | P1: Download sem áudio gravado (404) | - | Pending |

**Coverage:** 32 total, 0 mapped to tasks, 32 unmapped ⚠️

---

## Success Criteria

- [ ] Todas as células da tabela de status cobertas por teste parametrizado.
- [ ] O exemplo do SDD §13 reproduzido exatamente num teste de integração.
- [ ] Nenhuma alteração em avaliação FINALIZADA fica sem registro de auditoria (teste de propriedade sobre as operações de escrita).

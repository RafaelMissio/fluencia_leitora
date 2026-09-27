# Histórico e Evolução Specification

> Origem: SDD §5.1, §6.1, §14, §15, §16, §26, RF012, RF013, RF014, RNF005, §24 item 8. Decisões: AD-004, AD-005.

## Problem Statement

O objetivo do sistema é acompanhar o aluno ao longo do tempo: entre os ciclos Entrada, Acompanhamento e Saída de um ano e entre anos letivos diferentes. O professor precisa desse contexto ao selecionar um aluno (SDD §6.1), e o coordenador precisa consultar avaliações e comparar desempenho.

## Goals

- [ ] Ao selecionar um aluno, o professor vê resumo, histórico e evolução no ano em uma única chamada de resumo.
- [ ] A evolução por ciclo e a comparação anual reproduzem exatamente os exemplos do SDD §14 e §15.
- [ ] Mudanças de turma ou professor nunca alteram o histórico exibido (RNF005).

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Gráficos e dashboards | Evolução futura (SDD §25); o frontend exibe tabelas no MVP |
| Relatórios por turma, professor ou ciclo e comparação entre turmas | Evolução futura (SDD §25) |
| Exportação para PDF ou Excel | Evolução futura (SDD §25) |
| Palavras por minuto (WPM) | Não pedido no SDD |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Avaliação que conta por ciclo | A FINALIZADA mais recente (maior `finalizadoEm`) por aluno + ano letivo + tipo de leitura + ciclo | Opção escolhida pelo usuário (AD-004) | y |
| Métrica da comparação anual (SDD §24 item 8) | As duas: palavras corretas (absoluto) e percentual de acerto | Os exemplos do SDD usam valores absolutos, e o percentual compensa listas de tamanhos diferentes | n |
| Separação por tipo de leitura | Evolução e comparação são sempre calculadas para um único tipo de leitura (parâmetro obrigatório) | Comparar palavras com texto curto distorce o resultado | n |
| Base da comparação anual | O mesmo ciclo no ano letivo anterior em que o aluno tem avaliação (ex.: Entrada 2027 × Entrada 2026) | Segue o formato de tabela do SDD §15 | n |
| Evolução percentual com base zero | `evolucaoPercentual = null` e `semBase = true` | O SDD §15 exige tratar a divisão por zero separadamente | n |
| "Ciclo atual" (SDD §6.1) | O ciclo seguinte ao último que tem avaliação FINALIZADA (de qualquer tipo) no ano ATIVO; ENTRADA se não houver nenhuma; SAIDA se a Saída já tiver avaliação | O SDD não define datas de ciclo; esta regra deriva o ciclo dos dados | n |
| "Última classificação" | Fase e nível da avaliação FINALIZADA mais recente, de qualquer tipo e ano | É a leitura mais direta do SDD §6.1 | n |
| Avaliações canceladas | Aparecem no histórico com status CANCELADA; ficam fora da evolução, da comparação e da última classificação | Transparência sem distorcer os indicadores | n |
| Arredondamento | Percentuais com 2 casas, HALF_UP | Igual ao `percentualAcerto` (AVA-20) | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Resumo do aluno ⭐ MVP

**User Story**: Como professor, quero ver ao selecionar um aluno os dados, o ciclo atual, a última classificação e a evolução no ano, para decidir qual avaliação aplicar (SDD §6.1).

**Why P1**: É a tela que abre o fluxo de avaliação.

**Acceptance Criteria**:
1. WHEN um usuário autorizado envia `GET /api/v1/alunos/{id}/resumo` THEN o sistema SHALL retornar `nome`, `turma`, `professor`, `anoLetivo`, `serie` (da matrícula no ano ATIVO), `cicloAtual`, `ultimaClassificacao` (`fase`, `nivel`, `dataAvaliacao`, `tipoLeitura`) e as 5 avaliações mais recentes.
2. WHEN o aluno não tem nenhuma avaliação FINALIZADA no ano ATIVO THEN o sistema SHALL retornar `cicloAtual = ENTRADA`.
3. WHEN a última avaliação FINALIZADA do ano ATIVO é do ciclo ACOMPANHAMENTO THEN o sistema SHALL retornar `cicloAtual = SAIDA`.
4. WHEN o aluno não tem nenhuma avaliação FINALIZADA THEN o sistema SHALL retornar `ultimaClassificacao = null`.
5. IF o aluno não tiver matrícula no ano ATIVO THEN o sistema SHALL retornar os dados da matrícula mais recente e `cicloAtual = null`.

**Independent Test**: Aluno com avaliação de Entrada finalizada → `cicloAtual = ACOMPANHAMENTO`.

---

### P1: Histórico do aluno ⭐ MVP

**User Story**: Como professor ou coordenador, quero consultar todas as avaliações do aluno em todos os anos (RF012, SDD §16).

**Why P1**: RF012 é requisito funcional explícito.

**Acceptance Criteria**:
1. WHEN um usuário autorizado envia `GET /api/v1/alunos/{id}/avaliacoes` THEN o sistema SHALL retornar as avaliações do aluno em ordem decrescente de `dataAvaliacao` e depois de `criadoEm`, paginadas com 20 itens.
2. The system SHALL incluir em cada item todos os campos do SDD §16: ano letivo, série, turma, professor, ciclo, tipo de leitura, data, quantidade de palavras, corretas, incorretas, não lidas, percentual, fase, nível, tempo utilizado, status e `possuiAudio`.
3. The system SHALL tirar turma, série e professor de cada item das cópias gravadas na avaliação, e não da matrícula atual.
4. WHEN são informados os filtros `anoLetivoId`, `tipoLeitura`, `ciclo` ou `status` THEN o sistema SHALL aplicar todos eles em conjunto (E).
5. WHEN o professor do aluno muda depois de uma avaliação THEN o sistema SHALL continuar mostrando o professor original naquela avaliação.

**Independent Test**: Trocar a turma do aluno e conferir que o histórico mostra a turma antiga nas avaliações anteriores.

---

### P1: Evolução por ciclo ⭐ MVP

**User Story**: Como professor, quero ver a evolução Entrada → Acompanhamento → Saída de um ano letivo (RF013, SDD §14).

**Why P1**: RF013 é o principal objetivo pedagógico dentro do ano.

**Acceptance Criteria**:
1. WHEN um usuário autorizado envia `GET /api/v1/alunos/{id}/evolucao?anoLetivoId={a}&tipoLeitura={t}` THEN o sistema SHALL retornar exatamente 3 entradas, na ordem ENTRADA, ACOMPANHAMENTO e SAIDA, cada uma com `avaliacaoId`, `corretas`, `percentualAcerto`, `fase` e `nivel` da avaliação que conta naquele ciclo.
2. WHEN um ciclo não tem avaliação FINALIZADA THEN o sistema SHALL retornar a entrada com os valores nulos.
3. The system SHALL retornar, para ACOMPANHAMENTO e SAIDA, `evolucaoAbsoluta = corretas atual − corretas do ciclo anterior` e `evolucaoPercentual = (atual − anterior) / anterior × 100`, com os dois nulos se algum dos lados for nulo.
4. WHEN o aluno do 1º ano tem corretas 6, 10 e 15 nos três ciclos (seed) THEN o sistema SHALL retornar as classificações PRE_LEITOR/3, LEITOR_INICIANTE e LEITOR_FLUENTE, evoluções absolutas +4 e +5 e percentuais 66.67 e 50.00.
5. WHEN houver 2 avaliações FINALIZADAS no mesmo ciclo e tipo THEN o sistema SHALL usar a de maior `finalizadoEm`.
6. IF `tipoLeitura` não for informado THEN o sistema SHALL retornar 422.

**Independent Test**: Reproduzir o exemplo do SDD §14.

---

### P1: Comparação entre anos letivos ⭐ MVP

**User Story**: Como coordenador ou professor, quero comparar o desempenho do aluno com os anos anteriores (RF014, SDD §15).

**Why P1**: RF014 é requisito funcional explícito e o objetivo longitudinal do sistema (SDD §26).

**Acceptance Criteria**:
1. WHEN um usuário autorizado envia `GET /api/v1/alunos/{id}/comparacao-anual?tipoLeitura={t}` THEN o sistema SHALL retornar uma linha por ano letivo com matrícula, em ordem crescente de ano, com `anoLetivo`, `serie` e, para cada ciclo, `corretas` e `percentualAcerto`.
2. The system SHALL retornar, para cada ciclo de cada ano a partir do segundo, `evolucaoAbsoluta` e `evolucaoPercentual` em relação ao mesmo ciclo do ano anterior da lista, calculados sobre as `corretas`, e também `evolucaoPercentualAcerto = percentualAcerto atual − percentualAcerto anterior` (em pontos percentuais).
3. WHEN 2026 tem Entrada 12 e 2027 tem Entrada 25 THEN o sistema SHALL retornar para 2027/Entrada `evolucaoAbsoluta = 13` e `evolucaoPercentual = 108.33`.
4. IF o valor anterior for 0 THEN o sistema SHALL retornar `evolucaoPercentual = null` e `semBase = true`, mantendo `evolucaoAbsoluta` calculada.
5. IF o valor anterior ou o atual não existir THEN o sistema SHALL retornar as evoluções daquele ciclo como null e `semBase = false`.
6. WHEN o aluno tem matrícula em um único ano THEN o sistema SHALL retornar uma linha, sem evoluções.

**Independent Test**: Reproduzir a tabela do SDD §15 (2026 e 2027).

---

### P2: Consultar avaliações (coordenador)

**User Story**: Como coordenador, quero listar avaliações com filtros, para acompanhar a aplicação na escola (SDD §17.1).

**Why P2**: Útil para gestão, mas não bloqueia o professor.

**Acceptance Criteria**:
1. WHEN o coordenador envia `GET /api/v1/avaliacoes?anoLetivoId=&turmaId=&professorId=&ciclo=&tipoLeitura=&status=` THEN o sistema SHALL retornar as avaliações que atendem a todos os filtros, paginadas com 20 itens, usando turma e professor das cópias gravadas.
2. IF um PROFESSOR chamar esse endpoint THEN o sistema SHALL restringir o resultado às avaliações com a cópia de `professorId` igual à dele.

**Independent Test**: Filtrar por turma e ciclo e conferir a contagem.

---

## Edge Cases

- WHEN o aluno não tem nenhuma avaliação THEN o sistema SHALL retornar 200 com histórico vazio, evolução com 3 ciclos nulos e comparação com as linhas dos anos matriculados sem valores.
- IF o aluno não existir, ou for de outro professor (perfil PROFESSOR) THEN o sistema SHALL retornar 404.
- The system SHALL ignorar avaliações com status diferente de FINALIZADA na evolução, na comparação e na última classificação.
- WHEN uma avaliação FINALIZADA é recalculada por alteração de palavra (AVA-19) THEN o sistema SHALL refletir os valores novos na próxima consulta.

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | EVO-15 (tipo obrigatório); paginação limitada a 100 itens por página |
| Failure / partial-failure | N/A because são somente leituras |
| Idempotency / duplicates | Várias avaliações por ciclo: vale a mais recente (EVO-14) |
| Auth boundaries | Escopo do professor (EVO-21, AUTH-09) |
| Concurrency / ordering | N/A because é leitura; a ordem é definida por `finalizadoEm` e `dataAvaliacao` |
| Data lifecycle | O histórico vem das cópias gravadas (EVO-08, EVO-09) |
| Observability | N/A because são consultas simples |
| External-dependency failure | N/A because não há dependência externa |
| State-transition integrity | Só FINALIZADA conta nos indicadores |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| EVO-01 | P1: Resumo do aluno - campos | - | Pending |
| EVO-02 | P1: Regra do ciclo atual | - | Pending |
| EVO-03 | P1: Última classificação | - | Pending |
| EVO-04 | P1: Resumo sem matrícula ativa | - | Pending |
| EVO-05 | P1: Histórico ordenado e paginado | - | Pending |
| EVO-06 | P1: Campos do SDD §16 | - | Pending |
| EVO-07 | P1: Filtros do histórico | - | Pending |
| EVO-08 | P1: Histórico usa as cópias | - | Pending |
| EVO-09 | P1: Mudança de professor não altera o histórico | - | Pending |
| EVO-10 | P1: Evolução com 3 ciclos | - | Pending |
| EVO-11 | P1: Ciclo sem avaliação nulo | - | Pending |
| EVO-12 | P1: Cálculo da evolução entre ciclos | - | Pending |
| EVO-13 | P1: Exemplo do SDD §14 | - | Pending |
| EVO-14 | P1: Mais recente por ciclo | - | Pending |
| EVO-15 | P1: Tipo de leitura obrigatório | - | Pending |
| EVO-16 | P1: Comparação anual - linhas | - | Pending |
| EVO-17 | P1: Comparação anual - evoluções | - | Pending |
| EVO-18 | P1: Exemplo do SDD §15 | - | Pending |
| EVO-19 | P1: Base zero | - | Pending |
| EVO-20 | P1: Valores ausentes / ano único | - | Pending |
| EVO-21 | P2: Consulta de avaliações com filtros e escopo | - | Pending |

**Coverage:** 21 total, 0 mapped to tasks, 21 unmapped ⚠️

---

## Success Criteria

- [ ] Os exemplos do SDD §14 e §15 reproduzidos em testes de integração.
- [ ] A consulta do histórico de um aluno com 30 avaliações responde em menos de 300 ms com os índices do SDD §19.

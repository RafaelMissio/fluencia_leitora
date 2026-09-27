# Cadastros Base Specification

> Origem: SDD §4, §4.1–§4.4, §5, §2, §6.1 (busca), RF001–RF005, RNF005, RNF006. Decisões: AD-005, AD-006.

## Problem Statement

Toda avaliação depende de um aluno vinculado a uma turma, a um professor e a um ano letivo, com limites de quantidade de palavras por série. Sem esses cadastros não é possível criar avaliações nem montar o histórico entre anos. O modelo precisa manter a mesma identidade do aluno de um ano para o outro, o que o DDL original não permite.

## Goals

- [ ] O coordenador cadastra ano letivo, configuração de palavras por série, professores, turmas, alunos e matrículas pela API.
- [ ] O professor encontra um aluno pelo nome em até 2 requisições (busca e depois detalhe).
- [ ] O mesmo aluno pode ter matrículas em anos letivos diferentes, sempre com o mesmo `alunoId`.
- [ ] Nenhum registro com histórico de avaliação pode ser excluído fisicamente.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Cadastro de usuários e login | Feature `autenticacao-perfis` |
| Listas de palavras | Feature `banco-palavras` |
| Importação em massa (CSV/planilha) de alunos | Não pedida no SDD |
| Edição de ciclos e tipos de leitura | São domínios fixos (SDD §2 e §5); ficam como seed somente leitura |
| Promoção automática de alunos para o ano seguinte | Não pedida; a nova matrícula é feita manualmente |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Aluno preso a um ano letivo (DDL §19) | Separar em `aluno` (identidade) + `matricula` (vínculo anual) | Necessário para a comparação anual (RF014); ver AD-005 | y |
| Situação do ano letivo | Enum `PLANEJADO`, `ATIVO`, `ENCERRADO`; no máximo um `ATIVO` por vez | Avaliações só acontecem no ano corrente; o booleano do DDL não diferencia planejado de encerrado | n |
| Onde fica a configuração de mín./máx. de palavras | Por ano letivo + série (tabela `configuracao_avaliacao`), com seed automático ao criar o ano: série 1 = 15–20; séries 2–5 = 20–60 | O DDL §19 e o SDD §11 definem os limites por série; o SDD §4.1 diz que o coordenador pode configurá-los | n |
| Séries suportadas | 1 a 5 | O SDD só define regras para o 1º ao 5º ano | n |
| Professor do aluno | A matrícula tem `professorId`, que por padrão é o professor da turma e pode ser alterado a qualquer momento | O SDD §4.3 lista professor no aluno e diz que ele pode ser alterado | n |
| "Dados de identificação" imutáveis (SDD §4.3) | O `nome` do aluno fica bloqueado depois da primeira avaliação (qualquer status exceto CANCELADA) | O nome é o único dado de identificação do modelo | n |
| Quem cadastra alunos | Somente o COORDENADOR; o PROFESSOR apenas consulta os seus alunos | SDD §17 não dá ao professor permissão de cadastro | n |
| Exclusão | Não existe exclusão física; `DELETE` apenas inativa o registro | RNF006 | n |
| Transferência de turma no mesmo ano | É permitida; as avaliações anteriores mantêm a cópia da turma antiga | RNF005 | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Gerenciar ano letivo e limites de palavras ⭐ MVP

**User Story**: Como coordenador, quero cadastrar o ano letivo e configurar a quantidade mínima e máxima de palavras por série, para que as avaliações respeitem os limites pedagógicos.

**Why P1**: Todas as outras entidades dependem do ano letivo.

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/anos-letivos` com `ano`, `dataInicio`, `dataFim` válidos THEN o sistema SHALL criar o ano com situação `PLANEJADO`, retornar 201 e criar a configuração de palavras das séries 1 (15–20) e 2 a 5 (20–60).
2. IF `ano` já existir THEN o sistema SHALL retornar 409 com código `ANO_LETIVO_DUPLICADO`.
3. IF `dataFim` for anterior ou igual a `dataInicio`, ou `ano` estiver fora de 2000–2100 THEN o sistema SHALL retornar 422 indicando o campo inválido.
4. WHEN o coordenador ativa um ano letivo THEN o sistema SHALL mudar para `ENCERRADO` o ano que estava `ATIVO` e deixar somente o novo como `ATIVO`.
5. WHEN o coordenador envia `PUT /api/v1/anos-letivos/{id}/configuracoes/{serie}` com `quantidadeMinima` e `quantidadeMaxima` THEN o sistema SHALL gravar os novos limites e retornar 200.
6. IF `quantidadeMinima` < 1, `quantidadeMaxima` > 200 ou `quantidadeMinima` > `quantidadeMaxima` THEN o sistema SHALL retornar 422 sem alterar a configuração.
7. IF a série estiver fora de 1–5 THEN o sistema SHALL retornar 422.

**Independent Test**: Criar o ano 2026, conferir as 5 configurações do seed, alterar a série 1 para 10–25 e conferir a leitura.

---

### P1: Gerenciar professores e turmas ⭐ MVP

**User Story**: Como coordenador, quero cadastrar professores e turmas e ligar cada turma a um professor responsável, para organizar quem avalia quem.

**Why P1**: A turma define série e professor, que são necessários para a matrícula e a avaliação.

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/professores` com `nome` de 3 a 150 caracteres THEN o sistema SHALL criar o professor com situação ativa e retornar 201.
2. WHEN o coordenador envia `POST /api/v1/turmas` com `nome` (1–100), `serie` (1–5), `anoLetivoId` e `professorId` opcional THEN o sistema SHALL criar a turma ativa e retornar 201.
3. IF já existir uma turma com o mesmo `nome` (sem diferenciar maiúsculas) no mesmo ano letivo THEN o sistema SHALL retornar 409 com código `TURMA_DUPLICADA`.
4. IF `professorId` ou `anoLetivoId` não existir ou estiver inativo THEN o sistema SHALL retornar 422 indicando a referência inválida.
5. WHEN o coordenador consulta `GET /api/v1/professores/{id}` THEN o sistema SHALL retornar o professor com a lista de turmas ativas associadas (0..n).
6. IF o coordenador tentar inativar um professor que seja responsável por alguma turma ativa THEN o sistema SHALL retornar 409 com código `PROFESSOR_COM_TURMA_ATIVA`.
7. WHEN o coordenador troca o professor responsável de uma turma THEN o sistema SHALL atualizar a turma e SHALL NOT alterar as avaliações já criadas.

**Independent Test**: Criar 1 professor e 2 turmas ligadas a ele, consultar o professor e ver as 2 turmas, tentar inativá-lo e receber 409.

---

### P1: Gerenciar alunos e matrículas ⭐ MVP

**User Story**: Como coordenador, quero cadastrar alunos e matriculá-los numa turma a cada ano letivo, para acompanhar o mesmo aluno ao longo dos anos.

**Why P1**: A avaliação é sempre feita sobre uma matrícula ativa.

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/alunos` com `nome` (3–150) e `turmaId` THEN o sistema SHALL criar o aluno e a matrícula no ano letivo da turma, com `serie` igual à da turma, `professorId` igual ao professor da turma e `anoFinalizado=false`, e retornar 201 com `alunoId` e `matriculaId`.
2. WHEN o coordenador envia `POST /api/v1/alunos/{alunoId}/matriculas` com `turmaId` de outro ano letivo THEN o sistema SHALL criar uma nova matrícula ligada ao mesmo `alunoId`.
3. IF o aluno já tiver matrícula no ano letivo da turma informada THEN o sistema SHALL retornar 409 com código `MATRICULA_DUPLICADA`.
4. WHEN o coordenador altera o `professorId` de uma matrícula THEN o sistema SHALL gravar o novo professor e SHALL NOT alterar as avaliações anteriores.
5. WHEN o coordenador transfere a matrícula para outra turma do mesmo ano letivo THEN o sistema SHALL atualizar turma e série da matrícula e SHALL NOT alterar as avaliações anteriores.
6. IF o coordenador tentar alterar o `nome` de um aluno que tem pelo menos uma avaliação com status diferente de `CANCELADA` THEN o sistema SHALL retornar 409 com código `ALUNO_COM_AVALIACAO`.
7. WHEN o coordenador marca `anoFinalizado=true` numa matrícula THEN o sistema SHALL gravar o indicador e SHALL impedir novas avaliações nessa matrícula.

**Independent Test**: Criar o aluno em 2026, matriculá-lo em 2027 e conferir o mesmo `alunoId` com 2 matrículas.

---

### P1: Buscar aluno ⭐ MVP

**User Story**: Como professor, quero buscar um aluno pelo nome antes de iniciar uma avaliação (RF005).

**Why P1**: É o primeiro passo do fluxo de avaliação (SDD §20).

**Acceptance Criteria**:
1. WHEN um usuário envia `GET /api/v1/alunos?nome={termo}` com termo de pelo menos 2 caracteres THEN o sistema SHALL retornar os alunos cujo nome contém o termo, sem diferenciar maiúsculas nem acentos, paginados com 20 itens por página e ordenados por nome.
2. IF o termo tiver menos de 2 caracteres THEN o sistema SHALL retornar 422.
3. WHILE o usuário autenticado tiver perfil PROFESSOR, o sistema SHALL retornar somente os alunos com matrícula no ano letivo `ATIVO` cujo `professorId` é o dele.
4. The system SHALL incluir em cada item da busca `alunoId`, `nome`, `turma`, `serie`, `professor`, `anoLetivo` e a situação da matrícula ativa.

**Independent Test**: Com os alunos "João Silva" e "Joana", buscar "joao" retorna apenas João; o professor B não vê os alunos do professor A.

---

### P2: Consultar domínios fixos

**User Story**: Como usuário, quero listar ciclos e tipos de leitura para preencher os formulários.

**Why P2**: Os valores são fixos e podem ficar no código do frontend, mas expô-los na API evita divergência.

**Acceptance Criteria**:
1. WHEN um usuário autenticado envia `GET /api/v1/ciclos` THEN o sistema SHALL retornar exatamente `ENTRADA`, `ACOMPANHAMENTO` e `SAIDA`, nessa ordem, com as descrições do SDD §5.
2. WHEN um usuário autenticado envia `GET /api/v1/tipos-leitura` THEN o sistema SHALL retornar exatamente `PALAVRA`, `PSEUDOPALAVRA` e `TEXTO_CURTO`, com as descrições do SDD §2.
3. IF houver uma tentativa de criar, alterar ou excluir ciclo ou tipo de leitura THEN o sistema SHALL retornar 405.

**Independent Test**: Chamar os dois endpoints e comparar com os valores do seed.

---

## Edge Cases

- IF alguém chamar `DELETE` em ano letivo, turma, professor ou aluno THEN o sistema SHALL apenas inativar o registro e retornar 204, sem excluí-lo fisicamente (RNF006).
- IF uma turma inativa for informada numa nova matrícula THEN o sistema SHALL retornar 422.
- IF a turma pertencer a um ano letivo `ENCERRADO` THEN o sistema SHALL retornar 422 ao tentar criar uma matrícula nela.
- WHEN a busca não encontrar nada THEN o sistema SHALL retornar 200 com a lista vazia.
- IF duas requisições alterarem o mesmo registro ao mesmo tempo THEN o sistema SHALL aceitar a primeira e retornar 409 com código `CONFLITO_DE_VERSAO` para a segunda (lock otimista).

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | CAD-01, CAD-03, CAD-06, CAD-11 |
| Failure / partial-failure | A criação de aluno e matrícula é feita numa única transação: se uma falha, as duas são revertidas (CAD-11) |
| Idempotency / duplicates | Unicidade de ano, turma por ano e matrícula por aluno e ano (CAD-02, CAD-07, CAD-13) |
| Auth boundaries | A escrita é só do COORDENADOR; a busca do PROFESSOR é limitada aos seus alunos (CAD-16); o mecanismo está em `autenticacao-perfis` |
| Concurrency / ordering | Lock otimista com 409 `CONFLITO_DE_VERSAO` |
| Data lifecycle | Não há exclusão física; apenas inativação (CAD-19) |
| Observability | N/A because é CRUD simples; o log de requisições padrão basta |
| External-dependency failure | N/A because não há dependência externa além do banco |
| State-transition integrity | Situação do ano letivo PLANEJADO → ATIVO → ENCERRADO, com um único ATIVO (CAD-04) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| CAD-01 | P1: Ano letivo - criação com seed de configuração | - | Pending |
| CAD-02 | P1: Ano letivo - unicidade | - | Pending |
| CAD-03 | P1: Ano letivo - validação de datas e ano | - | Pending |
| CAD-04 | P1: Ano letivo - único ATIVO | - | Pending |
| CAD-05 | P1: Configuração de mín./máx. por série | - | Pending |
| CAD-06 | P1: Validação de limites e série | - | Pending |
| CAD-07 | P1: Professores e turmas - CRUD e unicidade | - | Pending |
| CAD-08 | P1: Professor com várias turmas | - | Pending |
| CAD-09 | P1: Bloqueio de inativação de professor com turma ativa | - | Pending |
| CAD-10 | P1: Troca de professor da turma sem afetar histórico | - | Pending |
| CAD-11 | P1: Criação de aluno + matrícula | - | Pending |
| CAD-12 | P1: Nova matrícula no ano seguinte com o mesmo aluno | - | Pending |
| CAD-13 | P1: Unicidade de matrícula por ano | - | Pending |
| CAD-14 | P1: Troca de professor ou turma da matrícula sem afetar histórico | - | Pending |
| CAD-15 | P1: Nome do aluno imutável após avaliação | - | Pending |
| CAD-16 | P1: Busca de aluno (termo, acentos, paginação, escopo do professor) | - | Pending |
| CAD-17 | P1: Indicador de ano finalizado | - | Pending |
| CAD-18 | P2: Domínios fixos (ciclos e tipos de leitura) | - | Pending |
| CAD-19 | P1: Sem exclusão física (RNF006) | - | Pending |
| CAD-20 | P1: Lock otimista | - | Pending |

**Coverage:** 20 total, 0 mapped to tasks, 20 unmapped ⚠️

---

## Success Criteria

- [ ] Todos os ACs P1 cobertos por testes de integração com MySQL (Testcontainers).
- [ ] O mesmo aluno aparece com 2 matrículas em anos diferentes sem duplicar a identidade.
- [ ] Nenhum endpoint executa `DELETE` físico nas tabelas do cadastro.

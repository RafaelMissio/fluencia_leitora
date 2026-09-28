# Autenticação e Perfis Specification

> Origem: SDD §17, RNF002. Decisões: AD-006.

## Problem Statement

O sistema guarda dados de crianças e áudios de leitura, então todo acesso precisa ser autenticado. Coordenador e professor têm permissões diferentes (SDD §17), e cada professor só pode ver os próprios alunos e avaliações.

## Goals

- [ ] 100% dos endpoints `/api/v1/**` (exceto login) exigem token válido.
- [ ] Cada operação do SDD §17 fica restrita ao perfil correspondente.
- [ ] Um professor nunca obtém dados de aluno ou avaliação de outro professor.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| SSO / OAuth externo (Google, gov.br) | Não pedido no SDD |
| Recuperação de senha por e-mail | Exige envio de e-mail; o coordenador redefine a senha |
| Refresh token / logout no servidor | O token de 8h cobre um turno escolar; o logout é feito descartando o token no cliente |
| Perfil de diretor/secretaria | Não definido no SDD |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Mecanismo de autenticação | Login com e-mail e senha (BCrypt) que retorna um JWT assinado (HS256) válido por 8 horas | API stateless consumida por uma SPA; 8h cobre um turno | n |
| Primeiro acesso | Na inicialização, se não existir nenhum usuário, o sistema cria um COORDENADOR a partir das variáveis `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` | Evita credencial fixa no código | n |
| Acesso de um professor a recurso de outro | Retornar 404 (e não 403) | Não revela a existência do recurso | n |
| Proteção contra força bruta | Após 5 falhas seguidas, bloquear a conta por 15 minutos | Controle mínimo com baixo atrito | n |
| Quem realiza avaliações | Somente PROFESSOR; o COORDENADOR apenas consulta | SDD §17.1 não lista "realizar avaliações" para o coordenador | n |
| Histórico de anos anteriores | O professor vê o histórico completo (todos os anos) dos alunos que são dele no ano ATIVO | Necessário para a comparação anual (RF014) | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Login ⭐ MVP

**User Story**: Como usuário, quero entrar com e-mail e senha para acessar as funções do meu perfil.

**Why P1**: Sem autenticação nenhuma outra feature pode ir para produção (RNF002).

**Acceptance Criteria**:
1. WHEN um usuário ativo envia `POST /api/v1/auth/login` com e-mail e senha corretos THEN o sistema SHALL retornar 200 com `accessToken`, `expiresIn=28800`, `perfil` e `professorId` (quando o perfil for PROFESSOR).
2. IF o e-mail não existir ou a senha estiver errada THEN o sistema SHALL retornar 401 com a mensagem genérica "Credenciais inválidas", igual nos dois casos.
3. IF o usuário tiver 5 falhas de login seguidas THEN o sistema SHALL bloquear a conta por 15 minutos e retornar 429 com o header `Retry-After` em segundos, mesmo que a senha esteja correta.
4. WHEN o login dá certo THEN o sistema SHALL zerar o contador de falhas.
5. IF o usuário estiver inativo THEN o sistema SHALL retornar 401 com "Credenciais inválidas".
6. The system SHALL armazenar as senhas apenas como hash BCrypt e SHALL NOT registrar senhas em log.

**Independent Test**: Fazer login com sucesso; errar 5 vezes e receber 429 na sexta tentativa.

---

### P1: Proteção de rotas por perfil ⭐ MVP

**User Story**: Como coordenador, quero que cada perfil acesse apenas o que lhe cabe, para proteger os dados dos alunos.

**Why P1**: RNF002.

**Acceptance Criteria**:
1. IF uma requisição a `/api/v1/**` (exceto `/auth/login`) vier sem token, com token inválido ou com token expirado THEN o sistema SHALL retornar 401.
2. IF um PROFESSOR chamar um endpoint de escrita de cadastros, regras de classificação, listas de palavras ou usuários THEN o sistema SHALL retornar 403.
3. IF um COORDENADOR chamar um endpoint de execução de avaliação (criar, transições de status, marcar palavra, enviar áudio) THEN o sistema SHALL retornar 403. **Deferred**: nenhum endpoint de execução de avaliação existe ainda nesta feature (`avaliacao`/`audio-avaliacao` não implementadas) - o mecanismo (`@PreAuthorize("hasRole('PROFESSOR')")`) já está comprovado por AUTH-07 (mesmo mecanismo, papel diferente); a aplicação concreta deste AC, com teste real, é tarefa das features `avaliacao`/`audio-avaliacao` (ver `.specs/features/autenticacao-perfis/design.md` linha 168).
4. IF um PROFESSOR acessar um aluno, avaliação ou áudio cuja matrícula no ano ATIVO não tenha o `professorId` dele THEN o sistema SHALL retornar 404.
5. IF o usuário dono do token tiver sido inativado depois da emissão THEN o sistema SHALL retornar 401 na próxima requisição.
6. The system SHALL liberar sem autenticação apenas `/api/v1/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**` e `/actuator/health`.

**Independent Test**: Um token de professor recebe 403 em `POST /api/v1/turmas` e 404 ao acessar um aluno de outro professor.

---

### P1: Gerenciar usuários ⭐ MVP

**User Story**: Como coordenador, quero criar usuários, ligá-los ao cadastro de professor e redefinir senhas.

**Why P1**: Sem isso nenhum professor consegue entrar.

**Acceptance Criteria**:
1. WHEN o coordenador envia `POST /api/v1/usuarios` com `email`, `senha`, `perfil` e `professorId` THEN o sistema SHALL criar o usuário ativo e retornar 201 sem incluir a senha nem o hash na resposta.
2. IF o perfil for PROFESSOR sem `professorId` válido e ativo, ou COORDENADOR com `professorId` preenchido THEN o sistema SHALL retornar 422.
3. IF o e-mail já estiver em uso (sem diferenciar maiúsculas) THEN o sistema SHALL retornar 409 com código `EMAIL_DUPLICADO`.
4. IF a senha tiver menos de 8 caracteres THEN o sistema SHALL retornar 422.
5. WHEN o coordenador envia `PUT /api/v1/usuarios/{id}/senha` THEN o sistema SHALL gravar o novo hash e desbloquear a conta.
6. WHEN a aplicação inicia sem nenhum usuário cadastrado e com `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` definidos THEN o sistema SHALL criar um usuário COORDENADOR com essas credenciais.

**Independent Test**: O coordenador cria o usuário do professor, que faz login e recebe o `professorId` no token.

---

## Edge Cases

- IF o JWT tiver a assinatura adulterada THEN o sistema SHALL retornar 401.
- IF a aplicação iniciar sem usuários e sem `APP_ADMIN_EMAIL` THEN o sistema SHALL registrar um log WARN "Nenhum usuário cadastrado" e continuar a inicialização.
- WHEN o bloqueio de 15 minutos expirar THEN o sistema SHALL aceitar novamente o login com a senha correta.
- IF o segredo de assinatura do JWT (`APP_JWT_SECRET`) tiver menos de 32 bytes THEN a aplicação SHALL falhar na inicialização.

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | E-mail válido, senha com no mínimo 8 caracteres (AUTH-13) |
| Failure / partial-failure | N/A because o login é uma operação atômica de leitura com atualização de um contador |
| Idempotency / duplicates | E-mail único (AUTH-12) |
| Auth boundaries & rate limits | AUTH-03, AUTH-06..AUTH-10 |
| Concurrency / ordering | N/A because o incremento do contador de falhas é feito em uma única instrução UPDATE |
| Data lifecycle | Usuários são inativados, nunca excluídos; o token expira em 8h |
| Observability | Log INFO de login com sucesso e WARN de falha ou bloqueio, com e-mail e IP e sem senha (AUTH-05) |
| External-dependency failure | N/A because não há provedor de identidade externo |
| State-transition integrity | ATIVO/BLOQUEADO/INATIVO: o bloqueio expira sozinho; inativo nunca faz login |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AUTH-01 | P1: Login - emissão de JWT | Execute | ✅ Verified |
| AUTH-02 | P1: Login - credenciais inválidas | Execute | ✅ Verified |
| AUTH-03 | P1: Login - bloqueio após 5 falhas | Execute | ✅ Verified (spec-precision gap: contador após expirar o bloqueio) |
| AUTH-04 | P1: Login - usuário inativo | Execute | ✅ Verified |
| AUTH-05 | P1: Senha com hash e fora dos logs | Execute | ✅ Verified |
| AUTH-06 | P1: 401 sem token ou com token inválido | Execute | ✅ Verified |
| AUTH-07 | P1: 403 para professor em endpoints de coordenador | Execute | ✅ Verified (todos os endpoints de escrita existentes) |
| AUTH-08 | P1: 403 para coordenador em execução de avaliação | Execute | ⏭️ Deferred → `avaliacao` / `audio-avaliacao` (ver nota no AC) |
| AUTH-09 | P1: 404 para recurso de outro professor | Execute | ✅ Verified (escopo aluno) |
| AUTH-10 | P1: Revogação por inativação | Execute | ✅ Verified |
| AUTH-11 | P1: CRUD de usuários ligado ao professor | Execute | ✅ Verified |
| AUTH-12 | P1: E-mail único | Execute | ✅ Verified |
| AUTH-13 | P1: Política de senha | Execute | ✅ Verified |
| AUTH-14 | P1: Bootstrap do coordenador | Execute | ✅ Verified |
| AUTH-15 | P1: Rotas públicas | Execute | ✅ Verified (spec-precision gap: actuator ausente do projeto) |

**Coverage:** 15 total, 14 verified, 1 deferred (AUTH-08 → `avaliacao`/`audio-avaliacao`). Verifier report: `.specs/features/autenticacao-perfis/validation.md` (PASS, iteration 2/3).

---

## Success Criteria

- [ ] Um teste de segurança parametrizado cobre todos os endpoints, verificando 401 sem token e 403 no perfil errado.
- [ ] Nenhum log contém a senha (teste com captura de log).

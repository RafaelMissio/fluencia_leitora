# Autenticação e Perfis Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/autenticacao-perfis/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.specs/STATE.md` **AD-007** (JaCoCo ≥85% linhas; Testcontainers MySQL nos testes de integração). Amostra de testes existentes (`cadastros-base`, já implementada): `*ServiceTest.java` unit com Mockito, `*RepositoryIT.java`/`*ControllerIT.java` integration via `IntegrationTestBase` (Testcontainers MySQL, singleton container). Esta feature segue o mesmo padrão.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Serviço de domínio (`AuthService`, `UsuarioService`) | unit | Todos os branches; 1:1 com os ACs do spec (AUTH-01..AUTH-14); todo edge case listado tem um teste | `src/test/java/com/missio/fluencia_leitora/autenticacao/**/*ServiceTest.java` | `./mvnw test` |
| Infra de segurança (`JwtService`, `JwtAuthenticationFilter`, `JwtContextoUsuarioAdapter`, `PertencimentoProfessorGuard`) | unit | Emissão/validação de token (assinatura, expiração, malformado); filtro seta/rejeita o `SecurityContext` corretamente; guard lança 404 só quando o professor não é o dono | `src/test/java/com/missio/fluencia_leitora/common/security/**/*Test.java` | `./mvnw test` |
| Repositório com query customizada (`UsuarioRepository`) | integration | `findByEmailIgnoreCase`, `registrarFalha` (atômico, incrementa e bloqueia ao chegar em 5), `zerarFalhas` | `src/test/java/com/missio/fluencia_leitora/autenticacao/**/*RepositoryIT.java` | `./mvnw verify` |
| Controller (REST) - novo (`AuthController`, `UsuarioController`) e retrofitado (`AnoLetivoController`, `ProfessorController`, `TurmaController`, `AlunoController`, `MatriculaController`) | integration | Toda rota do escopo: caminho feliz + cada edge case listado + cada erro (401/403/404/409/422/429) do spec | `src/test/java/com/missio/fluencia_leitora/**/*ControllerIT.java` | `./mvnw verify` |
| Entidade (`Usuario`) / migração Flyway / bootstrap (`AdminBootstrap`) | none | - (build gate only; `AdminBootstrap` é exercitado indiretamente pela T13 via `ApplicationContextRunner`, ver task) | - | `./mvnw compile` |

## Gate Check Commands

> Reaproveita a configuração já existente do projeto (`maven-failsafe-plugin` para `*IT.java`, `jacoco-maven-plugin` com 85% de linhas na fase `verify` - ambos configurados em `cadastros-base`/T1, nada novo a configurar aqui).

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Após tasks que só adicionam `*ServiceTest`/`*Test` (unit, sem Docker) | `./mvnw test` |
| Full | Após tasks que adicionam/alteram `*RepositoryIT`/`*ControllerIT` (integration, precisa Docker) | `./mvnw verify` |
| Build | Fechamento de fase, ou tasks só de migração/entidade/config sem teste novo | `./mvnw compile` (e `./mvnw verify` no fechamento de cada fase) |

---

## Execution Plan

### Phase 1: Infraestrutura de segurança (JWT, filtro, contexto de usuário)

```
T1 -> T2
T1 -> T3
T3 -> T4
T2 -> T4
T4 -> T5
T5 -> T6
T5 -> T7
T7 -> T8
```

### Phase 2: Login e gestão de usuários

```
T9 -> T10
T9 -> T13
T11 -> T12
```

`T9` e `T11` dependem de `T2` (cross-fase, Phase 1); `T10` também depende de `T3` (cross-fase).

### Phase 3: Retrofit de `cadastros-base` (autorização por perfil)

```
T14
T15
T16
T17
```

`T14`, `T15`, `T16` e `T17` não dependem umas das outras - cada uma só depende de tasks da Phase 1 (`T5`, `T6`; `T16` também de `T8`). Executam em sequência pela ordem de listagem.

---

## Task Breakdown

### T1: Adicionar `spring-boot-starter-security` e `jjwt` ao `pom.xml`

**What**: Adicionar `spring-boot-starter-security` e `jjwt-api`/`jjwt-impl`(runtime)/`jjwt-jackson`(runtime) versão `0.13.0` ao `pom.xml`.
**Where**: `pom.xml`
**Depends on**: None
**Reuses**: N/A
**Requirement**: N/A (infraestrutura)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `./mvnw compile` passa com as novas dependências

**Tests**: none
**Gate**: build

---

### T2: Migração Flyway + entidade + repositório `Usuario`

**What**: Criar `V5__usuario.sql` (tabela `usuario`: `email` único, `senha_hash`, `perfil` enum, `professor_id` FK nullable, `ativo`, `tentativas_falhas` default 0, `bloqueado_ate` nullable, `version`, timestamps) e a entidade `Usuario` + `UsuarioRepository` com `findByEmailIgnoreCase(String email)` e duas `@Modifying @Query` de UPDATE atômico: `registrarFalha(Long id, Instant bloqueadoAte, int limite)` (incrementa `tentativas_falhas`; seta `bloqueado_ate` só quando o contador atinge o limite, numa única instrução) e `zerarFalhas(Long id)`.
**Where**: `src/main/resources/db/migration/V5__usuario.sql`, `src/main/java/com/missio/fluencia_leitora/autenticacao/Usuario.java`, `.../autenticacao/UsuarioRepository.java`
**Depends on**: T1
**Reuses**: N/A
**Requirement**: AUTH-03, AUTH-05, AUTH-11, AUTH-12

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `findByEmailIgnoreCase` encontra independente de maiúsculas
- [x] `registrarFalha` incrementa o contador numa única instrução e só seta `bloqueado_ate` quando atinge o limite (não antes)
- [x] `zerarFalhas` reseta contador e desbloqueio
- [x] Inserir dois usuários com o mesmo e-mail (case-insensitive) viola a constraint única
- [x] `./mvnw verify` passa (os 4 testes novos e todo o resto verdes; as únicas falhas são as 31 pré-existentes de 401/403 nos `*ControllerIT`, corrigidas pela T6)
- [x] 4 testes cobrindo os pontos acima

**Tests**: integration
**Gate**: full

---

### T3: `JwtService` (emitir/validar HS256)

**What**: Implementar `JwtService.emitir(Long usuarioId)` (claim `sub`=usuarioId, expiração +8h/28800s, `Jwts.builder()...signWith(key).compact()`) e `JwtService.validarERetornarUsuarioId(String token)` (retorna `Optional<Long>`, vazio se assinatura inválida/expirado/malformado - nunca lança). Lê `APP_JWT_SECRET` do ambiente; um `@PostConstruct` lança `IllegalStateException` se tiver menos de 32 bytes (a aplicação falha no boot, conforme o edge case do spec).
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/JwtService.java`
**Depends on**: T1
**Reuses**: N/A
**Requirement**: AUTH-01, AUTH-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `emitir` produz um token que `validarERetornarUsuarioId` decodifica de volta para o mesmo `usuarioId`
- [ ] Token com assinatura adulterada → `Optional.empty()`
- [ ] Token expirado → `Optional.empty()`
- [ ] `APP_JWT_SECRET` com menos de 32 bytes → o contexto Spring falha ao subir (`ApplicationContextRunner` ou similar, sem precisar de Docker)
- [ ] `./mvnw test` passa
- [ ] 4 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T4: `UsuarioAutenticado` + `JwtAuthenticationFilter`

**What**: Criar o record `UsuarioAutenticado(Long usuarioId, Perfil perfil, Long professorId)` e `JwtAuthenticationFilter` (`OncePerRequestFilter`): lê `Authorization: Bearer <token>`, valida via `JwtService`, carrega o `Usuario` (via `UsuarioRepository`), rejeita (não seta o `SecurityContext`, deixa a cadeia seguir sem autenticação) se o token for inválido/expirado ou se o usuário estiver inativo; senão popula o `SecurityContext` com `UsuarioAutenticado` como principal e `ROLE_<perfil>` como `GrantedAuthority`.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/UsuarioAutenticado.java`, `.../common/security/JwtAuthenticationFilter.java`
**Depends on**: T3, T2
**Reuses**: `JwtService`, `UsuarioRepository`
**Requirement**: AUTH-06, AUTH-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Token válido de usuário ativo → `SecurityContext` populado com perfil/professorId corretos (lidos do banco, não do claim)
- [ ] Token válido de usuário que foi inativado depois da emissão → `SecurityContext` não é populado (a cadeia segue sem autenticação, o que resulta em 401 mais adiante)
- [ ] Sem header `Authorization` → segue sem autenticação, sem lançar
- [ ] `./mvnw test` passa
- [ ] 3 testes cobrindo os pontos acima (com `UsuarioRepository` mockado)

**Tests**: unit
**Gate**: quick

---

### T5: `SecurityConfig` (`SecurityFilterChain`, 401/403 em `ProblemDetail`)

**What**: `SecurityFilterChain` stateless (`SessionCreationPolicy.STATELESS`), libera `/api/v1/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`; exige autenticação no resto; registra `JwtAuthenticationFilter` antes do filtro padrão; `@EnableMethodSecurity` (habilita `@PreAuthorize`); `AuthenticationEntryPoint` (401) e `AccessDeniedHandler` (403) customizados devolvendo `ProblemDetail` no mesmo formato do `GlobalExceptionHandler` existente.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/SecurityConfig.java`
**Depends on**: T4
**Reuses**: `common.error.GlobalExceptionHandler` (mesmo formato de `ProblemDetail`)
**Requirement**: AUTH-06, AUTH-15

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Requisição sem token a uma rota protegida → 401 em formato `ProblemDetail`
- [ ] Requisição com role errada (via um endpoint de teste com `@PreAuthorize`) → 403 em formato `ProblemDetail`
- [ ] `GET /v3/api-docs`, `/swagger-ui/**`, `/actuator/health` acessíveis sem token
- [ ] `./mvnw verify` passa
- [ ] 3 testes de integração cobrindo os pontos acima

**Tests**: integration
**Gate**: full

---

### T6: Helper de autenticação JWT em `IntegrationTestBase` + correção dos ITs existentes

**What**: A partir de `T5`, `SecurityConfig` passa a exigir autenticação em toda rota que não esteja na lista de liberadas - o que quebra os `*ControllerIT` já existentes de `cadastros-base` (`AnoLetivoControllerIT`, `AlunoControllerIT`, `TurmaControllerIT`, `ProfessorControllerIT`, `MatriculaControllerIT`, `DominioFixoControllerIT`), hoje escritos sem nenhum header de autenticação. Adicionar em `IntegrationTestBase` um helper (ex.: `protected String tokenCoordenador()` / `protected HttpHeaders authHeaders()`) que garante um usuário `COORDENADOR` de teste (via `UsuarioRepository`, senha via o mesmo `PasswordEncoder` de `T8`/`PasswordEncoderConfig` - ou um hash fixo se `PasswordEncoderConfig` ainda não existir neste ponto, o que for mais simples) e emite um JWT real via `JwtService.emitir`. Usar esse helper para adicionar `Authorization: Bearer <token>` em toda chamada HTTP dos seis `*ControllerIT` listados, sem alterar as asserções de negócio de nenhum teste. Em `AlunoControllerIT`, que hoje também usa os headers `X-Perfil`/`X-Professor-Id` para simular perfil via `ContextoUsuarioHeaderAdapter`, manter esses headers como estão - esse adapter só é substituído na `T7`; a rescrita completa desses testes para perfil via JWT real é escopo da `T16`.
**Where**: `src/test/java/com/missio/fluencia_leitora/support/IntegrationTestBase.java` (modifica), `src/test/java/com/missio/fluencia_leitora/cadastros/anoletivo/AnoLetivoControllerIT.java` (modifica), `.../cadastros/aluno/AlunoControllerIT.java` (modifica), `.../cadastros/turma/TurmaControllerIT.java` (modifica), `.../cadastros/professor/ProfessorControllerIT.java` (modifica), `.../cadastros/aluno/MatriculaControllerIT.java` (modifica), `.../cadastros/dominiofixo/DominioFixoControllerIT.java` (modifica, ou pacote equivalente)
**Depends on**: T5
**Reuses**: `JwtService`, `Usuario`/`UsuarioRepository`
**Requirement**: N/A (fecha o gap de infraestrutura descoberto durante o Execute - `T1`/`T5` protegem endpoints que os ITs de `cadastros-base` ainda chamavam sem autenticação; decisão do usuário, 2026-09-27)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Os seis `*ControllerIT` listados voltam a passar autenticados como `COORDENADOR`, sem 401/403 inesperado
- [ ] Nenhuma asserção de negócio pré-existente foi enfraquecida, removida ou tornada menos específica para "consertar" o 401 - só o header de autenticação foi adicionado
- [ ] `AlunoControllerIT` continua funcionando com `X-Perfil`/`X-Professor-Id` (inalterados) + o novo header de autenticação
- [ ] `./mvnw verify` passa (suíte completa, sem falha de autenticação)

**Tests**: integration (correção de testes existentes; nenhum caso novo é exigido por esta task)
**Gate**: full

---

### T7: `JwtContextoUsuarioAdapter` (substitui o adapter provisório)

**What**: Criar `JwtContextoUsuarioAdapter implements ContextoUsuarioPort` que lê `UsuarioAutenticado` de `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`. **Remover** `common.security.ContextoUsuarioHeaderAdapter` (e seu teste) - era provisório e spoofável por header.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/JwtContextoUsuarioAdapter.java`; remove `.../common/security/ContextoUsuarioHeaderAdapter.java` e `src/test/java/com/missio/fluencia_leitora/common/security/ContextoUsuarioHeaderAdapterTest.java`
**Depends on**: T5
**Reuses**: `common.security.ContextoUsuarioPort` (interface já existente, não muda)
**Requirement**: N/A (resolve o débito técnico registrado em `cadastros-base`)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `JwtContextoUsuarioAdapter.perfilAtual()`/`professorIdAtual()` retornam os valores do `UsuarioAutenticado` do contexto atual
- [ ] `ContextoUsuarioHeaderAdapter` e seu teste não existem mais no código
- [ ] `./mvnw test` passa
- [ ] 2 testes (perfil COORDENADOR e PROFESSOR) com um `Authentication` mockado no `SecurityContext`

**Tests**: unit
**Gate**: quick

---

### T8: `PertencimentoProfessorGuard`

**What**: Criar `PertencimentoProfessorGuard.verificar(Long professorIdDoRecurso)`: lança `BusinessException(404, "RECURSO_NAO_ENCONTRADO", ...)` quando `ContextoUsuarioPort.perfilAtual()==PROFESSOR` e `professorIdDoRecurso` não é igual a `professorIdAtual()`; não faz nada (retorna normalmente) para `COORDENADOR` ou quando os ids coincidem.
**Where**: `src/main/java/com/missio/fluencia_leitora/common/security/PertencimentoProfessorGuard.java`
**Depends on**: T7
**Reuses**: `common.security.ContextoUsuarioPort`, `common.error.BusinessException`
**Requirement**: AUTH-09

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] PROFESSOR com `professorIdDoRecurso` diferente do seu → 404
- [ ] PROFESSOR com `professorIdDoRecurso` igual ao seu → passa sem lançar
- [ ] COORDENADOR → passa sem lançar, independente do `professorIdDoRecurso`
- [ ] `./mvnw test` passa
- [ ] 3 testes cobrindo os pontos acima (com `ContextoUsuarioPort` mockado)

**Tests**: unit
**Gate**: quick

---

### T9: `AuthService.login`

**What**: Implementar `AuthService.login(String email, String senhaPlana)`: busca por `findByEmailIgnoreCase` (case-insensitive); se não existe, está inativo, ou a senha (via `PasswordEncoder.matches`, `BCryptPasswordEncoder` como `@Bean`) não confere → mesma mensagem genérica "Credenciais inválidas" nos três casos, chamando `registrarFalha` quando o usuário existe; se `bloqueado_ate` estiver no futuro → retorna estado "bloqueado" com os segundos restantes, mesmo que a senha esteja correta; login certo → `zerarFalhas`, `JwtService.emitir`, log INFO (sem senha); falha → log WARN (sem senha).
**Where**: `src/main/java/com/missio/fluencia_leitora/autenticacao/AuthService.java`, `.../autenticacao/PasswordEncoderConfig.java` (bean `BCryptPasswordEncoder`)
**Depends on**: T2
**Reuses**: `JwtService`, `UsuarioRepository`
**Requirement**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Login certo retorna token + `expiresIn=28800` + perfil (+ `professorId` se PROFESSOR)
- [ ] E-mail inexistente e senha errada retornam exatamente a mesma mensagem/estrutura de erro
- [ ] Usuário inativo → mesma mensagem genérica de credenciais inválidas
- [ ] 5ª falha seguida bloqueia por 15 minutos; a 6ª tentativa (mesmo com senha certa) retorna estado bloqueado com segundos restantes
- [ ] Login certo zera o contador de falhas
- [ ] Nenhuma linha de log contém a senha em texto puro (captura de log no teste)
- [ ] `./mvnw test` passa
- [ ] 6 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T10: `AuthController` (`POST /api/v1/auth/login`)

**What**: Implementar `POST /api/v1/auth/login` mapeando o resultado de `AuthService.login` para 200 (token+`expiresIn`+perfil+professorId), 401 (`ProblemDetail` "Credenciais inválidas") ou 429 com header `Retry-After` (segundos até o desbloqueio).
**Where**: `src/main/java/com/missio/fluencia_leitora/autenticacao/AuthController.java`, `.../autenticacao/dto/LoginRequest.java`, `.../autenticacao/dto/LoginResponse.java`
**Depends on**: T9, T3
**Reuses**: `common.error`
**Requirement**: AUTH-01, AUTH-02, AUTH-03, AUTH-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Login certo → 200 com o corpo esperado
- [ ] Credenciais erradas → 401
- [ ] 6ª tentativa após bloqueio → 429 com `Retry-After` presente e numérico
- [ ] `./mvnw verify` passa
- [ ] 4 testes cobrindo os pontos acima, contra MySQL real (Testcontainers)

**Tests**: integration
**Gate**: full

---

### T11: `UsuarioService`

**What**: Implementar `UsuarioService.criar(CriarUsuarioRequest)` (valida perfil×professorId - 422 se PROFESSOR sem `professorId` válido/ativo ou COORDENADOR com `professorId` preenchido; 409 `EMAIL_DUPLICADO` se e-mail já em uso case-insensitive; 422 se senha < 8 caracteres; nunca retorna senha/hash) e `UsuarioService.alterarSenha(Long id, String novaSenha)` (grava novo hash e desbloqueia - zera `tentativas_falhas`/`bloqueado_ate`).
**Where**: `src/main/java/com/missio/fluencia_leitora/autenticacao/UsuarioService.java`
**Depends on**: T2
**Reuses**: `common.error.BusinessException`, `cadastros.professor.ProfessorRepository` (já existe, para validar `professorId`)
**Requirement**: AUTH-11, AUTH-12, AUTH-13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `criar` grava com sucesso (sem retornar senha/hash)
- [ ] PROFESSOR sem `professorId` válido/ativo → 422; COORDENADOR com `professorId` → 422
- [ ] E-mail duplicado (case-insensitive) → 409 `EMAIL_DUPLICADO`
- [ ] Senha < 8 caracteres → 422
- [ ] `alterarSenha` grava o novo hash e zera o bloqueio
- [ ] `./mvnw test` passa
- [ ] 6 testes cobrindo os pontos acima

**Tests**: unit
**Gate**: quick

---

### T12: `UsuarioController`

**What**: Implementar `POST /api/v1/usuarios` e `PUT /api/v1/usuarios/{id}/senha`.
**Where**: `src/main/java/com/missio/fluencia_leitora/autenticacao/UsuarioController.java`, `.../autenticacao/dto/*`
**Depends on**: T11
**Reuses**: `common.error`
**Requirement**: AUTH-11, AUTH-12, AUTH-13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `POST /usuarios` cria (201), sem senha/hash na resposta
- [ ] Casos 422/409 do spec confirmados via HTTP
- [ ] `PUT /usuarios/{id}/senha` grava e desbloqueia
- [ ] Só `COORDENADOR` pode chamar ambos (`@PreAuthorize("hasRole('COORDENADOR')")`) - 403 para PROFESSOR
- [ ] `./mvnw verify` passa
- [ ] 5 testes cobrindo os pontos acima, contra MySQL real

**Tests**: integration
**Gate**: full

---

### T13: `AdminBootstrap`

**What**: `ApplicationRunner` que, se `UsuarioRepository.count()==0`: cria um usuário COORDENADOR com `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` (senha via o mesmo `PasswordEncoder`); se essas variáveis não estiverem definidas, loga WARN "Nenhum usuário cadastrado" e deixa a aplicação subir normalmente (edge case do spec).
**Where**: `src/main/java/com/missio/fluencia_leitora/autenticacao/AdminBootstrap.java`
**Depends on**: T9
**Reuses**: `UsuarioRepository`, `PasswordEncoder`
**Requirement**: AUTH-14

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Banco vazio + variáveis definidas → cria o COORDENADOR
- [ ] Banco vazio + variáveis ausentes → loga WARN e não lança, aplicação sobe
- [ ] Banco não vazio → não faz nada
- [ ] `./mvnw test` passa
- [ ] 3 testes com `ApplicationContextRunner`/repositório mockado (sem precisar de Docker)

**Tests**: unit
**Gate**: quick

---

### T14: Retrofit `AnoLetivoController` com `@PreAuthorize`

**What**: Adicionar `@PreAuthorize("hasRole('COORDENADOR')")` em todos os métodos de escrita de `AnoLetivoController` (`POST`, `POST /ativar`, `PUT /configuracoes/{serie}`, `DELETE`). `AnoLetivoControllerIT` já autentica como `COORDENADOR` desde a `T6`; adicionar aqui só o caso novo com JWT de um usuário PROFESSOR esperando 403.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/anoletivo/AnoLetivoController.java` (modifica), `src/test/java/com/missio/fluencia_leitora/cadastros/anoletivo/AnoLetivoControllerIT.java` (modifica)
**Depends on**: T5, T6
**Reuses**: `JwtService`, `Usuario`/`UsuarioRepository`
**Requirement**: AUTH-07

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Token de PROFESSOR em qualquer endpoint de escrita de ano letivo → 403
- [ ] Token de COORDENADOR continua funcionando como antes (nenhum teste antigo quebra)
- [ ] `./mvnw verify` passa
- [ ] Testes existentes + pelo menos 1 novo caso de 403 por task

**Tests**: integration
**Gate**: full

**Commit**: `feat(autenticacao)!: require COORDENADOR role on ano-letivo write endpoints`

---

### T15: Retrofit `ProfessorController` e `TurmaController` com `@PreAuthorize`

**What**: Mesmo retrofit da T14, aplicado a `ProfessorController` (`POST`, `DELETE`) e `TurmaController` (`POST`, `PUT`, `DELETE`). `ProfessorControllerIT` e `TurmaControllerIT` já autenticam como `COORDENADOR` desde a `T6`; adicionar aqui só o caso novo com JWT de PROFESSOR esperando 403 em cada um.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/professor/ProfessorController.java` (modifica), `.../cadastros/turma/TurmaController.java` (modifica), + os dois `*ControllerIT.java` correspondentes (modifica)
**Depends on**: T5, T6
**Reuses**: `JwtService`, `Usuario`/`UsuarioRepository`
**Requirement**: AUTH-07

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Token de PROFESSOR em qualquer endpoint de escrita de professor/turma → 403
- [ ] Tokens de COORDENADOR continuam funcionando
- [ ] `./mvnw verify` passa
- [ ] Testes existentes + pelo menos 1 novo caso de 403 por controller

**Tests**: integration
**Gate**: full

**Commit**: `feat(autenticacao)!: require COORDENADOR role on professor and turma write endpoints`

---

### T16: Retrofit `AlunoController` + novo `GET /api/v1/alunos/{id}`

**What**: Adicionar `@PreAuthorize("hasRole('COORDENADOR')")` em `POST`/`PUT`/`DELETE` de `AlunoController`. Adicionar `GET /api/v1/alunos/{id}` (ambos os perfis autenticados podem chamar; para PROFESSOR, usa `PertencimentoProfessorGuard.verificar(professorIdDaMatriculaAtiva)` antes de retornar - 404 se não for o dono). **Reescrever `AlunoControllerIT`** para gerar JWT real por perfil em vez de usar os headers `X-Perfil`/`X-Professor-Id` (que deixam de ter efeito, já que `ContextoUsuarioHeaderAdapter` foi removido na T7) - a `T6` só tinha mantido esses headers funcionando temporariamente ao lado do novo header de autenticação; esta task remove essa dependência de vez.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoController.java` (modifica), `.../cadastros/aluno/AlunoService.java` (modifica - adiciona `buscarPorId`), `src/test/java/com/missio/fluencia_leitora/cadastros/aluno/AlunoControllerIT.java` (reescreve)
**Depends on**: T8, T5, T6
**Reuses**: `PertencimentoProfessorGuard`, `JwtService`
**Requirement**: AUTH-07, AUTH-09

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Token de PROFESSOR em `POST`/`PUT`/`DELETE` de aluno → 403
- [ ] `GET /alunos/{id}` com token de COORDENADOR → 200 para qualquer aluno
- [ ] `GET /alunos/{id}` com token de PROFESSOR dono do aluno (via matrícula ativa) → 200
- [ ] `GET /alunos/{id}` com token de PROFESSOR que não é o dono → 404
- [ ] Todos os testes antigos de `AlunoControllerIT` continuam passando, agora com JWT real por perfil em vez de headers
- [ ] `./mvnw verify` passa
- [ ] Testes existentes + pelo menos 4 novos casos (403 escrita, 200 coordenador, 200 dono, 404 não-dono)

**Tests**: integration
**Gate**: full

**Commit**: `feat(autenticacao)!: require COORDENADOR role on aluno write endpoints and add scoped GET by id`

---

### T17: Retrofit `MatriculaController` com `@PreAuthorize`

**What**: Adicionar `@PreAuthorize("hasRole('COORDENADOR')")` em `POST`/`PATCH` de `MatriculaController`. `MatriculaControllerIT` já autentica como `COORDENADOR` desde a `T6`; adicionar aqui só o caso novo com JWT de PROFESSOR esperando 403.
**Where**: `src/main/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaController.java` (modifica), `src/test/java/com/missio/fluencia_leitora/cadastros/aluno/MatriculaControllerIT.java` (modifica)
**Depends on**: T5, T6
**Reuses**: `JwtService`
**Requirement**: AUTH-07

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Token de PROFESSOR em `POST`/`PATCH` de matrícula → 403
- [ ] Tokens de COORDENADOR continuam funcionando
- [ ] `./mvnw verify` passa
- [ ] Testes existentes + pelo menos 1 novo caso de 403

**Tests**: integration
**Gate**: full

**Commit**: `feat(autenticacao)!: require COORDENADOR role on matricula write endpoints`

---

## Phase Execution Map

Arestas intra-fase (dependência real dentro da mesma fase):

```
T1 -> T2
T1 -> T3
T3 -> T4
T2 -> T4
T4 -> T5
T5 -> T6
T5 -> T7
T7 -> T8
T9 -> T10
T9 -> T13
T11 -> T12
```

Arestas cross-fase (backward, validadas pelo check de "forward-phase dependency", não pelo diagrama):

```
T2 -> T9
T2 -> T11
T3 -> T10
T5 -> T14
T5 -> T15
T5 -> T16
T5 -> T17
T6 -> T14
T6 -> T15
T6 -> T16
T6 -> T17
T8 -> T16
```

Fase 3 (`T14`-`T17`) não tem nenhuma aresta intra-fase - as quatro tasks são independentes entre si, cada uma só depende de tasks da Phase 1. Executam em sequência pela ordem de listagem, não por dependência.

As fases em si executam em sequência: Phase 1 → Phase 2 → Phase 3.

Execution is strictly sequential - there is no intra-phase parallelism. A single agent (or batch worker) works one task at a time, in order.

Total: 17 tasks → phases `[8, 5, 4]`. Empacotamento em lotes de ~7 tasks: `{Phase 1 = 8}`, `{Phase 2 + Phase 3 = 9}` → **2 lotes**. Delegação por sub-agentes já confirmada pelo usuário antes do início do Execute; `T6` foi inserida durante o Execute (decisão do usuário, 2026-09-27, ver `.specs/STATE.md`) e mantém o lote 1 dentro do orçamento (8 tasks).

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Dependências | 1 arquivo (`pom.xml`) | ✅ Granular |
| T2: Migração + entidade + repo `Usuario` | 3 arquivos, 1 agregado | ✅ Granular (coeso) |
| T3: `JwtService` | 1 arquivo | ✅ Granular |
| T4: `UsuarioAutenticado` + filtro | 2 arquivos pequenos, 1 conceito | ✅ Granular (coeso) |
| T5: `SecurityConfig` | 1 arquivo | ✅ Granular |
| T6: Helper de JWT em `IntegrationTestBase` + correção dos ITs existentes | 7 arquivos (1 base + 6 ITs), mesma correção mecânica repetida | ✅ Granular (coeso - é uma única correção de infraestrutura de teste) |
| T7: `JwtContextoUsuarioAdapter` (+ remoção do adapter antigo) | 1 arquivo novo + 2 removidos | ✅ Granular (coeso - é uma substituição) |
| T8: `PertencimentoProfessorGuard` | 1 arquivo | ✅ Granular |
| T9: `AuthService.login` | 2 arquivos (service + bean de config) | ✅ Granular (coeso) |
| T10: `AuthController` | 1 controller + 2 DTOs | ✅ Granular |
| T11: `UsuarioService` | 1 arquivo | ✅ Granular |
| T12: `UsuarioController` | 1 controller + DTOs | ✅ Granular |
| T13: `AdminBootstrap` | 1 arquivo | ✅ Granular |
| T14: Retrofit `AnoLetivoController` | 1 controller + seu IT (modificação pontual) | ✅ Granular |
| T15: Retrofit `ProfessorController`+`TurmaController` | 2 controllers + 2 ITs - mesmo padrão mecânico repetido | ✅ Granular (coeso, é o mesmo retrofit em 2 arquivos análogos) |
| T16: Retrofit `AlunoController` + novo endpoint | 1 controller + service + IT reescrito | ✅ Granular (coeso, é uma unidade: o novo endpoint e seu teste) |
| T17: Retrofit `MatriculaController` | 1 controller + seu IT | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | nenhuma seta entrando em T1 | ✅ Match |
| T2 | T1 | T1→T2 | ✅ Match |
| T3 | T1 | T1→T3 | ✅ Match |
| T4 | T3, T2 | T3→T4, T2→T4 | ✅ Match |
| T5 | T4 | T4→T5 | ✅ Match |
| T6 | T5 | T5→T6 | ✅ Match |
| T7 | T5 | T5→T7 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T2 (cross-fase) | sem seta intra-fase exigida | ✅ Match |
| T10 | T9, T3 (cross-fase) | T9→T10 | ✅ Match |
| T11 | T2 (cross-fase) | sem seta intra-fase exigida | ✅ Match |
| T12 | T11 | T11→T12 | ✅ Match |
| T13 | T9 | T9→T13 | ✅ Match |
| T14 | T5, T6 (cross-fase) | sem seta intra-fase exigida | ✅ Match |
| T15 | T5, T6 (cross-fase) | sem seta intra-fase exigida | ✅ Match |
| T16 | T8, T5, T6 (cross-fase) | sem seta intra-fase exigida | ✅ Match |
| T17 | T5, T6 (cross-fase) | sem seta intra-fase exigida | ✅ Match |

Nenhum `Depends on` aponta para uma task de fase posterior.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1: Dependências | Entity/config | none | none | ✅ OK |
| T2: `Usuario` + repo | Repositório com query customizada | integration | integration | ✅ OK |
| T3: `JwtService` | Infra de segurança | unit | unit | ✅ OK |
| T4: Filtro | Infra de segurança | unit | unit | ✅ OK |
| T5: `SecurityConfig` | Controller (efeito observável só via requisição HTTP) | integration | integration | ✅ OK |
| T6: Helper de JWT em ITs existentes | Infraestrutura de teste (correção, não cria camada nova) | none | integration | ⚠️ Ver nota |
| T7: `JwtContextoUsuarioAdapter` | Infra de segurança | unit | unit | ✅ OK |
| T8: `PertencimentoProfessorGuard` | Infra de segurança | unit | unit | ✅ OK |
| T9: `AuthService.login` | Serviço de domínio | unit | unit | ✅ OK |
| T10: `AuthController` | Controller | integration | integration | ✅ OK |
| T11: `UsuarioService` | Serviço de domínio | unit | unit | ✅ OK |
| T12: `UsuarioController` | Controller | integration | integration | ✅ OK |
| T13: `AdminBootstrap` | Entidade/bootstrap (efeito testável sem Docker) | none | unit* | ⚠️ Ver nota |
| T14: Retrofit `AnoLetivoController` | Controller | integration | integration | ✅ OK |
| T15: Retrofit `Professor`/`TurmaController` | Controller | integration | integration | ✅ OK |
| T16: Retrofit `AlunoController` | Controller | integration | integration | ✅ OK |
| T17: Retrofit `MatriculaController` | Controller | integration | integration | ✅ OK |

*Nota T13: a matriz classifica "bootstrap" como `none` (build gate only) por analogia com "entidade/config puro", mas `AdminBootstrap` tem lógica condicional real (AUTH-14 + o edge case do WARN) que merece teste dedicado - por isso a task pede `unit` via `ApplicationContextRunner`, mais rigoroso que o mínimo da matriz, não uma violação dela.

**Nota T6**: task inserida durante o Execute (não prevista na matriz original) para corrigir um gap descoberto ao rodar o gate: `T1`/`T5` introduzem Spring Security, que quebra os `*ControllerIT` de `cadastros-base` por falta de autenticação. A matriz não tem uma categoria para "correção de infraestrutura de teste"; o gate `full`/`./mvnw verify` continua sendo a forma correta de verificar, mesmo sem um caso de teste novo dedicado - o critério de sucesso é a suíte completa voltar a passar sem enfraquecer nenhuma asserção existente.

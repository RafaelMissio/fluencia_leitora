# Fluência Leitora

Sistema web para aplicar e acompanhar **avaliações de fluência em leitura** de alunos. O professor conduz a avaliação (palavras, pseudopalavras ou texto curto) com cronômetro e gravação de áudio, marca acertos e erros, e o sistema classifica o aluno e mantém o histórico e a evolução entre ciclos e anos letivos.

A especificação funcional completa está em [`docs/SDD_Sistema_Avaliacao_Fluencia_Leitora.md`](docs/SDD_Sistema_Avaliacao_Fluencia_Leitora.md).

## Funcionalidades

- **Avaliações de leitura** de três tipos: palavras, pseudopalavras e texto curto.
- **Fluxo da avaliação:** iniciar, pausar, continuar, resetar, finalizar e cancelar, com cronômetro e auditoria dos eventos.
- **Modo refazer:** nova tentativa da avaliação, inclusive uma palavra por vez com correção automática por fala.
- **Avaliações programadas:** a coordenação programa avaliações e o professor as aplica a partir da lista de pendentes.
- **Gravação e reprodução de áudio** da leitura do aluno.
- **Classificação** em Pré-Leitor (níveis 1 a 4), Leitor Iniciante ou Leitor Fluente, segundo regras configuráveis por série, com histórico de alterações.
- **Histórico e evolução** do aluno entre tentativas, ciclos e anos letivos, com gráficos.
- **Cadastros:** anos letivos (com ativação e configuração por série), turmas, alunos, matrículas (com situação), professores e banco de palavras.
- **Perfis:** `COORDENADOR` e `PROFESSOR`. O professor só acessa os recursos que lhe pertencem.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security (JWT via jjwt), Bean Validation, springdoc-openapi |
| Banco | MySQL, migrações com Flyway |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query |
| Testes | JUnit + Testcontainers (backend), Vitest + Testing Library (frontend), Playwright (E2E) |

## Estrutura

```
.
├── src/main/java/com/missio/fluencia_leitora/
│   ├── autenticacao/        login e criação do administrador inicial
│   ├── cadastros/           ano letivo, turma, aluno, professor, domínios
│   ├── bancopalavras/       listas de palavras
│   ├── regrasclassificacao/ regras de classificação por série
│   ├── avaliacao/           avaliações e avaliações programadas
│   ├── audioavaliacao/      armazenamento do áudio
│   ├── historicoevolucao/   histórico e evolução do aluno
│   └── common/              segurança, erros e utilitários
├── src/main/resources/db/migration/   migrações Flyway (V1..V15)
├── frontend/                SPA React (src/features por módulo, e2e/ com Playwright)
├── docs/                    especificação (SDD)
└── data/audios/             áudios gravados (armazenamento local)
```

## Pré-requisitos

- JDK 21
- Node.js e npm
- MySQL 8+ rodando localmente (ou via Docker)
- Docker, apenas para executar os testes de integração do backend (Testcontainers)

## Como executar

### 1. Banco de dados

Crie o schema (as tabelas são criadas pelo Flyway na primeira execução):

```sql
CREATE DATABASE fluencia_leitora CHARACTER SET utf8mb4;
```

### 2. Backend

```bash
export APP_JWT_SECRET='<segredo longo e aleatório>'
export APP_ADMIN_EMAIL='admin@exemplo.com'
export APP_ADMIN_PASSWORD='<senha>'

./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`, com prefixo `/api/v1`. A documentação OpenAPI/Swagger UI fica disponível via springdoc (por padrão em `/swagger-ui.html`).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

O Vite serve a aplicação em `http://localhost:5173` e encaminha `/api` para `http://localhost:8080`, então não é necessário configurar CORS em desenvolvimento.

## Configuração

Variáveis de ambiente lidas pelo backend:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do MySQL |
| `DB_PORT` | `3306` | Porta do MySQL |
| `DB_NAME` | `fluencia_leitora` | Nome do schema |
| `DB_USERNAME` | `root` | Usuário do banco |
| `DB_PASSWORD` | `root` | Senha do banco |
| `APP_JWT_SECRET` | — | Segredo usado para assinar os tokens JWT |
| `APP_ADMIN_EMAIL` | — | E-mail do administrador criado na inicialização |
| `APP_ADMIN_PASSWORD` | — | Senha do administrador criado na inicialização |
| `APP_AUDIO_STORAGE_DIR` | `data/audios` | Diretório onde os áudios são gravados |
| `APP_AUDIO_TAMANHO_MAXIMO_BYTES` | `26214400` (25 MB) | Tamanho máximo de cada áudio |

> Os padrões de banco servem apenas para desenvolvimento. Defina valores próprios em qualquer outro ambiente.

## Principais endpoints

Todos sob `/api/v1`:

| Recurso | Caminho |
|---|---|
| Anos letivos | `/anos-letivos` |
| Turmas | `/turmas` |
| Alunos e matrículas | `/alunos`, `/alunos/{id}/matriculas`, `/matriculas/{id}` |
| Banco de palavras | `/listas-palavras` |
| Regras de classificação | `/regras-classificacao` |
| Avaliações | `/avaliacoes` (`/iniciar`, `/pausar`, `/continuar`, `/finalizar`, `/refazer`, `/audio`, `/auditoria`, `/pendentes`) |
| Avaliações programadas | `/avaliacoes-programadas` |
| Histórico e evolução | `/alunos/{id}/historico-avaliacoes`, `/evolucao-ciclos`, `/evolucao-anos` |

## Testes

```bash
# Backend (requer Docker para o MySQL via Testcontainers)
./mvnw test

# Frontend
cd frontend
npm run test        # unitários (Vitest)
npm run lint
npm run build       # checagem de tipos + build de produção
npm run test:e2e    # Playwright (suba o backend e o frontend antes)
```

## Licença

Projeto sem licença definida.

# Áudio de Avaliação Specification

> Origem: SDD §8, §21 (RF008, RF015), §22 (RNF003), §19 (DDL `avaliacao_audio`, referência). Decisões: AD-003.

## Problem Statement

O professor grava, pelo navegador, a leitura do aluno durante uma avaliação (SDD §8); depois de finalizada, precisa poder reproduzir e baixar esse áudio (RF015). A tabela `avaliacao_audio` do SDD tem uma FK para `avaliacao(id)`, mas a feature `avaliacao` ainda não existe (é a próxima do roadmap, depois desta). Esta feature entrega a capacidade de armazenamento em si - guardar bytes de áudio em disco e devolvê-los de volta - sem depender de `avaliacao` existir, para que `avaliacao` só precise chamá-la quando for construída.

## Goals

- [ ] Dado um array de bytes de áudio válido, `armazenar` grava no disco e devolve uma referência; `recuperar` com essa referência devolve os mesmos bytes, sem perda ou corrupção.
- [ ] Nenhum arquivo é aceito fora da lista de formatos permitidos ou acima do tamanho máximo configurado.
- [ ] O nome do arquivo em disco nunca é derivado de entrada do chamador (elimina path traversal por construção).

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Endpoint HTTP de upload/download (`POST/GET .../avaliacoes/{id}/audio`) | Decisão do usuário - fica na feature `avaliacao`, que expõe o endpoint e chama esta porta internamente (mesmo padrão do `classificar()` em `regras-classificacao`) |
| Tabela `avaliacao_audio` / metadados (nome, mimeType, tamanho, duração, `avaliacaoId`) | Pertence à feature `avaliacao` (FK real para `avaliacao(id)`, que não existe ainda) |
| Exclusão de áudio | AD-003 (`.specs/STATE.md`) já decidiu que os áudios não expiram; RNF006 (`.specs/features/avaliacao/spec.md`) proíbe excluir fisicamente uma avaliação, então nada no fluxo aciona exclusão de áudio |
| Extração automática de duração do áudio (`duracao_segundos`) | Coluna de `avaliacao_audio`, que pertence à feature `avaliacao`; se o cliente enviar essa informação, cabe a `avaliacao` gravá-la, não a este serviço de armazenamento |
| Reconhecimento automático de fala / transcrição | SDD §25, evolução futura |
| Gravação client-side (MediaRecorder API), permissão do microfone (RNF003) | Feature `frontend-web` |
| Troca para armazenamento em nuvem (S3) | AD-003 já decidiu disco local para o MVP; a porta (`AudioStoragePort`) existe justamente para permitir essa troca depois sem mudar quem a chama |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | --------------- | --------- | ---------- |
| Escopo do backend (REST vs. porta pura) | Porta Java pura (`AudioStoragePort` + adapter em disco), sem endpoint HTTP e sem tabela própria | Opção escolhida pelo usuário - mesmo padrão do `classificar()` em `regras-classificacao`: capacidade interna com interface estável para a feature futura consumir | y |
| Lista de mime types permitidos | `audio/webm`, `audio/ogg`, `audio/mp4`, `audio/mpeg`, `audio/wav` | Formatos mais comuns produzidos pela MediaRecorder API dos navegadores (SDD §23, Frontend); o SDD não define uma lista (Ponto 6, "política de armazenamento" - AD-003 resolveu retenção, não formato) | n |
| Tamanho máximo por arquivo | 25 MB, configurável via propriedade (`app.audio.tamanho-maximo-bytes`) | O SDD não define um limite; 25 MB cobre confortavelmente vários minutos de áudio comprimido (webm/opus) sem abrir a porta para uploads arbitrariamente grandes | n |
| Diretório de armazenamento | Configurável via variável de ambiente (`APP_AUDIO_STORAGE_DIR`), com um default sensato para desenvolvimento | AD-003 já decidiu "diretório configurável"; segue o padrão de configuração já usado no projeto (`APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` em `AdminBootstrap`) | n |
| Nome do arquivo em disco | UUID gerado pelo servidor + extensão derivada do mime type permitido; o `nomeOriginal` do chamador nunca é usado para compor o caminho | Elimina colisão entre gravações concorrentes e risco de path traversal por construção, sem precisar sanitizar entrada | n |
| Referência devolvida por `armazenar` | Caminho relativo ao diretório configurado (string opaca) | É o suficiente para `recuperar` localizar o arquivo depois; não expõe o caminho absoluto do disco do servidor | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Armazenar e recuperar áudio ⭐ MVP

**User Story**: Como funcionalidade interna do backend (consumida pela feature `avaliacao`, ainda não construída), preciso de uma porta de armazenamento que grave bytes de áudio em disco e os devolva de volta exatamente iguais, para que a gravação da leitura do aluno (SDD §8) fique disponível para reprodução e download (RF015) quando `avaliacao` existir.

**Why P1**: É a única capacidade desta feature; sem ela, `avaliacao` não tem onde persistir o áudio gravado pelo navegador.

**Acceptance Criteria**:

1. WHEN `armazenar` é chamado com bytes de áudio não vazios, um mime type permitido e tamanho dentro do limite configurado THEN o sistema SHALL gravar o arquivo no diretório configurado e devolver uma referência (caminho relativo) que identifica o arquivo de forma única.
2. WHEN `recuperar` é chamado com uma referência devolvida por um `armazenar` anterior THEN o sistema SHALL devolver os bytes gravados, idênticos byte a byte ao conteúdo original.
3. IF o mime type informado não estiver entre `audio/webm`, `audio/ogg`, `audio/mp4`, `audio/mpeg` e `audio/wav` THEN o sistema SHALL rejeitar o armazenamento sem gravar nada no disco.
4. IF o tamanho do conteúdo exceder o limite configurado THEN o sistema SHALL rejeitar o armazenamento sem gravar nada no disco.
5. IF a escrita em disco falhar (ex.: diretório sem permissão, disco cheio) THEN o sistema SHALL lançar uma exceção e não deixar nenhum arquivo parcial no diretório.
6. IF `recuperar` for chamado com uma referência que não existe no disco THEN o sistema SHALL lançar uma exceção específica, nunca devolver um recurso vazio silenciosamente.
7. The system SHALL criar o diretório configurado automaticamente, se ele ainda não existir, antes da primeira gravação.
8. The system SHALL gerar, para cada gravação, um nome de arquivo em disco que nunca é derivado do nome original enviado pelo chamador.

**Independent Test**: chamar `armazenar` com um array de bytes de teste e um mime type válido; chamar `recuperar` com a referência devolvida e comparar os bytes - devem ser idênticos. Chamar `armazenar` com um mime type inválido e confirmar que a contagem de arquivos no diretório não muda.

---

## Edge Cases

- IF os bytes de áudio tiverem tamanho zero THEN o sistema SHALL rejeitar o armazenamento (mesma família de erro do tamanho acima do limite - tamanho fora da faixa aceitável, que é `[1, limite configurado]`).
- IF o mime type informado for nulo ou vazio THEN o sistema SHALL rejeitar o armazenamento (mesmo comportamento de um mime type fora da lista permitida).
- IF o `nomeOriginal` do chamador contiver separadores de diretório ou sequências como `../` THEN o sistema SHALL ignorá-los ao gerar o nome do arquivo em disco (já garantido pela AC8 - o nome gerado é sempre um UUID, nunca derivado do `nomeOriginal`).
- WHEN duas chamadas a `armazenar` acontecem concorrentemente THEN o sistema SHALL gravar cada uma em um arquivo distinto, sem colisão de nome (garantido pela AC8).

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | AUD-03, AUD-04 (mime type e tamanho); Edge Cases (tamanho zero, mime type nulo) |
| Failure / partial-failure | AUD-05 - falha de escrita não deixa arquivo parcial |
| Idempotency / retry / duplicate handling | N/A because não há endpoint HTTP nesta feature (chamador é código Java interno, sem retry de rede a considerar aqui); cada chamada a `armazenar` sempre cria um novo arquivo, por design (não há conceito de "reenviar a mesma gravação") |
| Auth boundaries & rate limits | N/A because não há endpoint HTTP nesta feature; a feature `avaliacao` decide sua própria autorização quando expuser o endpoint que chama esta porta |
| Concurrency / ordering | Nomes de arquivo únicos (UUID) eliminam colisão entre gravações concorrentes; não há estado compartilhado entre chamadas |
| Data lifecycle / expiry | N/A because AD-003 já decidiu que os áudios não expiram nesta feature |
| Observability | Log INFO na gravação bem-sucedida (referência, tamanho); log WARN na rejeição por mime type/tamanho; log ERROR na falha de escrita |
| External-dependency failure | AUD-05 - a falha do sistema de arquivos (disco cheio, permissão) é tratada como erro explícito, nunca silenciosa |
| State-transition integrity | N/A because não há máquina de estados nesta feature - cada arquivo, uma vez gravado, é imutável |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AUD-01 | P1: Armazenar grava e devolve referência | T3 | Done |
| AUD-02 | P1: Recuperar devolve bytes idênticos | T3 | Done |
| AUD-03 | P1: Mime type fora da lista é rejeitado | T3 | Done |
| AUD-04 | P1: Tamanho acima do limite é rejeitado | T3 | Done |
| AUD-05 | P1: Falha de escrita não deixa arquivo parcial | T3 | Done |
| AUD-06 | P1: Recuperar referência inexistente lança exceção | T3 | Done |
| AUD-07 | P1: Diretório configurado é criado automaticamente | T3 | Done |
| AUD-08 | P1: Nome do arquivo nunca deriva de entrada do chamador | T3 | Done |

**Coverage:** 8 total, 8 mapped to tasks, 8 testáveis agora, 0 unmapped

---

## Success Criteria

- [ ] `armazenar` seguido de `recuperar` com a mesma referência devolve bytes idênticos, para qualquer mime type permitido dentro do limite de tamanho.
- [ ] Nenhuma chamada com mime type inválido ou tamanho fora do limite grava um arquivo no disco.
- [ ] Os testes de integração usam um diretório temporário configurável, nunca um caminho fixo do ambiente do desenvolvedor.

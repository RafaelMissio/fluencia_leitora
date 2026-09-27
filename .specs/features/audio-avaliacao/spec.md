# Áudio da Avaliação Specification

> Origem: SDD §7.1, §7.4, §8, RF008, RF015, RNF003. Decisões: AD-003.

## Problem Statement

O áudio da leitura prova o desempenho do aluno e permite que o professor revise a marcação das palavras. O navegador grava o áudio; o backend precisa receber, guardar, associar à avaliação e servir o arquivo para reprodução e download, sem perda e sem expor arquivos de outros professores.

## Goals

- [ ] Toda avaliação finalizada pode ter exatamente 1 áudio associado.
- [ ] O professor reproduz o áudio com barra de progresso (range requests) e faz o download com nome descritivo.
- [ ] Nenhum arquivo fica sem registro no banco, e nenhum registro fica sem arquivo depois de uma falha.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Captura pelo microfone e pedido de permissão | Feature `frontend-web` (MediaRecorder, RNF003) |
| Transcrição ou reconhecimento de fala | Evolução futura (SDD §25) |
| Conversão de formato ou compressão no servidor | Desnecessária: o navegador já grava em formatos comprimidos (webm/ogg/mp4) |
| Armazenamento em S3 e retenção com expiração | AD-003: disco local sem expiração no MVP |
| Envio em partes (chunked) durante a gravação | Os arquivos são pequenos (~0,5 MB/min em Opus) |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Armazenamento e retenção (SDD §24 item 6) | Disco local em `app.audio.storage-dir`, atrás de uma porta `AudioStorage`; os arquivos não expiram | Opção escolhida pelo usuário (AD-003) | y |
| Quando o envio é aceito | Só com a avaliação `FINALIZADA` | A gravação termina na finalização (SDD §7.4) | n |
| Reenvio | Substitui o áudio anterior (o arquivo antigo é removido depois que o novo é confirmado) e gera auditoria | Permite repetir o envio após falha de rede e atende o RNF004 | n |
| Formatos aceitos | `audio/webm`, `audio/ogg`, `audio/wav`, `audio/mp4`, `audio/mpeg` | Cobre MediaRecorder no Chrome, Firefox e Safari | n |
| Tamanho máximo | 25 MB | 600 s em WAV mono 16 kHz dá cerca de 19 MB; formatos comprimidos ficam bem abaixo | n |
| Duração | Informada pelo cliente (`duracaoSegundos`, opcional) e validada entre 0 e `tempoConfigurado + 5` | Evita ler o conteúdo do áudio no servidor | n |
| Nome físico do arquivo | UUID gerado pelo servidor + extensão derivada do MIME; o nome enviado pelo cliente é descartado | Evita path traversal e colisão de nomes | n |
| Áudio de avaliação cancelada | Mantido e acessível | RNF006 (integridade do histórico) | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Enviar o áudio da avaliação ⭐ MVP

**User Story**: Como professor, quero que o áudio gravado seja salvo junto com a avaliação (RF008).

**Why P1**: É requisito funcional explícito e evidência da avaliação.

**Acceptance Criteria**:
1. WHEN o professor envia `PUT /api/v1/avaliacoes/{id}/audio` (multipart, campo `arquivo`, `duracaoSegundos` opcional) para uma avaliação `FINALIZADA` THEN o sistema SHALL gravar o arquivo com nome UUID, gravar `mimeType`, `tamanhoBytes`, `duracaoSegundos` e `criadoEm` e retornar 201 com os metadados.
2. IF a avaliação não estiver `FINALIZADA` THEN o sistema SHALL retornar 409 com código `AUDIO_NAO_PERMITIDO` e o `statusAtual`.
3. IF o MIME type não estiver na lista permitida THEN o sistema SHALL retornar 415.
4. IF o arquivo tiver mais de 25 MB THEN o sistema SHALL retornar 413.
5. IF o arquivo estiver vazio (0 bytes) ou `duracaoSegundos` estiver fora de 0 a `tempoConfigurado + 5` THEN o sistema SHALL retornar 422.
6. WHEN já existe um áudio e um novo envio é bem-sucedido THEN o sistema SHALL substituir os metadados, remover o arquivo antigo depois de confirmar a transação e gerar auditoria `AUDIO_SUBSTITUIDO` com os dois nomes de arquivo.
7. IF gravar o arquivo em disco falhar THEN o sistema SHALL retornar 503 com código `ARMAZENAMENTO_INDISPONIVEL` e SHALL NOT gravar metadados.
8. IF gravar os metadados no banco falhar depois de o arquivo ter sido escrito THEN o sistema SHALL remover o arquivo recém-escrito e retornar 500.

**Independent Test**: Finalizar uma avaliação, enviar um webm de 100 KB e conferir os metadados e o arquivo no diretório configurado.

---

### P1: Reproduzir e baixar o áudio ⭐ MVP

**User Story**: Como professor, quero ouvir e baixar o áudio de uma avaliação finalizada (RF015, SDD §8).

**Why P1**: É requisito funcional explícito; o professor revisa a marcação ouvindo o áudio.

**Acceptance Criteria**:
1. WHEN um usuário autorizado envia `GET /api/v1/avaliacoes/{id}/audio` THEN o sistema SHALL responder 200 com o conteúdo, `Content-Type` igual ao `mimeType` gravado, `Content-Length` e `Accept-Ranges: bytes`.
2. WHEN a requisição traz o header `Range: bytes=a-b` válido THEN o sistema SHALL responder 206 com apenas o trecho pedido e `Content-Range` correto.
3. WHEN a requisição traz `?download=true` THEN o sistema SHALL incluir `Content-Disposition: attachment; filename="avaliacao-{id}-{nome-aluno-sem-acentos-com-hifens}-{yyyy-MM-dd}.{ext}"`.
4. IF a avaliação não tiver áudio THEN o sistema SHALL retornar 404 com código `AUDIO_INEXISTENTE`.
5. IF os metadados existirem mas o arquivo não estiver no disco THEN o sistema SHALL retornar 404 com código `AUDIO_ARQUIVO_AUSENTE` e registrar log ERROR com `avaliacaoId` e caminho.
6. IF um PROFESSOR pedir o áudio de uma avaliação que não é dele THEN o sistema SHALL retornar 404 (AUTH-09).
7. The system SHALL permitir ao COORDENADOR reproduzir e baixar qualquer áudio.
8. IF o `Range` pedido não for satisfazível THEN o sistema SHALL retornar 416.

**Independent Test**: `GET` com `Range: bytes=0-99` retorna 206 com 100 bytes.

---

## Edge Cases

- The system SHALL resolver o caminho físico só a partir do diretório configurado + nome UUID, e SHALL NOT usar nenhum dado do cliente para montar o caminho.
- IF `app.audio.storage-dir` não existir ou não tiver permissão de escrita na inicialização THEN a aplicação SHALL falhar ao iniciar, com mensagem que indica o diretório.
- WHEN a avaliação é cancelada THEN o sistema SHALL manter o áudio.
- IF dois envios simultâneos chegarem para a mesma avaliação THEN o sistema SHALL aceitar o que confirmar primeiro e retornar 409 `CONFLITO_DE_VERSAO` para o outro, removendo o arquivo órfão dele.

### Implicit-requirement dimensions sweep

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | AUD-03..AUD-05 |
| Failure / partial-failure | AUD-07, AUD-08; o arquivo antigo só é apagado depois da confirmação (AUD-06) |
| Idempotency / duplicates | Reenviar substitui o áudio (AUD-06) |
| Auth boundaries & rate limits | AUD-13, AUD-14; limite de tamanho (AUD-04) |
| Concurrency / ordering | Lock otimista na avaliação; o arquivo órfão é removido |
| Data lifecycle | Sem expiração (AD-003); mantido mesmo em cancelamento |
| Observability | Log ERROR em arquivo ausente (AUD-12); log INFO em cada envio com tamanho e MIME |
| External-dependency failure | Disco indisponível dá 503 (AUD-07); o diretório é validado na inicialização |
| State-transition integrity | Envio só em FINALIZADA (AUD-02) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AUD-01 | P1: Envio em avaliação finalizada | - | Pending |
| AUD-02 | P1: Envio bloqueado fora de FINALIZADA | - | Pending |
| AUD-03 | P1: MIME permitido | - | Pending |
| AUD-04 | P1: Limite de 25 MB | - | Pending |
| AUD-05 | P1: Arquivo vazio e duração inválida | - | Pending |
| AUD-06 | P1: Substituição com auditoria | - | Pending |
| AUD-07 | P1: Falha de disco sem metadados | - | Pending |
| AUD-08 | P1: Falha de banco remove o arquivo | - | Pending |
| AUD-09 | P1: Streaming 200 | - | Pending |
| AUD-10 | P1: Range 206 / 416 | - | Pending |
| AUD-11 | P1: Download com nome descritivo | - | Pending |
| AUD-12 | P1: 404 sem áudio / arquivo ausente | - | Pending |
| AUD-13 | P1: Escopo do professor | - | Pending |
| AUD-14 | P1: Acesso do coordenador | - | Pending |
| AUD-15 | P1: Caminho seguro e validação do diretório | - | Pending |

**Coverage:** 15 total, 0 mapped to tasks, 15 unmapped ⚠️

---

## Success Criteria

- [ ] Teste de integração: enviar, reproduzir com Range, baixar e substituir, sem arquivo órfão no diretório ao final.
- [ ] Teste com falha de disco simulada: 503 e nenhum registro no banco.

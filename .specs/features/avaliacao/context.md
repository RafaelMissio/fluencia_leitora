# Avaliação de Leitura Context

**Gathered:** 2026-09-28
**Spec:** `.specs/features/avaliacao/spec.md`
**Status:** Ready for design

---

## Feature Boundary

O professor cria, executa (iniciar/pausar/continuar/resetar/finalizar) e cancela uma avaliação cronometrada de leitura, marca cada palavra como correta/incorreta/não lida, e o sistema calcula resultado e classificação. Áudio, cronômetro visual/microfone e reconhecimento de fala ficam fora (outras features); histórico/evolução também.

---

## Implementation Decisions

### Auto-finalização de avaliações abandonadas

- Finalização preguiçosa: se um comando chegar numa avaliação `EM_ANDAMENTO` cujo tempo somado já é ≥ `tempoConfigurado`, o servidor finaliza antes de processar o comando (sem exigir um `motivo` explícito do frontend).
- Além disso, uma rotina agendada de hora em hora finaliza toda avaliação `EM_ANDAMENTO` parada há mais de 24h, para que sessões abandonadas (navegador fechado, nenhum novo request) não fiquem presas indefinidamente.

### Escopo do "resetar"

- Reset completo: volta para `CRIADA`, zera o tempo somado, limpa `iniciadoEm` e devolve **todas** as palavras para `PENDENTE` - o professor recomeça a leitura do zero, mesmo que já tivesse marcado algumas palavras.

### Edição depois de finalizar

- Permitida: o professor pode mudar o status de uma palavra numa avaliação `FINALIZADA`. Cada mudança gera um registro de auditoria (usuário, data/hora, valor anterior, valor novo) e recalcula resultado e classificação.
- O recálculo usa as faixas de `regras-classificacao` **ativas no momento do recálculo** - a avaliação não guarda uma cópia/versão da regra usada na primeira finalização. A auditoria registra a classificação anterior e a nova.

### Alcance do cancelamento

- Cancelamento permitido a partir de qualquer status, exceto `CANCELADA`, incluindo `FINALIZADA` - com justificativa de 10 a 500 caracteres, gerando registro de auditoria com o status anterior.

### Envio e download do áudio (gap encontrado durante o Design, resolvido antes de continuar)

- `audio-avaliacao/spec.md` deferiu explicitamente o endpoint HTTP e a tabela `avaliacao_audio` para esta feature (RF015); o spec.md original não tinha essa AC - foi adicionada agora (AVA-27..AVA-32) em vez de deixar RF015 sem dono.
- Upload só é aceito numa avaliação `FINALIZADA` (grava-se continuamente no navegador; um único `POST` no fim).
- Escrita única: um segundo `POST` para a mesma avaliação retorna 409 `AUDIO_JA_ENVIADO`; não há substituição.
- Cancelar uma avaliação com áudio não apaga o arquivo (AD-003: áudio não expira).

### Agent's Discretion

Nenhuma - todas as áreas discutidas foram decididas explicitamente pelo usuário.

### Declined / Undiscussed Gray Areas → Assumptions

Não discutidas em detalhe (baixo risco, já alinhadas com exemplos do SDD); registradas como assumptions confirmadas no spec:

- Cômputo do tempo: soma de segmentos via timestamps de iniciar/pausar/continuar, `tempoUtilizado = min(soma, tempoConfigurado)`.
- Arredondamento do `percentualAcerto`: `corretas / total × 100`, 2 casas, HALF_UP (bate com o exemplo do SDD §13).
- Limites de `tempoSegundos`: 10-600, padrão 60.
- `dataAvaliacao`: dentro do período do ano letivo ATIVO e nunca no futuro.
- Palavras digitadas sem `tipoPalavra`: aceitas como `null`; a restrição do 1º ano só vale quando o tipo é informado.

---

## Specific References

Nenhuma referência visual/de produto específica - a interação é via API; o comportamento visual do cronômetro/microfone fica na feature `frontend-web`.

---

## Deferred Ideas

Nenhuma - a discussão ficou dentro do escopo da feature.

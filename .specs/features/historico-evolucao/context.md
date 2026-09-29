# Histórico e Evolução Context

**Gathered:** 2026-09-28
**Spec:** `.specs/features/historico-evolucao/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Consultas somente leitura sobre avaliações `FINALIZADA` já gravadas pela feature `avaliacao`: histórico do aluno (RF012), evolução entre os três ciclos de um ano letivo (RF013) e comparação de desempenho entre anos letivos (RF014). Não cria, altera nem recalcula nenhuma avaliação; UI fica para `frontend-web`.

---

## Implementation Decisions

### Escopo da comparação anual (RF014)

- Só por aluno individual, uma linha por ano letivo - sem agregação por turma/série. Bate com o exemplo do SDD §15 (uma linha por ano do mesmo aluno).

### Métrica de evolução

- `quantidade_corretas` é o valor usado nas fórmulas de evolução absoluta/percentual (§14 e §15), não `percentual_acerto`. Bate literalmente com as tabelas de exemplo do SDD ("Corretas" como coluna), mesmo não sendo normalizado pelo tamanho da lista de palavras por série.

### Alinhamento da evolução entre anos

- Mesmo ciclo, ano a ano: Entrada do ano atual vs Entrada do ano anterior, Acompanhamento vs Acompanhamento, Saída vs Saída. Não compara Saída do ano anterior com Entrada do ano atual.

### Acesso ao RF013 (evolução por ciclo)

- Só Coordenador. O SDD (§17.1/§17.2) só atribui explicitamente RF014 ao Coordenador e RF012 (histórico) ao Professor; RF013 fica sem dono explícito no texto. Perguntado ao usuário, que decidiu tratar RF013 como relatório gerencial (mesmo padrão restrito do RF014), não como extensão do histórico do Professor.

### Divisão por zero no percentual

- Resultado anterior = 0 e atual = 0 → percentual = 0.
- Resultado anterior = 0 e atual > 0 → percentual = `null` (indefinido); a evolução absoluta continua sendo calculada normalmente.

### Agent's Discretion

Nenhuma decisão de produto foi tomada por conta própria sem registro - as quatro perguntas de maior impacto (escopo do RF014, métrica, alinhamento anual, acesso ao RF013) foram levadas ao usuário via pergunta direta antes de escrever o spec.

### Declined / Undiscussed Gray Areas → Assumptions

Não levadas ao usuário (baixo risco, resolvidas por precedente do próprio projeto); registradas como assumptions confirmadas no spec:

- Acesso ao histórico (RF012): Professor (só seus alunos, via matrícula ativa) + Coordenador - segue o padrão `AUTH-09` já usado em `avaliacao`/`cadastros-base`.
- Ownership do Professor verificado pela matrícula **ativa** do aluno, não pelo snapshot de professor de cada avaliação (uma avaliação antiga pode citar um professor que já não é mais responsável pelo aluno).
- Histórico/evoluções só consideram avaliações `FINALIZADA` (não `CANCELADA`/`CRIADA`/`EM_ANDAMENTO`/`PAUSADA`), pois são as únicas com resultado consolidado.
- Grupo (aluno, ano, tipo, ciclo) com mais de uma `FINALIZADA`: usa a mais recente por `finalizadoEm` - reaproveita `AD-004`, já implementado.
- `tipoLeitura` obrigatório em RF013/RF014 (os três tipos não são comparáveis na mesma série temporal); opcional (com filtro) no histórico.
- `anoLetivoId` opcional na evolução por ciclo, default = ano ATIVO.
- Paginação do histórico: página de tamanho 20, mesmo padrão de `AlunoController`.

---

## Specific References

Nenhuma referência visual - a interação é via API; comportamento visual fica em `frontend-web`.

---

## Deferred Ideas

- Comparação agregada por turma/série entre anos (só aluno individual por enquanto - ver Out of Scope no spec).
- Exportação de histórico/relatórios (PDF, planilha) - não pedida, sem menção no SDD para esta feature.

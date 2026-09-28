-- T14: `alterado_em` é usada como chave de agrupamento do histórico (REG-15,
-- RegraClassificacaoService.buscarHistorico). TIMESTAMP sem casas decimais
-- (V7) trunca para o segundo - duas substituições na mesma série dentro do
-- mesmo segundo ficam com o mesmo `alterado_em` e o histórico as funde num
-- só grupo, embora sejam eventos distintos. TIMESTAMP(6) (precisão de
-- microssegundo, a mesma do `Instant` da entidade) resolve a colisão.
ALTER TABLE regra_classificacao MODIFY COLUMN alterado_em TIMESTAMP(6) NULL;

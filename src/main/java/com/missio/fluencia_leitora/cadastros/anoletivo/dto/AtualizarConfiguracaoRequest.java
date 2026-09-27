package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

/**
 * CAD-06: novos limites de palavras para uma série. A validação de negócio
 * (faixas permitidas, mínimo &lt;= máximo) é feita pelo
 * {@code ConfiguracaoAvaliacaoService}, não aqui, para manter um único lugar
 * com os códigos de erro específicos do domínio.
 */
public record AtualizarConfiguracaoRequest(int quantidadeMinima, int quantidadeMaxima) {
}

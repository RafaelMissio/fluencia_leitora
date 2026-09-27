package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import com.missio.fluencia_leitora.cadastros.anoletivo.ConfiguracaoAvaliacao;

public record ConfiguracaoAvaliacaoResponse(
        Long id, int serie, int quantidadeMinima, int quantidadeMaxima) {

    public static ConfiguracaoAvaliacaoResponse from(ConfiguracaoAvaliacao configuracaoAvaliacao) {
        return new ConfiguracaoAvaliacaoResponse(
                configuracaoAvaliacao.getId(),
                configuracaoAvaliacao.getSerie(),
                configuracaoAvaliacao.getQuantidadeMinima(),
                configuracaoAvaliacao.getQuantidadeMaxima());
    }
}

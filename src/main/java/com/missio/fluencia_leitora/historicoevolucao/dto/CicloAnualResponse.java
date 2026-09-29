package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoValor;

import java.math.BigDecimal;

/** HIST-12..19: resultado + evolução de um ciclo numa linha anual (design.md, Data Models). */
public record CicloAnualResponse(
        String ciclo,
        int quantidadeCorretas,
        BigDecimal percentualAcerto,
        String fase,
        Integer nivel,
        EvolucaoValor evolucao) {

    /** {@code null} quando o ciclo não tem avaliação `FINALIZADA` nesse ano (mesma regra de {@link ResultadoCicloResponse}). */
    public static CicloAnualResponse from(Avaliacao avaliacao, EvolucaoValor evolucao) {
        if (avaliacao == null) {
            return null;
        }
        return new CicloAnualResponse(
                avaliacao.getCiclo().getCodigo(),
                avaliacao.getQuantidadeCorretas(),
                avaliacao.getPercentualAcerto(),
                avaliacao.getFase() == null ? null : avaliacao.getFase().name(),
                avaliacao.getNivel(),
                evolucao);
    }
}

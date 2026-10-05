package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** HIST-07..11: resultado de um ciclo na evolução (design.md, Data Models). */
public record ResultadoCicloResponse(
        String ciclo,
        String nomeAvaliacao,
        int tentativas,
        LocalDate dataAvaliacao,
        int quantidadeCorretas,
        BigDecimal percentualAcerto,
        String fase,
        Integer nivel) {

    /** HIST-08: {@code null} (não um record vazio) quando o ciclo não tem avaliação {@code FINALIZADA}. */
    public static ResultadoCicloResponse from(Avaliacao avaliacao) {
        if (avaliacao == null) {
            return null;
        }
        return new ResultadoCicloResponse(
                avaliacao.getCiclo().getCodigo(),
                avaliacao.getNomeAvaliacao(),
                avaliacao.getNumeroTentativa(),
                avaliacao.getDataAvaliacao(),
                avaliacao.getQuantidadeCorretas(),
                avaliacao.getPercentualAcerto(),
                avaliacao.getFase() == null ? null : avaliacao.getFase().name(),
                avaliacao.getNivel());
    }
}

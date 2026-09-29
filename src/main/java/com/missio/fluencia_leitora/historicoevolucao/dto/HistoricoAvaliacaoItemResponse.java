package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** HIST-01..06, HIST-22: item de uma página do histórico (design.md, Data Models). */
public record HistoricoAvaliacaoItemResponse(
        Long avaliacaoId,
        int anoLetivo,
        int serie,
        String turma,
        String professor,
        String ciclo,
        String tipoLeitura,
        LocalDate dataAvaliacao,
        int quantidadeTotal,
        int quantidadeCorretas,
        int quantidadeIncorretas,
        int quantidadeNaoLidas,
        BigDecimal percentualAcerto,
        String fase,
        Integer nivel,
        Integer tempoUtilizadoSegundos,
        boolean temAudio) {

    public static HistoricoAvaliacaoItemResponse from(Avaliacao avaliacao, boolean temAudio) {
        return new HistoricoAvaliacaoItemResponse(
                avaliacao.getId(),
                avaliacao.getAnoLetivo().getAno(),
                avaliacao.getSerie(),
                avaliacao.getTurmaNome(),
                avaliacao.getProfessorNome(),
                avaliacao.getCiclo().getCodigo(),
                avaliacao.getTipoLeitura().name(),
                avaliacao.getDataAvaliacao(),
                avaliacao.getQuantidadeTotal(),
                avaliacao.getQuantidadeCorretas(),
                avaliacao.getQuantidadeIncorretas(),
                avaliacao.getQuantidadeNaoLidas(),
                avaliacao.getPercentualAcerto(),
                avaliacao.getFase() == null ? null : avaliacao.getFase().name(),
                avaliacao.getNivel(),
                avaliacao.getTempoUtilizadoSegundos(),
                temAudio);
    }
}

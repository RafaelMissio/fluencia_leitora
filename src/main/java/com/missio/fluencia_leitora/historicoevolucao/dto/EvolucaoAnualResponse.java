package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoAnualLinha;

import java.util.List;

/** HIST-12..19: comparação de desempenho entre anos letivos (design.md, Data Models). */
public record EvolucaoAnualResponse(Long alunoId, String tipoLeitura, List<EvolucaoAnualLinhaResponse> anos) {

    public static EvolucaoAnualResponse from(Long alunoId, TipoLeituraCodigo tipoLeitura, List<EvolucaoAnualLinha> linhas) {
        return new EvolucaoAnualResponse(
                alunoId, tipoLeitura.name(), linhas.stream().map(EvolucaoAnualLinhaResponse::from).toList());
    }
}

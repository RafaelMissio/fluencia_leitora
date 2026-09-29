package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoAnualLinha;

/** HIST-12..19: uma linha (ano letivo) da comparação anual (design.md, Data Models). */
public record EvolucaoAnualLinhaResponse(
        int anoLetivo,
        int serie,
        CicloAnualResponse entrada,
        CicloAnualResponse acompanhamento,
        CicloAnualResponse saida) {

    public static EvolucaoAnualLinhaResponse from(EvolucaoAnualLinha linha) {
        return new EvolucaoAnualLinhaResponse(
                linha.anoLetivo(),
                linha.serie(),
                CicloAnualResponse.from(linha.entrada(), linha.evolucaoEntrada()),
                CicloAnualResponse.from(linha.acompanhamento(), linha.evolucaoAcompanhamento()),
                CicloAnualResponse.from(linha.saida(), linha.evolucaoSaida()));
    }
}

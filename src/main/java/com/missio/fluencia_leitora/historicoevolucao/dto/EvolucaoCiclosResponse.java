package com.missio.fluencia_leitora.historicoevolucao.dto;

import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoCiclos;

/** HIST-07..11: os três ciclos de um ano letivo (design.md, Data Models). */
public record EvolucaoCiclosResponse(
        Long alunoId,
        int anoLetivo,
        String tipoLeitura,
        ResultadoCicloResponse entrada,
        ResultadoCicloResponse acompanhamento,
        ResultadoCicloResponse saida) {

    public static EvolucaoCiclosResponse from(EvolucaoCiclos evolucaoCiclos) {
        return new EvolucaoCiclosResponse(
                evolucaoCiclos.alunoId(),
                evolucaoCiclos.anoLetivo(),
                evolucaoCiclos.tipoLeitura().name(),
                ResultadoCicloResponse.from(evolucaoCiclos.entrada()),
                ResultadoCicloResponse.from(evolucaoCiclos.acompanhamento()),
                ResultadoCicloResponse.from(evolucaoCiclos.saida()));
    }
}

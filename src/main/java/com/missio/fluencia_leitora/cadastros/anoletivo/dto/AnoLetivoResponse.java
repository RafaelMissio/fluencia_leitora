package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;

import java.time.LocalDate;

public record AnoLetivoResponse(
        Long id, int ano, LocalDate dataInicio, LocalDate dataFim, String situacao, boolean ativo) {

    public static AnoLetivoResponse from(AnoLetivo anoLetivo) {
        return new AnoLetivoResponse(
                anoLetivo.getId(),
                anoLetivo.getAno(),
                anoLetivo.getDataInicio(),
                anoLetivo.getDataFim(),
                anoLetivo.getSituacao().name(),
                anoLetivo.isAtivo());
    }
}

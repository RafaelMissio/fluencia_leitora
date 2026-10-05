package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.AvaliacaoProgramada;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;

public record AvaliacaoProgramadaResponse(
        Long id,
        Long anoLetivoId,
        String nome,
        int serie,
        Long cicloId,
        TipoLeituraCodigo tipoLeitura,
        int tempoSegundos,
        Long listaPalavrasId,
        String palavras,
        String texto,
        int maxRefazeres) {

    public static AvaliacaoProgramadaResponse from(AvaliacaoProgramada programada) {
        return new AvaliacaoProgramadaResponse(
                programada.getId(),
                programada.getAnoLetivo().getId(),
                programada.getNome(),
                programada.getSerie(),
                programada.getCiclo().getId(),
                programada.getTipoLeitura(),
                programada.getTempoSegundos(),
                programada.getListaPalavrasId(),
                programada.getPalavras(),
                programada.getTexto(),
                programada.getMaxRefazeres());
    }
}

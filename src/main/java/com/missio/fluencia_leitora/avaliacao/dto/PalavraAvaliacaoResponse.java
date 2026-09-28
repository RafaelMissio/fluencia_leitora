package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.PalavraAvaliacao;
import com.missio.fluencia_leitora.avaliacao.StatusPalavra;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;

/** Uma palavra de {@link AvaliacaoResponse}, na sua ordem, com o status de leitura. */
public record PalavraAvaliacaoResponse(int ordem, String palavra, TipoPalavra tipoPalavra, StatusPalavra status) {

    public static PalavraAvaliacaoResponse from(PalavraAvaliacao palavra) {
        return new PalavraAvaliacaoResponse(
                palavra.getOrdem(), palavra.getPalavra(), palavra.getTipoPalavra(), palavra.getStatus());
    }
}

package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;

/**
 * Uma palavra digitada pelo professor (AVA-01). {@code tipoPalavra} é
 * opcional (spec.md, Assumptions). O formato de {@code palavra} (vazia, mais
 * de 60 caracteres, caracteres fora de letras e hífen) é validado no
 * service, que devolve a posição da palavra (AVA-08) - a mesma regra vale
 * para os tokens de um {@code texto}. Espaços nas bordas são removidos aqui,
 * como em {@code ItemPalavraRequest}.
 */
public record PalavraDigitadaRequest(String palavra, TipoPalavra tipoPalavra) {

    public PalavraDigitadaRequest {
        palavra = palavra == null ? null : palavra.trim();
    }
}

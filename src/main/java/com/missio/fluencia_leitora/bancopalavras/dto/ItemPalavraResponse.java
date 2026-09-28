package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.ItemListaPalavras;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;

/** PAL-11: um item de {@link ListaPalavrasResponse}, na sua ordem original. */
public record ItemPalavraResponse(String palavra, TipoPalavra tipoPalavra, int ordem) {

    public static ItemPalavraResponse from(ItemListaPalavras item) {
        return new ItemPalavraResponse(item.getPalavra(), item.getTipoPalavra(), item.getOrdem());
    }
}

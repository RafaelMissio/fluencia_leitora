package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.ListaPalavras;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;

import java.util.List;

/**
 * PAL-11: detalhe completo de uma lista, incluindo os itens na ordem e,
 * quando {@code tipoLeitura == TEXTO_CURTO}, o texto original ("se for
 * TEXTO_CURTO, também o texto original" - spec.md). Para os outros tipos,
 * {@code texto} vem {@code null}.
 */
public record ListaPalavrasResponse(
        Long id,
        String nome,
        int serie,
        TipoLeituraCodigo tipoLeitura,
        TipoPalavra tipoPalavra,
        String texto,
        boolean ativo,
        long quantidadePalavras,
        List<ItemPalavraResponse> itens) {

    public static ListaPalavrasResponse from(ListaPalavras lista) {
        List<ItemPalavraResponse> itens = lista.getItens().stream().map(ItemPalavraResponse::from).toList();
        String texto = lista.getTipoLeitura() == TipoLeituraCodigo.TEXTO_CURTO ? lista.getTexto() : null;
        return new ListaPalavrasResponse(
                lista.getId(),
                lista.getNome(),
                lista.getSerie(),
                lista.getTipoLeitura(),
                lista.getTipoPalavra(),
                texto,
                lista.isAtivo(),
                itens.size(),
                itens);
    }
}

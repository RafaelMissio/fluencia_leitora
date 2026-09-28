package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.ListaPalavrasResumoProjection;

/** PAL-10: item do `GET` filtrado por série + tipo de leitura. */
public record ListaPalavrasResumoResponse(Long id, String nome, Long quantidadePalavras) {

    public static ListaPalavrasResumoResponse from(ListaPalavrasResumoProjection projection) {
        return new ListaPalavrasResumoResponse(
                projection.getId(), projection.getNome(), projection.getQuantidadePalavras());
    }
}

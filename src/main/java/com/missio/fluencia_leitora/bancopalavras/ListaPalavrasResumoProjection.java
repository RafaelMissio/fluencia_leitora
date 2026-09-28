package com.missio.fluencia_leitora.bancopalavras;

/**
 * Projeção Spring Data para o resumo de uma lista no {@code GET} filtrado
 * (PAL-10): evita carregar a coleção {@code itens} inteira só para contar.
 */
public interface ListaPalavrasResumoProjection {

    Long getId();

    String getNome();

    Long getQuantidadePalavras();
}

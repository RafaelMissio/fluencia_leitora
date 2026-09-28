package com.missio.fluencia_leitora.bancopalavras;

/**
 * Espelha os códigos de {@code cadastros.dominio.TipoLeitura} como enum
 * Java nativo (coluna {@code ENUM} em {@code lista_palavras}), seguindo o
 * precedente de {@code usuario.perfil} em vez de FK - ver design.md, Tech
 * Decisions.
 */
public enum TipoLeituraCodigo {
    PALAVRA,
    PSEUDOPALAVRA,
    TEXTO_CURTO
}

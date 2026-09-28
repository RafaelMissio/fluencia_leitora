package com.missio.fluencia_leitora.regrasclassificacao;

/**
 * Fase de leitura resultante da classificação (design.md, Data Models).
 * {@code PRE_LEITOR} tem um {@code nivel} de 1 a 4; as outras duas não têm
 * nível.
 */
public enum Fase {
    PRE_LEITOR,
    LEITOR_INICIANTE,
    LEITOR_FLUENTE
}

package com.missio.fluencia_leitora.cadastros.anoletivo;

/**
 * Ciclo de vida de negócio do ano letivo: PLANEJADO -> ATIVO -> ENCERRADO,
 * com no máximo um ATIVO por vez (CAD-04). Independente do soft-delete
 * ({@code AnoLetivo.ativo}), que trata de exclusão (RNF006).
 */
public enum SituacaoAnoLetivo {
    PLANEJADO,
    ATIVO,
    ENCERRADO
}

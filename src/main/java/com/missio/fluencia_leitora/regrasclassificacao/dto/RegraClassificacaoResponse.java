package com.missio.fluencia_leitora.regrasclassificacao.dto;

import com.missio.fluencia_leitora.regrasclassificacao.Fase;
import com.missio.fluencia_leitora.regrasclassificacao.RegraClassificacao;

import java.time.Instant;

/** Uma faixa, como devolvida pelos endpoints de leitura (design.md, DTOs). */
public record RegraClassificacaoResponse(
        Long id,
        int serie,
        Integer quantidadeMinimaAcertos,
        Integer quantidadeMaximaAcertos,
        Fase fase,
        Integer nivel,
        boolean ativo,
        Long alteradoPor,
        Instant alteradoEm) {

    public static RegraClassificacaoResponse from(RegraClassificacao regra) {
        return new RegraClassificacaoResponse(
                regra.getId(),
                regra.getSerie(),
                regra.getQuantidadeMinimaAcertos(),
                regra.getQuantidadeMaximaAcertos(),
                regra.getFase(),
                regra.getNivel(),
                regra.isAtivo(),
                regra.getAlteradoPor(),
                regra.getAlteradoEm());
    }
}

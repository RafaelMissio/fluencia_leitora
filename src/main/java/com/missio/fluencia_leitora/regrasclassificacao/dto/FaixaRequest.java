package com.missio.fluencia_leitora.regrasclassificacao.dto;

import com.missio.fluencia_leitora.regrasclassificacao.Fase;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Uma faixa do payload de {@code PUT .../series/{serie}}. {@code
 * quantidadeMaximaAcertos} é nullable (sem limite, só a última faixa);
 * {@code nivel} não tem anotação de bound aqui porque a faixa válida (1-4
 * só quando {@code fase=PRE_LEITOR}, null caso contrário) é uma regra
 * cruzada entre campos - validada no service (design.md, DTOs).
 */
public record FaixaRequest(
        @NotNull @Min(0) Integer quantidadeMinimaAcertos,
        @Min(0) Integer quantidadeMaximaAcertos,
        @NotNull Fase fase,
        Integer nivel) {
}

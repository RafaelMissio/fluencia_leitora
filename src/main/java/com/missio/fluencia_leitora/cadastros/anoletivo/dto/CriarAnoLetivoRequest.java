package com.missio.fluencia_leitora.cadastros.anoletivo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** CAD-01/CAD-03: payload de criação do ano letivo. */
@DataFimPosteriorADataInicio
public record CriarAnoLetivoRequest(
        @Min(2000) @Max(2100) int ano,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim) {
}

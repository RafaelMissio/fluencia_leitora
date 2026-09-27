package com.missio.fluencia_leitora.cadastros.turma.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CAD-07: payload de criação da turma. `professorId` é opcional. */
public record CriarTurmaRequest(
        @NotBlank @Size(min = 1, max = 100) String nome,
        @Min(1) @Max(5) int serie,
        @NotNull Long anoLetivoId,
        Long professorId) {
}

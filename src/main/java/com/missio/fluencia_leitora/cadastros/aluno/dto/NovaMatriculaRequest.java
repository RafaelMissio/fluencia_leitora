package com.missio.fluencia_leitora.cadastros.aluno.dto;

import jakarta.validation.constraints.NotNull;

/** CAD-12: turma de destino da nova matrícula (em outro ano letivo). */
public record NovaMatriculaRequest(@NotNull Long turmaId) {
}

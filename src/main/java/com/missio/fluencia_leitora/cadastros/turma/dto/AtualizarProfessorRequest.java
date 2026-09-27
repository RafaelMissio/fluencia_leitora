package com.missio.fluencia_leitora.cadastros.turma.dto;

import jakarta.validation.constraints.NotNull;

/** CAD-10: novo professor responsável pela turma. */
public record AtualizarProfessorRequest(@NotNull Long professorId) {
}

package com.missio.fluencia_leitora.cadastros.aluno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CAD-11: payload de criação do aluno com a primeira matrícula. */
public record CriarAlunoRequest(@NotBlank @Size(min = 3, max = 150) String nome, @NotNull Long turmaId) {
}

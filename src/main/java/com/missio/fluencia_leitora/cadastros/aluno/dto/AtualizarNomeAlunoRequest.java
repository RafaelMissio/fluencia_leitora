package com.missio.fluencia_leitora.cadastros.aluno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CAD-15: novo nome do aluno. */
public record AtualizarNomeAlunoRequest(@NotBlank @Size(min = 3, max = 150) String nome) {
}

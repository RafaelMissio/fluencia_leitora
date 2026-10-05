package com.missio.fluencia_leitora.cadastros.aluno.dto;

import jakarta.validation.constraints.NotNull;

/** Status do aluno: ativo ou inativo. */
public record AlterarSituacaoAlunoRequest(@NotNull Boolean ativo) {
}

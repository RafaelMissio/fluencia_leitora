package com.missio.fluencia_leitora.avaliacao.dto;

import jakarta.validation.constraints.NotNull;

public record AplicarAvaliacaoProgramadaRequest(@NotNull Long alunoId) {
}

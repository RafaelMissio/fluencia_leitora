package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.StatusPalavra;
import jakarta.validation.constraints.NotNull;

/** AVA-15: novo status de uma palavra ({@code PUT .../palavras/{ordem}}). */
public record MarcarPalavraRequest(@NotNull StatusPalavra status) {
}

package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.avaliacao.StatusPalavra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * AVA-15: marcação em lote ({@code PUT .../palavras}), gravada tudo ou nada.
 * {@code @NotNull} no elemento da lista aplica a lição L-023.
 */
public record MarcarPalavrasRequest(@NotNull @Valid List<@NotNull MarcacaoItem> itens) {

    /** Um item do lote: a posição da palavra e o novo status. */
    public record MarcacaoItem(@NotNull Integer ordem, @NotNull StatusPalavra status) {
    }
}

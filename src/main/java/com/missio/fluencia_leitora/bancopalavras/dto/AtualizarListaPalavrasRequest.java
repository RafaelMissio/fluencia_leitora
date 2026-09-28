package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PAL-06: mesmos campos de {@link CriarListaPalavrasRequest} mais
 * {@code version}, usada pelo lock otimista da edição (409
 * {@code CONFLITO_DE_VERSAO} quando divergente).
 */
public record AtualizarListaPalavrasRequest(
        @NotBlank @Size(min = 3, max = 100) String nome,
        @NotNull @Min(1) @Max(5) Integer serie,
        @NotNull TipoLeituraCodigo tipoLeitura,
        TipoPalavra tipoPalavra,
        @Size(min = 1, max = 2000) String texto,
        @Valid @Size(min = 1, max = 200) List<ItemPalavraRequest> itens,
        @NotNull Long version) {
}

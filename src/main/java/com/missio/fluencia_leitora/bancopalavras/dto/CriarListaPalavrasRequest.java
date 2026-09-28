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
 * PAL-01/PAL-03/PAL-05/PAL-07: payload de criação de uma lista de
 * palavras/pseudopalavras (com {@code itens}) ou de um texto curto (com
 * {@code texto} + {@code tipoPalavra}). A compatibilidade entre
 * {@code tipoLeitura} e o conteúdo enviado (PAL-12) é validada no service,
 * não aqui - ver design.md.
 */
public record CriarListaPalavrasRequest(
        @NotBlank @Size(min = 3, max = 100) String nome,
        @NotNull @Min(1) @Max(5) Integer serie,
        @NotNull TipoLeituraCodigo tipoLeitura,
        TipoPalavra tipoPalavra,
        @Size(min = 1, max = 2000) String texto,
        @Valid @Size(min = 1, max = 200) List<@NotNull ItemPalavraRequest> itens) {
}

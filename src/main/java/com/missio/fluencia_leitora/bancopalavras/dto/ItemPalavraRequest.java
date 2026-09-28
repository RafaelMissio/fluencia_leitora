package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PAL-01/PAL-03: um item do payload de criação/atualização de lista
 * (`PALAVRA`/`PSEUDOPALAVRA`). Espaços no início/fim de {@code palavra} são
 * removidos no construtor compacto - antes da validação Bean Validation
 * rodar, conforme spec.md, Edge Cases ("remove-los antes de validar").
 * {@code tipoPalavra} é obrigatório: a coluna {@code tipo_palavra} de
 * {@code item_lista_palavras} é `NOT NULL` (V6), e sem essa anotação um item
 * sem tipo derrubava a criação com 500 em vez de 422.
 */
public record ItemPalavraRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "^[\\p{L}-]+$") String palavra,
        @NotNull TipoPalavra tipoPalavra) {

    public ItemPalavraRequest {
        palavra = palavra == null ? null : palavra.trim();
    }
}

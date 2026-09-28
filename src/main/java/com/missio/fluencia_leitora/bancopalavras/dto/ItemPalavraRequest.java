package com.missio.fluencia_leitora.bancopalavras.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PAL-01/PAL-03: um item do payload de criação/atualização de lista
 * (`PALAVRA`/`PSEUDOPALAVRA`). Espaços no início/fim de {@code palavra} são
 * removidos no construtor compacto - antes da validação Bean Validation
 * rodar, conforme spec.md, Edge Cases ("remove-los antes de validar").
 */
public record ItemPalavraRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "^[\\p{L}-]+$") String palavra,
        TipoPalavra tipoPalavra) {

    public ItemPalavraRequest {
        palavra = palavra == null ? null : palavra.trim();
    }
}

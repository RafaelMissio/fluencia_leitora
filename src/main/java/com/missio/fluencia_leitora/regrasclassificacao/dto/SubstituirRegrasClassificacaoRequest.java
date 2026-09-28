package com.missio.fluencia_leitora.regrasclassificacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload de {@code PUT /api/v1/regras-classificacao/series/{serie}}: o
 * conjunto inteiro de faixas que substitui as anteriores (design.md, DTOs).
 * {@code @NotNull} no elemento da lista aplica a lição L-019/L-023 (um
 * elemento nulo passa por {@code @Valid} sem erro e quebra mais tarde com
 * 500).
 *
 * <p><b>Importante</b>: {@code faixas} NÃO leva {@code @NotEmpty}/{@code
 * @Size(min = 1)} de propósito - o spec (Edge Cases) pede que uma lista
 * vazia devolva o código de negócio {@code FAIXA_NAO_INICIA_EM_ZERO}, não o
 * {@code VALIDACAO_INVALIDA} genérico do Bean Validation. A checagem de
 * vazio fica no service ({@code RegraClassificacaoService.substituir}).
 */
public record SubstituirRegrasClassificacaoRequest(@NotNull @Valid List<@NotNull FaixaRequest> faixas) {
}

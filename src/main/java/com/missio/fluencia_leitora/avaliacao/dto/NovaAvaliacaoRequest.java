package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * AVA-01..AVA-08: payload de criação de uma avaliação. A exclusividade entre
 * {@code listaPalavrasId}, {@code palavras} e {@code texto} (AVA-04), a
 * compatibilidade da lista e a validação de cada palavra (AVA-08, com a
 * posição) ficam no service, não aqui. {@code tempoSegundos} ausente vira 60
 * (spec.md, Assumptions: "padrão 60").
 */
public record NovaAvaliacaoRequest(
        @NotNull Long alunoId,
        @NotNull TipoLeituraCodigo tipoLeitura,
        @NotNull Long cicloId,
        @NotNull LocalDate dataAvaliacao,
        @Min(10) @Max(600) Integer tempoSegundos,
        Long listaPalavrasId,
        @Valid List<@NotNull PalavraDigitadaRequest> palavras,
        String texto) {

    private static final int TEMPO_PADRAO_SEGUNDOS = 60;

    public NovaAvaliacaoRequest {
        tempoSegundos = tempoSegundos == null ? TEMPO_PADRAO_SEGUNDOS : tempoSegundos;
    }
}

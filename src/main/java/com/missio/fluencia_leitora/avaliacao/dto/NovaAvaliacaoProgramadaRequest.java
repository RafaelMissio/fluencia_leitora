package com.missio.fluencia_leitora.avaliacao.dto;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuração de uma avaliação para uma série ({@code anoLetivoId} ausente = ano letivo ativo). Exatamente
 * uma fonte entre {@code listaPalavrasId}, {@code palavras} (separadas por
 * espaço) e {@code texto} (só em TEXTO_CURTO) - validada no service.
 * {@code tempoSegundos} ausente vira 60; {@code maxRefazeres} ausente vira 3.
 */
public record NovaAvaliacaoProgramadaRequest(
        Long anoLetivoId,
        @NotBlank @Size(max = 150) String nome,
        @NotNull @Min(1) @Max(5) Integer serie,
        @NotNull TipoLeituraCodigo tipoLeitura,
        @NotNull Long cicloId,
        @Min(10) @Max(600) Integer tempoSegundos,
        Long listaPalavrasId,
        String palavras,
        String texto,
        @Min(0) @Max(20) Integer maxRefazeres) {

    public NovaAvaliacaoProgramadaRequest {
        tempoSegundos = tempoSegundos == null ? 60 : tempoSegundos;
        maxRefazeres = maxRefazeres == null ? 3 : maxRefazeres;
    }
}

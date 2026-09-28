package com.missio.fluencia_leitora.avaliacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AVA-24: justificativa obrigatória, de 10 a 500 caracteres. */
public record CancelarAvaliacaoRequest(@NotBlank @Size(min = 10, max = 500) String justificativa) {
}

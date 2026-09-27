package com.missio.fluencia_leitora.cadastros.professor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CAD-07: payload de criação do professor. */
public record CriarProfessorRequest(@NotBlank @Size(min = 3, max = 150) String nome) {
}

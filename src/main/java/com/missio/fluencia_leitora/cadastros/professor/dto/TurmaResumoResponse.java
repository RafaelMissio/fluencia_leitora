package com.missio.fluencia_leitora.cadastros.professor.dto;

import com.missio.fluencia_leitora.cadastros.turma.Turma;

/** CAD-08: resumo de uma turma ativa associada a um professor. */
public record TurmaResumoResponse(Long id, String nome, int serie) {

    public static TurmaResumoResponse from(Turma turma) {
        return new TurmaResumoResponse(turma.getId(), turma.getNome(), turma.getSerie());
    }
}

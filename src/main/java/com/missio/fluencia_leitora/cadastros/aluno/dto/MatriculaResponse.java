package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.StatusMatricula;

public record MatriculaResponse(
        Long id, Long alunoId, Long anoLetivoId, Long turmaId, int serie, Long professorId, boolean anoFinalizado,
        StatusMatricula status) {

    public static MatriculaResponse from(Matricula matricula) {
        return new MatriculaResponse(
                matricula.getId(),
                matricula.getAluno().getId(),
                matricula.getAnoLetivo().getId(),
                matricula.getTurma().getId(),
                matricula.getSerie(),
                matricula.getProfessor() == null ? null : matricula.getProfessor().getId(),
                matricula.isAnoFinalizado(),
                matricula.getStatus());
    }
}

package com.missio.fluencia_leitora.cadastros.professor.dto;

import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorService.ProfessorComTurmas;

import java.util.List;

public record ProfessorResponse(Long id, String nome, boolean ativo, List<TurmaResumoResponse> turmas) {

    public static ProfessorResponse from(Professor professor) {
        return new ProfessorResponse(professor.getId(), professor.getNome(), professor.isAtivo(), List.of());
    }

    public static ProfessorResponse from(ProfessorComTurmas professorComTurmas) {
        return new ProfessorResponse(
                professorComTurmas.professor().getId(),
                professorComTurmas.professor().getNome(),
                professorComTurmas.professor().isAtivo(),
                professorComTurmas.turmasAtivas().stream().map(TurmaResumoResponse::from).toList());
    }
}

package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.StatusMatricula;

public record AlunoDaTurmaResponse(Long alunoId, String nome, boolean alunoAtivo, Long matriculaId, StatusMatricula status) {

    public static AlunoDaTurmaResponse from(Matricula matricula) {
        return new AlunoDaTurmaResponse(
                matricula.getAluno().getId(),
                matricula.getAluno().getNome(),
                matricula.getAluno().isAtivo(),
                matricula.getId(),
                matricula.getStatus());
    }
}

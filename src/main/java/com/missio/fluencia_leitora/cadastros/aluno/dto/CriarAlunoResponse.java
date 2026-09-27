package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoComMatricula;

/** CAD-11: resposta da criação do aluno, com o par de identificadores. */
public record CriarAlunoResponse(Long alunoId, Long matriculaId) {

    public static CriarAlunoResponse from(AlunoComMatricula alunoComMatricula) {
        return new CriarAlunoResponse(
                alunoComMatricula.aluno().getId(), alunoComMatricula.matricula().getId());
    }
}

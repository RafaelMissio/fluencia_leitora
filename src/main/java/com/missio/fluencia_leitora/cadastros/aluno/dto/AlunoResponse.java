package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.Aluno;

public record AlunoResponse(Long id, String nome, boolean ativo) {

    public static AlunoResponse from(Aluno aluno) {
        return new AlunoResponse(aluno.getId(), aluno.getNome(), aluno.isAtivo());
    }
}

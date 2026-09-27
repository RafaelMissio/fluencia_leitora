package com.missio.fluencia_leitora.cadastros.turma.dto;

import com.missio.fluencia_leitora.cadastros.turma.Turma;

public record TurmaResponse(
        Long id, String nome, int serie, Long anoLetivoId, Long professorId, boolean ativo) {

    public static TurmaResponse from(Turma turma) {
        return new TurmaResponse(
                turma.getId(),
                turma.getNome(),
                turma.getSerie(),
                turma.getAnoLetivo().getId(),
                turma.getProfessor() == null ? null : turma.getProfessor().getId(),
                turma.isAtivo());
    }
}

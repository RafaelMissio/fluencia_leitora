package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.StatusMatricula;

/**
 * CAD-14/CAD-17: atualização parcial da matrícula - troca de professor e/ou
 * turma, e marcação de ano finalizado. Campos {@code null} deixam o
 * respectivo dado inalterado.
 */
public record AtualizarMatriculaRequest(
        Long professorId, Long turmaId, Boolean anoFinalizado, StatusMatricula status) {
}

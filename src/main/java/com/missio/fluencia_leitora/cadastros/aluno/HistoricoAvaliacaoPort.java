package com.missio.fluencia_leitora.cadastros.aluno;

/**
 * Pergunta "esse aluno tem avaliação com status diferente de CANCELADA?"
 * sem {@code cadastros-base} depender da tabela {@code avaliacao}, que
 * pertence à feature {@code avaliacao} (ainda não implementada).
 */
public interface HistoricoAvaliacaoPort {

    boolean existeAvaliacaoNaoCancelada(Long alunoId);
}

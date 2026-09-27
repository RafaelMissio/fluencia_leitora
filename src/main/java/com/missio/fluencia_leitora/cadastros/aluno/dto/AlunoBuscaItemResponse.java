package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;

/**
 * CAD-16: item de resultado da busca de aluno por nome. Inclui
 * {@code alunoId}, {@code nome}, {@code turma}, {@code serie},
 * {@code professor}, {@code anoLetivo} e a situação da matrícula ativa,
 * conforme a spec (P1: Buscar aluno, AC4).
 *
 * <p>SPEC-PRECISION-GAP: a spec não define os valores exatos do campo
 * {@code situacao} nem o formato de {@code anoLetivo} (o modelo de dados não
 * tem um enum de "situação da matrícula" - só o booleano
 * {@code anoFinalizado}). Aqui {@code situacao} deriva de
 * {@code anoFinalizado} ({@code "EM_ANDAMENTO"}/{@code "FINALIZADO"}) e
 * {@code anoLetivo} é o ano (int). Quando o aluno não tem matrícula no ano
 * letivo ATIVO, os campos de matrícula vêm {@code null}/{@code 0}.
 */
public record AlunoBuscaItemResponse(
        Long alunoId, String nome, String turma, int serie, String professor, Integer anoLetivo, String situacao) {

    public static AlunoBuscaItemResponse from(AlunoBusca alunoBusca) {
        Matricula matricula = alunoBusca.matriculaAtiva();
        if (matricula == null) {
            return new AlunoBuscaItemResponse(
                    alunoBusca.aluno().getId(), alunoBusca.aluno().getNome(), null, 0, null, null, null);
        }
        return new AlunoBuscaItemResponse(
                alunoBusca.aluno().getId(),
                alunoBusca.aluno().getNome(),
                matricula.getTurma().getNome(),
                matricula.getSerie(),
                matricula.getProfessor() == null ? null : matricula.getProfessor().getNome(),
                matricula.getAnoLetivo().getAno(),
                matricula.isAnoFinalizado() ? "FINALIZADO" : "EM_ANDAMENTO");
    }
}

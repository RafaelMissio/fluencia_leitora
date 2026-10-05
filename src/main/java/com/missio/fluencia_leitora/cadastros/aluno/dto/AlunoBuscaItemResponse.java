package com.missio.fluencia_leitora.cadastros.aluno.dto;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.StatusMatricula;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import java.util.List;

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
        Long alunoId,
        String nome,
        String turma,
        int serie,
        String professor,
        Integer anoLetivo,
        String situacao,
        boolean ativo,
        StatusMatricula status,
        List<MatriculaAnoResponse> matriculas) {

    /** Matrícula de um ano letivo (ativo ou não) com o status do aluno naquele ano. */
    public record MatriculaAnoResponse(
            Long matriculaId,
            Long anoLetivoId,
            int anoLetivo,
            SituacaoAnoLetivo situacaoAnoLetivo,
            Long turmaId,
            String turma,
            int serie,
            Long professorId,
            StatusMatricula status) {

        /** Ano ativo => sempre CURSANDO; ano inativo => resultado gravado (CURSANDO = ainda não definido). */
        static StatusMatricula statusEfetivo(Matricula m) {
            return m.getAnoLetivo().getSituacao() == SituacaoAnoLetivo.ATIVO ? StatusMatricula.CURSANDO : m.getStatus();
        }

        static MatriculaAnoResponse from(Matricula m) {
            return new MatriculaAnoResponse(
                    m.getId(),
                    m.getAnoLetivo().getId(),
                    m.getAnoLetivo().getAno(),
                    m.getAnoLetivo().getSituacao(),
                    m.getTurma().getId(),
                    m.getTurma().getNome(),
                    m.getSerie(),
                    m.getProfessor() == null ? null : m.getProfessor().getId(),
                    statusEfetivo(m));
        }
    }

    public static AlunoBuscaItemResponse from(AlunoBusca alunoBusca) {
        List<MatriculaAnoResponse> matriculas =
                alunoBusca.matriculas().stream().map(MatriculaAnoResponse::from).toList();
        Matricula matricula = alunoBusca.matriculaAtiva();
        if (matricula == null) {
            return new AlunoBuscaItemResponse(
                    alunoBusca.aluno().getId(), alunoBusca.aluno().getNome(), null, 0, null, null, null, alunoBusca.aluno().isAtivo(), null, matriculas);
        }
        return new AlunoBuscaItemResponse(
                alunoBusca.aluno().getId(),
                alunoBusca.aluno().getNome(),
                matricula.getTurma().getNome(),
                matricula.getSerie(),
                matricula.getProfessor() == null ? null : matricula.getProfessor().getNome(),
                matricula.getAnoLetivo().getAno(),
                matricula.isAnoFinalizado() ? "FINALIZADO" : "EM_ANDAMENTO",
                alunoBusca.aluno().isAtivo(),
                MatriculaAnoResponse.statusEfetivo(matricula),
                matriculas);
    }
}

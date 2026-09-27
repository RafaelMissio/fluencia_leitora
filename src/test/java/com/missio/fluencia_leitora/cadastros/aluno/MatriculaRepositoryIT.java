package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-12/CAD-13: existsByAlunoIdAndAnoLetivoId detecta a duplicidade de
 * matrícula do mesmo aluno no mesmo ano letivo, e a constraint única
 * `uk_matricula_aluno_ano` (migração V4) impede a duplicidade mesmo se o
 * código de aplicação não checar antes, exercitadas contra MySQL real.
 */
@Transactional
class MatriculaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    private AnoLetivo novoAnoLetivo(int ano) {
        return anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
    }

    @Test
    void existsByAlunoIdAndAnoLetivoIdDetectaDuplicidade() {
        AnoLetivo anoLetivo = novoAnoLetivo(2480);
        Turma turma = turmaRepository.save(new Turma("Turma A", 1, anoLetivo, null));
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Teste"));

        assertFalse(matriculaRepository.existsByAlunoIdAndAnoLetivoId(aluno.getId(), anoLetivo.getId()));

        matriculaRepository.save(new Matricula(aluno, anoLetivo, turma, 1, null));

        assertTrue(matriculaRepository.existsByAlunoIdAndAnoLetivoId(aluno.getId(), anoLetivo.getId()));
    }

    @Test
    void duasMatriculasDoMesmoAlunoNoMesmoAnoViolamAConstraintUnica() {
        AnoLetivo anoLetivo = novoAnoLetivo(2481);
        Turma turma = turmaRepository.save(new Turma("Turma B", 1, anoLetivo, null));
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Duplicado"));
        matriculaRepository.saveAndFlush(new Matricula(aluno, anoLetivo, turma, 1, null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> matriculaRepository.saveAndFlush(new Matricula(aluno, anoLetivo, turma, 1, null)));
    }
}

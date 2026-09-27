package com.missio.fluencia_leitora.cadastros.turma;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-07/CAD-08: existsByNomeIgnoreCaseAndAnoLetivoId detects a duplicate
 * turma name regardless of letter case within the same ano letivo;
 * findByProfessorIdAndAtivoTrue returns only the active turmas of a given
 * professor, exercised against real MySQL.
 */
@Transactional
class TurmaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    private AnoLetivo novoAnoLetivo(int ano) {
        return anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
    }

    @Test
    void existsByNomeIgnoreCaseAndAnoLetivoIdIgnoraMaiusculasEMinusculas() {
        AnoLetivo anoLetivo = novoAnoLetivo(2460);
        turmaRepository.save(new Turma("Turma A", 1, anoLetivo, null));

        assertTrue(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("turma a", anoLetivo.getId()));
        assertTrue(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("TURMA A", anoLetivo.getId()));
        assertFalse(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("Turma B", anoLetivo.getId()));
    }

    @Test
    void findByProfessorIdAndAtivoTrueRetornaListaVaziaQuandoNaoHaTurmaAtiva() {
        AnoLetivo anoLetivo = novoAnoLetivo(2461);
        Professor professor = professorRepository.save(new Professor("Sem Turmas"));

        List<Turma> turmasAtivas = turmaRepository.findByProfessorIdAndAtivoTrue(professor.getId());

        assertTrue(turmasAtivas.isEmpty());
    }

    @Test
    void findByProfessorIdAndAtivoTrueRetornaSomenteAsTurmasAtivasDoProfessor() {
        AnoLetivo anoLetivo = novoAnoLetivo(2462);
        Professor professor = professorRepository.save(new Professor("Com Turmas"));
        Turma ativa = turmaRepository.save(new Turma("Turma Ativa", 1, anoLetivo, professor));
        Turma inativa = turmaRepository.save(new Turma("Turma Inativa", 2, anoLetivo, professor));
        inativa.setAtivo(false);
        turmaRepository.save(inativa);

        List<Turma> turmasAtivas = turmaRepository.findByProfessorIdAndAtivoTrue(professor.getId());

        assertEquals(1, turmasAtivas.size());
        assertEquals(ativa.getId(), turmasAtivas.get(0).getId());
    }
}

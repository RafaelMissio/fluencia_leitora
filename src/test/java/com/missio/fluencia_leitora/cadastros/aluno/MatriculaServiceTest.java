package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-12/CAD-13: MatriculaService.matricular cria uma nova matrícula do mesmo
 * aluno em outro ano letivo, rejeita duplicidade no mesmo ano letivo (409) e
 * turma inválida/inativa ou ano letivo ENCERRADO (422).
 */
@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private AlunoRepository alunoRepository;

    private MatriculaService service() {
        return new MatriculaService(matriculaRepository, turmaRepository, alunoRepository);
    }

    private AnoLetivo anoLetivo(int ano, SituacaoAnoLetivo situacao) {
        AnoLetivo anoLetivo = new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15));
        anoLetivo.setSituacao(situacao);
        return anoLetivo;
    }

    @Test
    void matricularEmOutroAnoLetivoCriaComSucessoLigadaAoMesmoAluno() {
        Aluno aluno = new Aluno("Aluno Existente");
        AnoLetivo anoLetivo2027 = anoLetivo(2027, SituacaoAnoLetivo.PLANEJADO);
        Professor professor = new Professor("Professor Novo");
        Turma turma = new Turma("Turma 2027", 2, anoLetivo2027, professor);
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(turmaRepository.findById(2L)).thenReturn(Optional.of(turma));
        when(matriculaRepository.existsByAlunoIdAndAnoLetivoId(1L, null)).thenReturn(false);
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Matricula nova = service().matricular(1L, 2L);

        assertEquals(aluno, nova.getAluno());
        assertEquals(anoLetivo2027, nova.getAnoLetivo());
        assertEquals(turma, nova.getTurma());
        assertEquals(2, nova.getSerie());
        assertEquals(professor, nova.getProfessor());
    }

    @Test
    void matricularDuplicadaNoMesmoAnoLetivoRetorna409SemGravar() {
        Aluno aluno = new Aluno("Aluno Existente");
        AnoLetivo anoLetivo = anoLetivo(2027, SituacaoAnoLetivo.PLANEJADO);
        Turma turma = new Turma("Turma 2027", 2, anoLetivo, null);
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(turmaRepository.findById(2L)).thenReturn(Optional.of(turma));
        when(matriculaRepository.existsByAlunoIdAndAnoLetivoId(1L, null)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service().matricular(1L, 2L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("MATRICULA_DUPLICADA", exception.getCode());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void matricularComTurmaInativaRetorna422SemGravar() {
        Aluno aluno = new Aluno("Aluno Existente");
        AnoLetivo anoLetivo = anoLetivo(2027, SituacaoAnoLetivo.PLANEJADO);
        Turma turmaInativa = new Turma("Turma Inativa", 2, anoLetivo, null);
        turmaInativa.setAtivo(false);
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(turmaRepository.findById(2L)).thenReturn(Optional.of(turmaInativa));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().matricular(1L, 2L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("TURMA_INVALIDA", exception.getCode());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void matricularComAnoLetivoEncerradoRetorna422SemGravar() {
        Aluno aluno = new Aluno("Aluno Existente");
        AnoLetivo anoEncerrado = anoLetivo(2025, SituacaoAnoLetivo.ENCERRADO);
        Turma turma = new Turma("Turma Encerrada", 2, anoEncerrado, null);
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(turmaRepository.findById(2L)).thenReturn(Optional.of(turma));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().matricular(1L, 2L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("ANO_LETIVO_ENCERRADO", exception.getCode());
        verify(matriculaRepository, never()).save(any());
    }
}

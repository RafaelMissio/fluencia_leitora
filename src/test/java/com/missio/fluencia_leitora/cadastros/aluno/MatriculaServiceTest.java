package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
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

    @Mock
    private ProfessorRepository professorRepository;

    private MatriculaService service() {
        return new MatriculaService(matriculaRepository, turmaRepository, alunoRepository, professorRepository);
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

    @Test
    void atualizarTrocaProfessorGravaONovoProfessorId() {
        AnoLetivo anoLetivo = anoLetivo(2026, SituacaoAnoLetivo.ATIVO);
        Turma turma = new Turma("Turma A", 3, anoLetivo, null);
        Matricula matricula = new Matricula(new Aluno("Aluno"), anoLetivo, turma, 3, null);
        Professor novoProfessor = new Professor("Novo Professor");
        when(matriculaRepository.findById(10L)).thenReturn(Optional.of(matricula));
        when(professorRepository.findById(5L)).thenReturn(Optional.of(novoProfessor));
        when(matriculaRepository.save(matricula)).thenReturn(matricula);

        Matricula atualizada = service().atualizar(10L, 5L, null, null, null);

        assertEquals(novoProfessor, atualizada.getProfessor());
    }

    @Test
    void atualizarTransfereTurmaNoMesmoAnoLetivoAtualizaTurmaESerie() {
        AnoLetivo anoLetivo = anoLetivo(2026, SituacaoAnoLetivo.ATIVO);
        Turma turmaOriginal = new Turma("Turma A", 3, anoLetivo, null);
        Turma novaTurma = new Turma("Turma B", 4, anoLetivo, null);
        Matricula matricula = new Matricula(new Aluno("Aluno"), anoLetivo, turmaOriginal, 3, null);
        when(matriculaRepository.findById(10L)).thenReturn(Optional.of(matricula));
        when(turmaRepository.findById(20L)).thenReturn(Optional.of(novaTurma));
        when(matriculaRepository.save(matricula)).thenReturn(matricula);

        Matricula atualizada = service().atualizar(10L, null, 20L, null, null);

        assertEquals(novaTurma, atualizada.getTurma());
        assertEquals(4, atualizada.getSerie());
    }

    @Test
    void atualizarMarcaAnoFinalizadoTrueEGrava() {
        AnoLetivo anoLetivo = anoLetivo(2026, SituacaoAnoLetivo.ATIVO);
        Turma turma = new Turma("Turma A", 3, anoLetivo, null);
        Matricula matricula = new Matricula(new Aluno("Aluno"), anoLetivo, turma, 3, null);
        when(matriculaRepository.findById(10L)).thenReturn(Optional.of(matricula));
        when(matriculaRepository.save(matricula)).thenReturn(matricula);

        Matricula atualizada = service().atualizar(10L, null, null, true, null);

        assertEquals(true, atualizada.isAnoFinalizado());
    }

    @Test
    void atualizarStatusReprovadoFinalizaOAno() {
        AnoLetivo anoLetivo = anoLetivo(2025, SituacaoAnoLetivo.ENCERRADO);
        Turma turma = new Turma("Turma A", 3, anoLetivo, null);
        Matricula matricula = new Matricula(new Aluno("Aluno"), anoLetivo, turma, 3, null);
        when(matriculaRepository.findById(10L)).thenReturn(Optional.of(matricula));
        when(matriculaRepository.save(matricula)).thenReturn(matricula);

        Matricula atualizada = service().atualizar(10L, null, null, null, StatusMatricula.REPROVADO);

        assertEquals(StatusMatricula.REPROVADO, atualizada.getStatus());
        assertEquals(true, atualizada.isAnoFinalizado());
    }

    @Test
    void atualizarStatusAprovadoEmAnoAtivoERejeitado() {
        AnoLetivo anoLetivo = anoLetivo(2026, SituacaoAnoLetivo.ATIVO);
        Turma turma = new Turma("Turma A", 3, anoLetivo, null);
        Matricula matricula = new Matricula(new Aluno("Aluno"), anoLetivo, turma, 3, null);
        when(matriculaRepository.findById(10L)).thenReturn(Optional.of(matricula));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.missio.fluencia_leitora.common.error.BusinessException.class,
                () -> service().atualizar(10L, null, null, null, StatusMatricula.APROVADO));
    }
}

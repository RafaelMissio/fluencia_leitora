package com.missio.fluencia_leitora.cadastros.professor;

import com.missio.fluencia_leitora.cadastros.professor.ProfessorService.ProfessorComTurmas;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-07/CAD-08/CAD-09: ProfessorService.criar valida o tamanho do nome e
 * grava; buscarComTurmas traz o professor com as turmas ativas (0..n);
 * inativar bloqueia com 409 quando há turma ativa vinculada e, caso
 * contrário, seta ativo=false.
 */
@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private TurmaRepository turmaRepository;

    private ProfessorService service() {
        return new ProfessorService(professorRepository, turmaRepository);
    }

    @Test
    void criarComNomeValidoGrava() {
        when(professorRepository.save(any(Professor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Professor criado = service().criar("Maria Silva");

        assertEquals("Maria Silva", criado.getNome());
    }

    @Test
    void criarComNomeForaDaFaixaRetorna422SemGravar() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar("Ma"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("NOME_INVALIDO", exception.getCode());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void buscarComTurmasTrazAsTurmasAtivasDoProfessor() {
        Professor professor = new Professor("Maria Silva");
        Turma turmaAtiva = new Turma("Turma A", 1, null, professor);
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(turmaRepository.findByProfessorIdAndAtivoTrue(1L)).thenReturn(List.of(turmaAtiva));

        ProfessorComTurmas resultado = service().buscarComTurmas(1L);

        assertEquals(professor, resultado.professor());
        assertEquals(1, resultado.turmasAtivas().size());
        assertEquals(turmaAtiva, resultado.turmasAtivas().get(0));
    }

    @Test
    void buscarComTurmasSemTurmaAtivaRetornaListaVazia() {
        Professor professor = new Professor("Maria Silva");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(turmaRepository.findByProfessorIdAndAtivoTrue(1L)).thenReturn(List.of());

        ProfessorComTurmas resultado = service().buscarComTurmas(1L);

        assertTrue(resultado.turmasAtivas().isEmpty());
    }

    @Test
    void inativarComTurmaAtivaRetorna409SemAlterarProfessor() {
        Professor professor = new Professor("Maria Silva");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(turmaRepository.findByProfessorIdAndAtivoTrue(1L))
                .thenReturn(List.of(new Turma("Turma A", 1, null, professor)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().inativar(1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("PROFESSOR_COM_TURMA_ATIVA", exception.getCode());
        assertTrue(professor.isAtivo());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void inativarSemTurmaAtivaGravaAtivoFalse() {
        Professor professor = new Professor("Maria Silva");
        when(professorRepository.findById(1L)).thenReturn(Optional.of(professor));
        when(turmaRepository.findByProfessorIdAndAtivoTrue(1L)).thenReturn(List.of());

        service().inativar(1L);

        assertFalse(professor.isAtivo());
        verify(professorRepository).save(professor);
    }
}

package com.missio.fluencia_leitora.cadastros.turma;

import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-07/CAD-10: TurmaService.criar grava com sucesso (professorId opcional),
 * rejeita duplicidade de nome no mesmo ano letivo (409) e referência de
 * anoLetivoId/professorId inválida ou inativa (422); atualizarProfessor troca
 * o professor responsável.
 */
@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private AnoLetivoRepository anoLetivoRepository;

    private TurmaService service() {
        return new TurmaService(turmaRepository, professorRepository, anoLetivoRepository);
    }

    private AnoLetivo anoLetivoAtivo() {
        return new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
    }

    @Test
    void criarComProfessorInformadoGravaComSucesso() {
        AnoLetivo anoLetivo = anoLetivoAtivo();
        Professor professor = new Professor("Maria Silva");
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(anoLetivo));
        when(professorRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("Turma A", 1L)).thenReturn(false);
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turma criada = service().criar("Turma A", 3, 1L, 2L);

        assertEquals("Turma A", criada.getNome());
        assertEquals(3, criada.getSerie());
        assertEquals(anoLetivo, criada.getAnoLetivo());
        assertEquals(professor, criada.getProfessor());
    }

    @Test
    void criarSemProfessorGravaComProfessorNulo() {
        AnoLetivo anoLetivo = anoLetivoAtivo();
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(anoLetivo));
        when(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("Turma B", 1L)).thenReturn(false);
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turma criada = service().criar("Turma B", 1, 1L, null);

        assertNull(criada.getProfessor());
        verify(professorRepository, never()).findById(any());
    }

    @Test
    void criarComNomeDuplicadoNoMesmoAnoRetorna409SemGravar() {
        AnoLetivo anoLetivo = anoLetivoAtivo();
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(anoLetivo));
        when(turmaRepository.existsByNomeIgnoreCaseAndAnoLetivoId("Turma A", 1L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().criar("Turma A", 3, 1L, null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("TURMA_DUPLICADA", exception.getCode());
        verify(turmaRepository, never()).save(any());
    }

    @Test
    void criarComAnoLetivoInexistenteOuInativoRetorna422SemGravar() {
        AnoLetivo inativo = anoLetivoAtivo();
        inativo.setAtivo(false);
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(inativo));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().criar("Turma A", 3, 1L, null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("REFERENCIA_INVALIDA", exception.getCode());
        verify(turmaRepository, never()).save(any());
    }

    @Test
    void criarComProfessorInexistenteOuInativoRetorna422SemGravar() {
        AnoLetivo anoLetivo = anoLetivoAtivo();
        Professor professorInativo = new Professor("Maria Silva");
        professorInativo.setAtivo(false);
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(anoLetivo));
        when(professorRepository.findById(2L)).thenReturn(Optional.of(professorInativo));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().criar("Turma A", 3, 1L, 2L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("REFERENCIA_INVALIDA", exception.getCode());
        verify(turmaRepository, never()).save(any());
    }

    @Test
    void atualizarProfessorGravaONovoProfessor() {
        AnoLetivo anoLetivo = anoLetivoAtivo();
        Turma turma = new Turma("Turma A", 3, anoLetivo, null);
        Professor novoProfessor = new Professor("João Souza");
        when(turmaRepository.findById(10L)).thenReturn(Optional.of(turma));
        when(professorRepository.findById(5L)).thenReturn(Optional.of(novoProfessor));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Turma atualizada = service().atualizarProfessor(10L, 5L);

        assertEquals(novoProfessor, atualizada.getProfessor());
    }
}

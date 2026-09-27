package com.missio.fluencia_leitora.cadastros.aluno;

import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoComMatricula;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-11: AlunoService.criarComMatricula cria aluno + matrícula copiando
 * série/professor da turma numa única transação, e rejeita turma inativa ou
 * ano letivo ENCERRADO (422) sem persistir nada.
 */
@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private TurmaRepository turmaRepository;

    @Mock
    private ContextoUsuarioPort contexto;

    private AlunoService service() {
        return new AlunoService(alunoRepository, matriculaRepository, turmaRepository);
    }

    private AnoLetivo anoLetivo(SituacaoAnoLetivo situacao) {
        AnoLetivo anoLetivo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        anoLetivo.setSituacao(situacao);
        return anoLetivo;
    }

    @Test
    void criarComMatriculaComSucessoCopiaSerieEProfessorDaTurma() {
        AnoLetivo anoLetivo = anoLetivo(SituacaoAnoLetivo.ATIVO);
        Professor professor = new Professor("Maria Silva");
        Turma turma = new Turma("Turma A", 4, anoLetivo, professor);
        when(turmaRepository.findById(1L)).thenReturn(Optional.of(turma));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlunoComMatricula resultado = service().criarComMatricula("João Silva", 1L);

        assertEquals("João Silva", resultado.aluno().getNome());
        assertEquals(4, resultado.matricula().getSerie());
        assertEquals(professor, resultado.matricula().getProfessor());
        assertEquals(anoLetivo, resultado.matricula().getAnoLetivo());
        assertEquals(turma, resultado.matricula().getTurma());
    }

    @Test
    void criarComMatriculaComTurmaInativaRetorna422SemPersistirNada() {
        AnoLetivo anoLetivo = anoLetivo(SituacaoAnoLetivo.ATIVO);
        Turma turmaInativa = new Turma("Turma B", 1, anoLetivo, null);
        turmaInativa.setAtivo(false);
        when(turmaRepository.findById(1L)).thenReturn(Optional.of(turmaInativa));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service().criarComMatricula("Aluno X", 1L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("TURMA_INVALIDA", exception.getCode());
        verify(alunoRepository, never()).save(any());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void criarComMatriculaComAnoLetivoEncerradoRetorna422SemPersistirNada() {
        AnoLetivo anoEncerrado = anoLetivo(SituacaoAnoLetivo.ENCERRADO);
        Turma turma = new Turma("Turma C", 1, anoEncerrado, null);
        when(turmaRepository.findById(1L)).thenReturn(Optional.of(turma));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service().criarComMatricula("Aluno Y", 1L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("ANO_LETIVO_ENCERRADO", exception.getCode());
        verify(alunoRepository, never()).save(any());
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void buscarComTermoCurtoRetorna422() {
        Pageable pageable = PageRequest.of(0, 20);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service().buscar("a", pageable, contexto));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("TERMO_INVALIDO", exception.getCode());
    }

    @Test
    void buscarComPerfilCoordenadorNaoFiltraPorProfessor() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contexto.perfilAtual()).thenReturn(Perfil.COORDENADOR);
        Aluno aluno = new Aluno("João Silva");
        Page<Aluno> pagina = new PageImpl<>(List.of(aluno));
        when(alunoRepository.buscarPorNome("joao", pageable)).thenReturn(pagina);

        Page<AlunoBusca> resultado = service().buscar("joao", pageable, contexto);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(aluno, resultado.getContent().get(0).aluno());
        verify(alunoRepository, never()).buscarPorNomeEProfessor(any(), any(), any());
    }

    @Test
    void buscarComPerfilProfessorFiltraPorProfessorIdDoContexto() {
        Pageable pageable = PageRequest.of(0, 20);
        when(contexto.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        when(contexto.professorIdAtual()).thenReturn(7L);
        Aluno alunoDoProfessor = new Aluno("Maria Souza");
        Page<Aluno> paginaProfessor = new PageImpl<>(List.of(alunoDoProfessor));
        when(alunoRepository.buscarPorNomeEProfessor("maria", 7L, pageable)).thenReturn(paginaProfessor);

        Page<AlunoBusca> resultado = service().buscar("maria", pageable, contexto);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(alunoDoProfessor, resultado.getContent().get(0).aluno());
        verify(alunoRepository, never()).buscarPorNome(any(), any());
    }
}

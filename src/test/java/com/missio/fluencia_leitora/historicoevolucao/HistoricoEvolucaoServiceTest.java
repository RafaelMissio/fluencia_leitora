package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudioRepository;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoRepository;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService.AlunoBusca;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T3 (historicoevolucao): {@link HistoricoEvolucaoService#historico} e
 * {@link HistoricoEvolucaoService#comAudio}. Cenário base: aluno com
 * matrícula ativa cujo professor é o mesmo do contexto (dono).
 */
@ExtendWith(MockitoExtension.class)
class HistoricoEvolucaoServiceTest {

    private static final Long ALUNO_ID = 1L;
    private static final Long PROFESSOR_ID = 7L;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private AvaliacaoAudioRepository avaliacaoAudioRepository;

    @Mock
    private AlunoService alunoService;

    @Mock
    private ContextoUsuarioPort contextoUsuario;

    private HistoricoEvolucaoService service;
    private Matricula matriculaAtiva;

    @BeforeEach
    void setUp() {
        lenient().when(contextoUsuario.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        lenient().when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID);

        Professor professor = new Professor("Professor Teste");
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        AnoLetivo anoLetivo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        Turma turma = new Turma("Turma 1A", 1, anoLetivo, professor);
        Aluno aluno = new Aluno("Aluno Teste");
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        matriculaAtiva = new Matricula(aluno, anoLetivo, turma, 1, professor);

        service = new HistoricoEvolucaoService(
                avaliacaoRepository,
                avaliacaoAudioRepository,
                alunoService,
                new PertencimentoProfessorGuard(contextoUsuario));
    }

    private void mockAlunoComMatricula(Matricula matricula) {
        when(alunoService.buscarPorId(ALUNO_ID)).thenReturn(new AlunoBusca(matricula.getAluno(), matricula));
    }

    @Test
    void historicoDevolveAPaginaTalComoORepositorioRetorna() {
        mockAlunoComMatricula(matriculaAtiva);
        Page<Avaliacao> paginaEsperada = new PageImpl<>(List.of(mock(Avaliacao.class)));
        when(avaliacaoRepository.buscarHistorico(
                        eq(ALUNO_ID), eq(StatusAvaliacao.FINALIZADA), any(), any(), any(), any()))
                .thenReturn(paginaEsperada);

        Page<Avaliacao> resultado = service.historico(ALUNO_ID, null, null, null, PageRequest.of(0, 20));

        assertSame(paginaEsperada, resultado);
    }

    @Test
    void historicoRepassaCadaFiltroEStatusFinalizadaAoRepositorio() {
        mockAlunoComMatricula(matriculaAtiva);
        Long anoLetivoId = 55L;
        TipoLeituraCodigo tipoLeitura = TipoLeituraCodigo.TEXTO_CURTO;
        Long cicloId = 3L;
        Pageable pageable = PageRequest.of(1, 20);
        when(avaliacaoRepository.buscarHistorico(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        service.historico(ALUNO_ID, anoLetivoId, tipoLeitura, cicloId, pageable);

        verify(avaliacaoRepository)
                .buscarHistorico(ALUNO_ID, StatusAvaliacao.FINALIZADA, anoLetivoId, tipoLeitura, cicloId, pageable);
    }

    @Test
    void historicoComAlunoInexistenteLanca404AlunoNaoEncontrado() {
        when(alunoService.buscarPorId(ALUNO_ID))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.historico(ALUNO_ID, null, null, null, PageRequest.of(0, 20)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("ALUNO_NAO_ENCONTRADO", ex.getCode());
    }

    @Test
    void historicoComProfessorQueNaoEDonoLanca404() {
        when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID + 1);
        mockAlunoComMatricula(matriculaAtiva);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.historico(ALUNO_ID, null, null, null, PageRequest.of(0, 20)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void historicoComProfessorDonoRetornaComSucesso() {
        mockAlunoComMatricula(matriculaAtiva);
        when(avaliacaoRepository.buscarHistorico(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<Avaliacao> resultado = service.historico(ALUNO_ID, null, null, null, PageRequest.of(0, 20));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void historicoComCoordenadorIgnoraOwnershipERetornaComSucesso() {
        when(contextoUsuario.perfilAtual()).thenReturn(Perfil.COORDENADOR);
        mockAlunoComMatricula(matriculaAtiva);
        when(avaliacaoRepository.buscarHistorico(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<Avaliacao> resultado = service.historico(ALUNO_ID, null, null, null, PageRequest.of(0, 20));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void comAudioDevolveOSetDoRepositorioComIds() {
        when(avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(anyList())).thenReturn(List.of(10L, 20L));

        Set<Long> resultado = service.comAudio(List.of(10L, 20L, 30L));

        assertEquals(Set.of(10L, 20L), resultado);
    }

    @Test
    void comAudioSemAudioDevolveSetVazio() {
        when(avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(anyList())).thenReturn(List.of());

        Set<Long> resultado = service.comAudio(List.of(10L));

        assertTrue(resultado.isEmpty());
    }
}

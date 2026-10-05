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
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoAnualLinha;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoCiclos;
import com.missio.fluencia_leitora.historicoevolucao.HistoricoEvolucaoService.EvolucaoValor;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * T3/T4 (historicoevolucao): {@link HistoricoEvolucaoService#historico},
 * {@link HistoricoEvolucaoService#comAudio} e {@link
 * HistoricoEvolucaoService#evolucaoPorCiclo}. Cenário base: aluno com
 * matrícula ativa cujo professor é o mesmo do contexto (dono).
 */
@ExtendWith(MockitoExtension.class)
class HistoricoEvolucaoServiceTest {

    private static final Long ALUNO_ID = 1L;
    private static final Long PROFESSOR_ID = 7L;
    private static final Long ANO_LETIVO_ID = 100L;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private AvaliacaoAudioRepository avaliacaoAudioRepository;

    @Mock
    private AlunoService alunoService;

    @Mock
    private AnoLetivoRepository anoLetivoRepository;

    @Mock
    private CicloRepository cicloRepository;

    @Mock
    private ContextoUsuarioPort contextoUsuario;

    @Mock
    private MatriculaRepository matriculaRepository;

    private HistoricoEvolucaoService service;
    private Matricula matriculaAtiva;
    private AnoLetivo anoLetivoAtivo;

    @BeforeEach
    void setUp() {
        lenient().when(contextoUsuario.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        lenient().when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID);
        lenient().when(cicloRepository.existsById(any())).thenReturn(true);

        Professor professor = new Professor("Professor Teste");
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        AnoLetivo anoLetivo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        Turma turma = new Turma("Turma 1A", 1, anoLetivo, professor);
        Aluno aluno = new Aluno("Aluno Teste");
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        matriculaAtiva = new Matricula(aluno, anoLetivo, turma, 1, professor);

        anoLetivoAtivo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        ReflectionTestUtils.setField(anoLetivoAtivo, "id", ANO_LETIVO_ID);
        lenient().when(alunoService.buscarPorId(ALUNO_ID)).thenReturn(new AlunoBusca(aluno, matriculaAtiva));

        service = new HistoricoEvolucaoService(
                avaliacaoRepository,
                avaliacaoAudioRepository,
                alunoService,
                anoLetivoRepository,
                cicloRepository,
                new PertencimentoProfessorGuard(contextoUsuario),
                matriculaRepository);
    }

    private static Ciclo cicloMock(long id, String codigo) {
        Ciclo ciclo = mock(Ciclo.class);
        lenient().when(ciclo.getId()).thenReturn(id);
        lenient().when(ciclo.getCodigo()).thenReturn(codigo);
        return ciclo;
    }

    private static Avaliacao avaliacaoMock(Ciclo ciclo, Instant finalizadoEm) {
        Avaliacao avaliacao = mock(Avaliacao.class);
        lenient().when(avaliacao.getCiclo()).thenReturn(ciclo);
        lenient().when(avaliacao.getFinalizadoEm()).thenReturn(finalizadoEm);
        return avaliacao;
    }

    private static Avaliacao avaliacaoAnualMock(
            Ciclo ciclo, AnoLetivo anoLetivo, int serie, int quantidadeCorretas, Instant finalizadoEm) {
        Avaliacao avaliacao = mock(Avaliacao.class);
        lenient().when(avaliacao.getCiclo()).thenReturn(ciclo);
        lenient().when(avaliacao.getAnoLetivo()).thenReturn(anoLetivo);
        lenient().when(avaliacao.getSerie()).thenReturn(serie);
        lenient().when(avaliacao.getQuantidadeCorretas()).thenReturn(quantidadeCorretas);
        lenient().when(avaliacao.getFinalizadoEm()).thenReturn(finalizadoEm);
        return avaliacao;
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

    /** HIST-22 (Verifier PASS 1, gap E4): cicloId numérico fora do domínio fixo de `ciclo` → 400, não página vazia. */
    @Test
    void historicoComCicloIdForaDoDominioLanca400() {
        mockAlunoComMatricula(matriculaAtiva);
        when(cicloRepository.existsById(999L)).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.historico(ALUNO_ID, null, null, 999L, PageRequest.of(0, 20)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CICLO_INVALIDO", ex.getCode());
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

    @Test
    void evolucaoPorCicloComOsTresCiclosPreenchidos() {
        when(anoLetivoRepository.findById(ANO_LETIVO_ID)).thenReturn(Optional.of(anoLetivoAtivo));
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Ciclo cicloAcompanhamento = cicloMock(2L, "ACOMPANHAMENTO");
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao avEntrada = avaliacaoMock(cicloEntrada, Instant.now());
        Avaliacao avAcompanhamento = avaliacaoMock(cicloAcompanhamento, Instant.now());
        Avaliacao avSaida = avaliacaoMock(cicloSaida, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(
                        ALUNO_ID, StatusAvaliacao.FINALIZADA, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA))
                .thenReturn(List.of(avEntrada, avAcompanhamento, avSaida));

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA);

        assertSame(avEntrada, resultado.entrada());
        assertSame(avAcompanhamento, resultado.acompanhamento());
        assertSame(avSaida, resultado.saida());
        assertEquals(2026, resultado.anoLetivo());
        assertEquals(ALUNO_ID, resultado.alunoId());
    }

    @Test
    void evolucaoPorCicloComCicloAusenteVemNuloSemErro() {
        when(anoLetivoRepository.findById(ANO_LETIVO_ID)).thenReturn(Optional.of(anoLetivoAtivo));
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Avaliacao avEntrada = avaliacaoMock(cicloEntrada, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(any(), any(), any(), any()))
                .thenReturn(List.of(avEntrada));

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA);

        assertSame(avEntrada, resultado.entrada());
        assertNull(resultado.acompanhamento());
        assertNull(resultado.saida());
    }

    @Test
    void evolucaoPorCicloComDuasFinalizadaNoMesmoCicloUsaAMaisRecente() {
        when(anoLetivoRepository.findById(ANO_LETIVO_ID)).thenReturn(Optional.of(anoLetivoAtivo));
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Avaliacao maisRecente = avaliacaoMock(cicloEntrada, Instant.now());
        Avaliacao maisAntiga = avaliacaoMock(cicloEntrada, Instant.now().minusSeconds(3600));
        // já ordenada por finalizadoEm desc dentro do grupo (buscarFinalizadasPorAnoETipo, HIST-20)
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(any(), any(), any(), any()))
                .thenReturn(List.of(maisRecente, maisAntiga));

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA);

        assertSame(maisRecente, resultado.entrada());
    }

    @Test
    void evolucaoPorCicloUsaATentativaComMaisCorretasMesmoSendoMaisAntiga() {
        when(anoLetivoRepository.findById(ANO_LETIVO_ID)).thenReturn(Optional.of(anoLetivoAtivo));
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Avaliacao maisRecente = avaliacaoMock(cicloEntrada, Instant.now());
        Avaliacao melhor = avaliacaoMock(cicloEntrada, Instant.now().minusSeconds(3600));
        when(maisRecente.getQuantidadeCorretas()).thenReturn(7);
        when(melhor.getQuantidadeCorretas()).thenReturn(10);
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(any(), any(), any(), any()))
                .thenReturn(List.of(maisRecente, melhor));

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA);

        assertSame(melhor, resultado.entrada());
    }

    @Test
    void evolucaoPorCicloComAnoLetivoOmitidoUsaOAnoAtivo() {
        when(anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO)).thenReturn(List.of(anoLetivoAtivo));
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(any(), any(), any(), any())).thenReturn(List.of());

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, null, TipoLeituraCodigo.PALAVRA);

        assertEquals(2026, resultado.anoLetivo());
        verify(avaliacaoRepository)
                .buscarFinalizadasPorAnoETipo(
                        ALUNO_ID, StatusAvaliacao.FINALIZADA, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA);
    }

    @Test
    void evolucaoPorCicloComAnoLetivoInformadoEExistenteUsaEsseAno() {
        Long outroAnoId = 200L;
        AnoLetivo outroAno = new AnoLetivo(2027, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 12, 15));
        ReflectionTestUtils.setField(outroAno, "id", outroAnoId);
        when(anoLetivoRepository.findById(outroAnoId)).thenReturn(Optional.of(outroAno));
        when(avaliacaoRepository.buscarFinalizadasPorAnoETipo(any(), any(), any(), any())).thenReturn(List.of());

        EvolucaoCiclos resultado = service.evolucaoPorCiclo(ALUNO_ID, outroAnoId, TipoLeituraCodigo.PALAVRA);

        assertEquals(2027, resultado.anoLetivo());
    }

    @Test
    void evolucaoPorCicloComAnoLetivoInformadoInexistenteLanca404() {
        when(anoLetivoRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.evolucaoPorCiclo(ALUNO_ID, 999L, TipoLeituraCodigo.PALAVRA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("ANO_LETIVO_NAO_ENCONTRADO", ex.getCode());
    }

    @Test
    void evolucaoPorCicloComAlunoInexistenteLanca404() {
        when(alunoService.buscarPorId(ALUNO_ID))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.evolucaoPorCiclo(ALUNO_ID, ANO_LETIVO_ID, TipoLeituraCodigo.PALAVRA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("ALUNO_NAO_ENCONTRADO", ex.getCode());
    }

    private static AnoLetivo anoLetivoComId(int ano, long id) {
        AnoLetivo anoLetivo = new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15));
        ReflectionTestUtils.setField(anoLetivo, "id", id);
        return anoLetivo;
    }

    @Test
    void evolucaoAnualUmaLinhaPorAnoComSerieDaAvaliacao() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloEntrada, ano2026, 2, 10, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloEntrada, ano2027, 3, 15, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(ALUNO_ID, StatusAvaliacao.FINALIZADA, TipoLeituraCodigo.PALAVRA))
                .thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        assertEquals(2, linhas.size());
        assertEquals(2026, linhas.get(0).anoLetivo());
        assertEquals(2, linhas.get(0).serie());
        assertEquals(2027, linhas.get(1).anoLetivo());
        assertEquals(3, linhas.get(1).serie());
    }

    @Test
    void evolucaoAnualTrazATurmaDeCadaAnoQuandoOAlunoMudaDeTurma() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloEntrada, ano2026, 1, 10, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloEntrada, ano2027, 2, 15, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(ALUNO_ID, StatusAvaliacao.FINALIZADA, TipoLeituraCodigo.PALAVRA))
                .thenReturn(List.of(av2026, av2027));
        Aluno aluno = matriculaAtiva.getAluno();
        when(matriculaRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of(
                new Matricula(aluno, ano2026, new Turma("1º A", 1, ano2026, null), 1, null),
                new Matricula(aluno, ano2027, new Turma("2º B", 2, ano2027, null), 2, null)));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        assertEquals("1º A", linhas.get(0).turma());
        assertEquals("2º B", linhas.get(1).turma());
    }

    @Test
    void evolucaoAnualOrdenaPorAnoCrescenteConformeORepositorio() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloSaida, ano2026, 2, 10, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 15, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        assertEquals(List.of(2026, 2027), linhas.stream().map(EvolucaoAnualLinha::anoLetivo).toList());
    }

    @Test
    void evolucaoAnualCalculaAbsolutaEPercentualEntreAnosConsecutivos() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloSaida, ano2026, 2, 10, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 15, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        EvolucaoValor evolucao = linhas.get(1).evolucaoSaida();
        assertEquals(5, evolucao.absoluta());
        assertEquals(new BigDecimal("50.00"), evolucao.percentual());
    }

    @Test
    void evolucaoAnualComAnteriorEAtualZeroPercentualZero() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloSaida, ano2026, 2, 0, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 0, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        EvolucaoValor evolucao = linhas.get(1).evolucaoSaida();
        assertEquals(0, evolucao.absoluta());
        assertEquals(BigDecimal.ZERO, evolucao.percentual());
    }

    @Test
    void evolucaoAnualComAnteriorZeroEAtualMaiorQueZeroPercentualNulo() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao av2026 = avaliacaoAnualMock(cicloSaida, ano2026, 2, 0, Instant.now());
        Avaliacao av2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 8, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        EvolucaoValor evolucao = linhas.get(1).evolucaoSaida();
        assertEquals(8, evolucao.absoluta());
        assertNull(evolucao.percentual());
    }

    @Test
    void evolucaoAnualCicloSemAnoAnteriorTemEvolucaoNula() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloEntrada = cicloMock(1L, "ENTRADA");
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Avaliacao entrada2026 = avaliacaoAnualMock(cicloEntrada, ano2026, 2, 10, Instant.now());
        Avaliacao saida2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 12, Instant.now());
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any()))
                .thenReturn(List.of(entrada2026, saida2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        EvolucaoValor evolucaoEntradaAno1 = linhas.get(0).evolucaoEntrada();
        EvolucaoValor evolucaoSaidaAno2 = linhas.get(1).evolucaoSaida();
        assertNull(evolucaoEntradaAno1.absoluta());
        assertNull(evolucaoEntradaAno1.percentual());
        assertNull(evolucaoSaidaAno2.absoluta());
        assertNull(evolucaoSaidaAno2.percentual());
    }

    @Test
    void evolucaoAnualCicloRepetidoNoMesmoAnoUsaOMaisRecente() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        Instant agora = Instant.now();
        Avaliacao maisRecente = avaliacaoAnualMock(cicloSaida, ano2026, 2, 18, agora);
        Avaliacao maisAntiga = avaliacaoAnualMock(cicloSaida, ano2026, 2, 5, agora.minusSeconds(3600));
        // já ordenada por finalizadoEm desc dentro do grupo (buscarFinalizadasPorTipo, HIST-20)
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any()))
                .thenReturn(List.of(maisRecente, maisAntiga));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        assertSame(maisRecente, linhas.get(0).saida());
    }

    @Test
    void evolucaoAnualListaVaziaQuandoAlunoSemFinalizadaDoTipo() {
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of());

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        assertTrue(linhas.isEmpty());
    }

    @Test
    void evolucaoAnualMetricaEQuantidadeCorretasNaoPercentualAcerto() {
        AnoLetivo ano2026 = anoLetivoComId(2026, 300L);
        AnoLetivo ano2027 = anoLetivoComId(2027, 301L);
        Ciclo cicloSaida = cicloMock(3L, "SAIDA");
        // mesmo percentualAcerto nos dois anos, quantidadeCorretas diferente:
        // se a métrica fosse percentualAcerto, a evolução daria 0.
        Avaliacao av2026 = avaliacaoAnualMock(cicloSaida, ano2026, 2, 10, Instant.now());
        lenient().when(av2026.getPercentualAcerto()).thenReturn(new BigDecimal("50.00"));
        Avaliacao av2027 = avaliacaoAnualMock(cicloSaida, ano2027, 3, 20, Instant.now());
        lenient().when(av2027.getPercentualAcerto()).thenReturn(new BigDecimal("50.00"));
        when(avaliacaoRepository.buscarFinalizadasPorTipo(any(), any(), any())).thenReturn(List.of(av2026, av2027));

        List<EvolucaoAnualLinha> linhas = service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA);

        EvolucaoValor evolucao = linhas.get(1).evolucaoSaida();
        assertEquals(10, evolucao.absoluta());
        assertEquals(new BigDecimal("100.00"), evolucao.percentual());
    }

    @Test
    void evolucaoAnualComAlunoInexistenteLanca404() {
        when(alunoService.buscarPorId(ALUNO_ID))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.evolucaoAnual(ALUNO_ID, TipoLeituraCodigo.PALAVRA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("ALUNO_NAO_ENCONTRADO", ex.getCode());
    }
}

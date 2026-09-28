package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.MarcarPalavrasRequest.MarcacaoItem;
import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoRequest;
import com.missio.fluencia_leitora.avaliacao.dto.PalavraDigitadaRequest;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavras;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavrasRepository;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.ConfiguracaoAvaliacao;
import com.missio.fluencia_leitora.cadastros.anoletivo.ConfiguracaoAvaliacaoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import com.missio.fluencia_leitora.avaliacao.dto.AvaliacaoResponse;
import com.missio.fluencia_leitora.regrasclassificacao.Fase;
import com.missio.fluencia_leitora.regrasclassificacao.RegraClassificacaoService;
import com.missio.fluencia_leitora.regrasclassificacao.RegraClassificacaoService.ClassificacaoResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AVA-01..AVA-08 (spec.md, P1 "Criar avaliação", ACs 1-10):
 * {@link AvaliacaoService#criar}. O cenário base é um aluno ativo do 1º ano
 * (limite 15-20 palavras), matriculado no ano letivo ATIVO com o professor
 * que está chamando a API.
 */
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    private static final Long ALUNO_ID = 1L;
    private static final Long PROFESSOR_ID = 7L;
    private static final Long ANO_LETIVO_ID = 100L;
    private static final Long CICLO_ID = 3L;
    private static final Long LISTA_ID = 40L;
    private static final Long USUARIO_ID = 55L;
    private static final Long AVALIACAO_ID = 900L;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private AnoLetivoRepository anoLetivoRepository;

    @Mock
    private ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    @Mock
    private ListaPalavrasRepository listaPalavrasRepository;

    @Mock
    private CicloRepository cicloRepository;

    @Mock
    private ContextoUsuarioPort contextoUsuario;

    @Mock
    private RegraClassificacaoService regraClassificacaoService;

    @Mock
    private AvaliacaoAuditoriaRepository avaliacaoAuditoriaRepository;

    private final LocalDate hoje = LocalDate.now();

    private AnoLetivo anoLetivo;
    private Professor professor;
    private Turma turma;
    private Aluno aluno;
    private Matricula matricula;
    private Ciclo ciclo;

    private AvaliacaoService service;

    @BeforeEach
    void setUp() {
        anoLetivo = new AnoLetivo(hoje.getYear(), hoje.minusDays(30), hoje.plusDays(30));
        ReflectionTestUtils.setField(anoLetivo, "id", ANO_LETIVO_ID);
        anoLetivo.setSituacao(SituacaoAnoLetivo.ATIVO);
        professor = new Professor("Professora Ana");
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        turma = new Turma("Turma 1A", 1, anoLetivo, professor);
        ReflectionTestUtils.setField(turma, "id", 20L);
        aluno = new Aluno("Aluno Teste");
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        matricula = new Matricula(aluno, anoLetivo, turma, 1, professor);
        ciclo = mock(Ciclo.class);

        lenient().when(anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO)).thenReturn(List.of(anoLetivo));
        lenient().when(matriculaRepository.findByAlunoIdAndAnoLetivoId(ALUNO_ID, ANO_LETIVO_ID))
                .thenReturn(Optional.of(matricula));
        lenient().when(cicloRepository.findById(CICLO_ID)).thenReturn(Optional.of(ciclo));
        lenient().when(configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(ANO_LETIVO_ID, 1))
                .thenReturn(Optional.of(new ConfiguracaoAvaliacao(anoLetivo, 1, 15, 20)));
        lenient().when(configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(ANO_LETIVO_ID, 2))
                .thenReturn(Optional.of(new ConfiguracaoAvaliacao(anoLetivo, 2, 3, 60)));
        lenient().when(avaliacaoRepository.save(any(Avaliacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(contextoUsuario.perfilAtual()).thenReturn(Perfil.PROFESSOR);
        lenient().when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID);
        lenient().when(contextoUsuario.usuarioIdAtual()).thenReturn(USUARIO_ID);
        lenient().when(regraClassificacaoService.classificar(anyInt(), anyInt()))
                .thenReturn(new ClassificacaoResultado(Fase.PRE_LEITOR, 1));

        service = new AvaliacaoService(
                avaliacaoRepository,
                matriculaRepository,
                anoLetivoRepository,
                configuracaoAvaliacaoRepository,
                listaPalavrasRepository,
                cicloRepository,
                new PertencimentoProfessorGuard(contextoUsuario),
                contextoUsuario,
                regraClassificacaoService,
                avaliacaoAuditoriaRepository);
    }

    // ---- helpers -------------------------------------------------------

    private static List<PalavraDigitadaRequest> palavras(int quantidade) {
        List<PalavraDigitadaRequest> palavras = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            palavras.add(new PalavraDigitadaRequest("palavra" + (char) ('a' + i % 26), null));
        }
        return palavras;
    }

    private NovaAvaliacaoRequest comPalavras(List<PalavraDigitadaRequest> palavras) {
        return new NovaAvaliacaoRequest(ALUNO_ID, TipoLeituraCodigo.PALAVRA, CICLO_ID, hoje, 60, null, palavras, null);
    }

    private NovaAvaliacaoRequest comTempo(int tempo) {
        return new NovaAvaliacaoRequest(ALUNO_ID, TipoLeituraCodigo.PALAVRA, CICLO_ID, hoje, tempo, null, palavras(15), null);
    }

    private NovaAvaliacaoRequest comData(LocalDate data) {
        return new NovaAvaliacaoRequest(ALUNO_ID, TipoLeituraCodigo.PALAVRA, CICLO_ID, data, 60, null, palavras(15), null);
    }

    private NovaAvaliacaoRequest comLista(TipoLeituraCodigo tipo) {
        return new NovaAvaliacaoRequest(ALUNO_ID, tipo, CICLO_ID, hoje, 60, LISTA_ID, null, null);
    }

    private NovaAvaliacaoRequest comTexto(TipoLeituraCodigo tipo, String texto) {
        return new NovaAvaliacaoRequest(ALUNO_ID, tipo, CICLO_ID, hoje, 60, null, null, texto);
    }

    private ListaPalavras lista(int serie, TipoLeituraCodigo tipo, int quantidade) {
        ListaPalavras lista = new ListaPalavras("Lista", serie, tipo, null, null);
        ReflectionTestUtils.setField(lista, "id", LISTA_ID);
        for (int i = 1; i <= quantidade; i++) {
            lista.adicionarItem("item" + (char) ('a' + i % 26), TipoPalavra.CANONICA, i);
        }
        return lista;
    }

    private BusinessException assertRejeita(NovaAvaliacaoRequest request, HttpStatus status, String code) {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.criar(request));
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
        verify(avaliacaoRepository, never()).save(any());
        return exception;
    }

    // ---- AVA-01 (AC 1): criação -----------------------------------------

    @Test
    void criarComPalavrasDigitadasCriaAvaliacaoCriadaComPalavrasPendentesEmOrdemECopiasDaMatricula() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(14));
        digitadas.add(0, new PalavraDigitadaRequest("gato", TipoPalavra.CANONICA));
        NovaAvaliacaoRequest request = new NovaAvaliacaoRequest(
                ALUNO_ID, TipoLeituraCodigo.PALAVRA, CICLO_ID, hoje.minusDays(1), 90, null, digitadas, null);

        Avaliacao criada = service.criar(request);

        verify(avaliacaoRepository).save(criada);
        assertEquals(StatusAvaliacao.CRIADA, criada.getStatus());
        assertEquals(15, criada.getQuantidadeTotal());
        assertEquals(15, criada.getPalavras().size());
        for (int i = 0; i < 15; i++) {
            PalavraAvaliacao palavra = criada.getPalavras().get(i);
            assertEquals(i + 1, palavra.getOrdem());
            assertEquals(digitadas.get(i).palavra(), palavra.getPalavra());
            assertEquals(StatusPalavra.PENDENTE, palavra.getStatus());
        }
        assertEquals(TipoPalavra.CANONICA, criada.getPalavras().get(0).getTipoPalavra());
        assertSame(aluno, criada.getAluno());
        assertSame(professor, criada.getProfessor());
        assertEquals("Professora Ana", criada.getProfessorNome());
        assertSame(turma, criada.getTurma());
        assertEquals("Turma 1A", criada.getTurmaNome());
        assertEquals(1, criada.getSerie());
        assertSame(anoLetivo, criada.getAnoLetivo());
        assertSame(ciclo, criada.getCiclo());
        assertEquals(TipoLeituraCodigo.PALAVRA, criada.getTipoLeitura());
        assertEquals(hoje.minusDays(1), criada.getDataAvaliacao());
        assertEquals(90, criada.getTempoConfiguradoSegundos());
    }

    @Test
    void criarComListaCopiaAsPalavrasDaListaNaOrdem() {
        ListaPalavras lista = lista(1, TipoLeituraCodigo.PSEUDOPALAVRA, 16);
        when(listaPalavrasRepository.findById(LISTA_ID)).thenReturn(Optional.of(lista));

        Avaliacao criada = service.criar(comLista(TipoLeituraCodigo.PSEUDOPALAVRA));

        assertEquals(StatusAvaliacao.CRIADA, criada.getStatus());
        assertEquals(16, criada.getPalavras().size());
        for (int i = 0; i < 16; i++) {
            PalavraAvaliacao palavra = criada.getPalavras().get(i);
            assertEquals(i + 1, palavra.getOrdem());
            assertEquals(lista.getItens().get(i).getPalavra(), palavra.getPalavra());
            assertEquals(TipoPalavra.CANONICA, palavra.getTipoPalavra());
            assertEquals(StatusPalavra.PENDENTE, palavra.getStatus());
        }
    }

    @Test
    void criarComCicloInexistenteRetorna422ReferenciaInvalida() {
        when(cicloRepository.findById(CICLO_ID)).thenReturn(Optional.empty());

        assertRejeita(comPalavras(palavras(15)), HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA");
    }

    // ---- AVA-02 (AC 2): aluno avaliável ---------------------------------

    @Test
    void criarSemAnoLetivoAtivoRetorna422AlunoNaoAvaliavel() {
        when(anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO)).thenReturn(List.of());

        assertRejeita(comPalavras(palavras(15)), HttpStatus.UNPROCESSABLE_ENTITY, "ALUNO_NAO_AVALIAVEL");
    }

    @Test
    void criarSemMatriculaNoAnoLetivoAtivoRetorna422AlunoNaoAvaliavel() {
        when(matriculaRepository.findByAlunoIdAndAnoLetivoId(ALUNO_ID, ANO_LETIVO_ID)).thenReturn(Optional.empty());

        assertRejeita(comPalavras(palavras(15)), HttpStatus.UNPROCESSABLE_ENTITY, "ALUNO_NAO_AVALIAVEL");
    }

    @Test
    void criarComMatriculaAnoFinalizadoRetorna422AlunoNaoAvaliavel() {
        matricula.setAnoFinalizado(true);

        assertRejeita(comPalavras(palavras(15)), HttpStatus.UNPROCESSABLE_ENTITY, "ALUNO_NAO_AVALIAVEL");
    }

    @Test
    void criarComAlunoInativoRetorna422AlunoNaoAvaliavel() {
        aluno.setAtivo(false);

        assertRejeita(comPalavras(palavras(15)), HttpStatus.UNPROCESSABLE_ENTITY, "ALUNO_NAO_AVALIAVEL");
    }

    // ---- Edge cases: pertencimento (AUTH-09) ----------------------------

    @Test
    void criarParaAlunoDeOutroProfessorRetorna404() {
        when(contextoUsuario.professorIdAtual()).thenReturn(99L);

        assertRejeita(comPalavras(palavras(15)), HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO");
    }

    @Test
    void criarParaMatriculaSemProfessorRetorna404SemFalhar() {
        matricula.setProfessor(null);

        assertRejeita(comPalavras(palavras(15)), HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO");
    }

    // ---- AVA-03 (AC 3): quantidade de palavras --------------------------

    @Test
    void criarPrimeiroAnoCom14PalavrasRetorna422ComMinimoMaximoEInformado() {
        BusinessException exception = assertRejeita(
                comPalavras(palavras(14)), HttpStatus.UNPROCESSABLE_ENTITY, "QUANTIDADE_PALAVRAS_FORA_DO_LIMITE");

        assertEquals(Map.of("minimo", 15, "maximo", 20, "informado", 14), exception.getDetails());
    }

    @Test
    void criarPrimeiroAnoCom21PalavrasRetorna422ComMinimoMaximoEInformado() {
        BusinessException exception = assertRejeita(
                comPalavras(palavras(21)), HttpStatus.UNPROCESSABLE_ENTITY, "QUANTIDADE_PALAVRAS_FORA_DO_LIMITE");

        assertEquals(Map.of("minimo", 15, "maximo", 20, "informado", 21), exception.getDetails());
    }

    @Test
    void criarUsaOLimiteDaSerieDaMatricula() {
        matricula.setSerie(2);

        Avaliacao criada = service.criar(comPalavras(palavras(3)));

        assertEquals(3, criada.getQuantidadeTotal());
        assertEquals(2, criada.getSerie());
    }

    // ---- AVA-04 (AC 4, AC 5): fonte de conteúdo -------------------------

    @Test
    void criarSemNenhumaFonteDeConteudoRetorna422ConteudoInvalido() {
        assertRejeita(comPalavras(null), HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO");
    }

    @Test
    void criarComListaEPalavrasRetorna422ConteudoInvalido() {
        NovaAvaliacaoRequest request = new NovaAvaliacaoRequest(
                ALUNO_ID, TipoLeituraCodigo.PALAVRA, CICLO_ID, hoje, 60, LISTA_ID, palavras(15), null);

        assertRejeita(request, HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO");
    }

    @Test
    void criarComPalavrasETextoRetorna422ConteudoInvalido() {
        NovaAvaliacaoRequest request = new NovaAvaliacaoRequest(
                ALUNO_ID, TipoLeituraCodigo.TEXTO_CURTO, CICLO_ID, hoje, 60, null, palavras(15), "um texto");

        assertRejeita(request, HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO");
    }

    @Test
    void criarComTextoEmTipoDiferenteDeTextoCurtoRetorna422ConteudoInvalido() {
        assertRejeita(
                comTexto(TipoLeituraCodigo.PALAVRA, "o gato pulou o muro alto hoje de manha cedo com a bola do menino"),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "CONTEUDO_INVALIDO");
    }

    @Test
    void criarComListaDeSerieDiferenteRetorna422ListaIncompativel() {
        when(listaPalavrasRepository.findById(LISTA_ID))
                .thenReturn(Optional.of(lista(2, TipoLeituraCodigo.PALAVRA, 16)));

        assertRejeita(comLista(TipoLeituraCodigo.PALAVRA), HttpStatus.UNPROCESSABLE_ENTITY, "LISTA_INCOMPATIVEL");
    }

    @Test
    void criarComListaDeTipoDeLeituraDiferenteRetorna422ListaIncompativel() {
        when(listaPalavrasRepository.findById(LISTA_ID))
                .thenReturn(Optional.of(lista(1, TipoLeituraCodigo.PSEUDOPALAVRA, 16)));

        assertRejeita(comLista(TipoLeituraCodigo.PALAVRA), HttpStatus.UNPROCESSABLE_ENTITY, "LISTA_INCOMPATIVEL");
    }

    @Test
    void criarComListaInexistenteRetorna422ReferenciaInvalida() {
        when(listaPalavrasRepository.findById(LISTA_ID)).thenReturn(Optional.empty());

        assertRejeita(comLista(TipoLeituraCodigo.PALAVRA), HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA");
    }

    // ---- AVA-05 (AC 6): tempo 10-600 -----------------------------------

    @ParameterizedTest
    @ValueSource(ints = {9, 601})
    void criarComTempoForaDe10a600Retorna422(int tempo) {
        assertRejeita(comTempo(tempo), HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO_INVALIDA");
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 600})
    void criarComTempoNosLimitesDe10a600Aceita(int tempo) {
        Avaliacao criada = service.criar(comTempo(tempo));

        assertEquals(tempo, criada.getTempoConfiguradoSegundos());
    }

    // ---- AVA-06 (AC 7): data -------------------------------------------

    @Test
    void criarComDataFuturaRetorna422() {
        assertRejeita(comData(hoje.plusDays(1)), HttpStatus.UNPROCESSABLE_ENTITY, "DATA_AVALIACAO_INVALIDA");
    }

    @Test
    void criarComDataAntesDoInicioDoAnoLetivoAtivoRetorna422() {
        assertRejeita(comData(hoje.minusDays(31)), HttpStatus.UNPROCESSABLE_ENTITY, "DATA_AVALIACAO_INVALIDA");
    }

    @Test
    void criarComDataPassadaDepoisDoFimDoAnoLetivoAtivoRetorna422() {
        ReflectionTestUtils.setField(anoLetivo, "dataFim", hoje.minusDays(5));

        assertRejeita(comData(hoje.minusDays(1)), HttpStatus.UNPROCESSABLE_ENTITY, "DATA_AVALIACAO_INVALIDA");
    }

    @Test
    void criarComDataNoInicioDoAnoLetivoAtivoAceita() {
        Avaliacao criada = service.criar(comData(hoje.minusDays(30)));

        assertEquals(hoje.minusDays(30), criada.getDataAvaliacao());
    }

    // ---- AVA-07 (AC 8): 1º ano sem não canônicas ------------------------

    @Test
    void criarPrimeiroAnoComPalavraDigitadaNaoCanonicaRetorna422ComPosicoes() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(1, new PalavraDigitadaRequest("blusa", TipoPalavra.NAO_CANONICA));
        digitadas.set(4, new PalavraDigitadaRequest("prato", TipoPalavra.NAO_CANONICA));

        BusinessException exception = assertRejeita(
                comPalavras(digitadas), HttpStatus.UNPROCESSABLE_ENTITY, "NAO_CANONICA_PROIBIDA_1_ANO");

        assertEquals(List.of(2, 5), exception.getDetails().get("posicoes"));
    }

    @Test
    void criarPrimeiroAnoComPalavraSemTipoAceitaComTipoNulo() {
        Avaliacao criada = service.criar(comPalavras(palavras(15)));

        assertNull(criada.getPalavras().get(0).getTipoPalavra());
    }

    @Test
    void criarSegundoAnoComPalavraNaoCanonicaAceita() {
        matricula.setSerie(2);
        List<PalavraDigitadaRequest> digitadas = List.of(
                new PalavraDigitadaRequest("blusa", TipoPalavra.NAO_CANONICA),
                new PalavraDigitadaRequest("prato", TipoPalavra.NAO_CANONICA),
                new PalavraDigitadaRequest("gato", TipoPalavra.CANONICA));

        Avaliacao criada = service.criar(comPalavras(digitadas));

        assertEquals(TipoPalavra.NAO_CANONICA, criada.getPalavras().get(0).getTipoPalavra());
    }

    // ---- AVA-08 (AC 9): tokenização do texto ----------------------------

    @Test
    void criarComTextoTokenizaComARegraDoBancoDePalavras() {
        matricula.setSerie(2);

        Avaliacao criada = service.criar(comTexto(TipoLeituraCodigo.TEXTO_CURTO, "  O guarda-chuva, do menino... caiu! "));

        List<String> palavras = criada.getPalavras().stream().map(PalavraAvaliacao::getPalavra).toList();
        assertEquals(List.of("O", "guarda-chuva", "do", "menino", "caiu"), palavras);
        assertEquals(List.of(1, 2, 3, 4, 5), criada.getPalavras().stream().map(PalavraAvaliacao::getOrdem).toList());
        assertNull(criada.getPalavras().get(0).getTipoPalavra());
        assertEquals(StatusPalavra.PENDENTE, criada.getPalavras().get(0).getStatus());
    }

    // ---- AVA-08 (AC 10): formato de cada palavra ------------------------

    @Test
    void criarComPalavraDigitadaVaziaRetorna422ComPosicao() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(2, new PalavraDigitadaRequest("   ", null));

        BusinessException exception =
                assertRejeita(comPalavras(digitadas), HttpStatus.UNPROCESSABLE_ENTITY, "PALAVRA_INVALIDA");

        assertEquals(3, exception.getDetails().get("posicao"));
    }

    @Test
    void criarComPalavraDigitadaComMaisDe60CaracteresRetorna422ComPosicao() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(6, new PalavraDigitadaRequest("a".repeat(61), null));

        BusinessException exception =
                assertRejeita(comPalavras(digitadas), HttpStatus.UNPROCESSABLE_ENTITY, "PALAVRA_INVALIDA");

        assertEquals(7, exception.getDetails().get("posicao"));
    }

    @Test
    void criarComPalavraDigitadaComCaractereInvalidoRetorna422ComPosicao() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(0, new PalavraDigitadaRequest("gat0", null));

        BusinessException exception =
                assertRejeita(comPalavras(digitadas), HttpStatus.UNPROCESSABLE_ENTITY, "PALAVRA_INVALIDA");

        assertEquals(1, exception.getDetails().get("posicao"));
    }

    @Test
    void criarComPalavraDigitadaCom60CaracteresAcentoEHifenAceita() {
        List<PalavraDigitadaRequest> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(0, new PalavraDigitadaRequest("a".repeat(60), null));
        digitadas.set(1, new PalavraDigitadaRequest("guarda-chuva", null));
        digitadas.set(2, new PalavraDigitadaRequest("maçã", null));

        Avaliacao criada = service.criar(comPalavras(digitadas));

        assertEquals("a".repeat(60), criada.getPalavras().get(0).getPalavra());
        assertEquals("guarda-chuva", criada.getPalavras().get(1).getPalavra());
        assertEquals("maçã", criada.getPalavras().get(2).getPalavra());
    }

    @Test
    void criarComTextoQueGeraPalavraInvalidaRetorna422ComPosicao() {
        matricula.setSerie(2);

        BusinessException exception = assertRejeita(
                comTexto(TipoLeituraCodigo.TEXTO_CURTO, "o gato a2b pulou"),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "PALAVRA_INVALIDA");

        assertEquals(3, exception.getDetails().get("posicao"));
    }

    // ---- Transições (AVA-09..AVA-14, AVA-16, AVA-17) --------------------

    /** Avaliação existente com 3 palavras PENDENTE, 60 s configurados, no status pedido. */
    private Avaliacao avaliacaoExistente(StatusAvaliacao status) {
        Avaliacao avaliacao = new Avaliacao(
                aluno, professor, professor.getNome(), turma, turma.getNome(), 1, anoLetivo, ciclo,
                TipoLeituraCodigo.PALAVRA, hoje, 60);
        avaliacao.adicionarPalavra("gato", null);
        avaliacao.adicionarPalavra("bola", null);
        avaliacao.adicionarPalavra("casa", null);
        ReflectionTestUtils.setField(avaliacao, "id", AVALIACAO_ID);
        avaliacao.setStatus(status);
        if (status == StatusAvaliacao.EM_ANDAMENTO) {
            avaliacao.setIniciadoEm(Instant.now());
        }
        lenient().when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        return avaliacao;
    }

    private void assertTransicaoInvalida(Runnable acao, StatusAvaliacao statusAtual, String nomeAcao) {
        BusinessException exception = assertThrows(BusinessException.class, acao::run);
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("TRANSICAO_INVALIDA", exception.getCode());
        assertEquals(statusAtual.name(), exception.getDetails().get("statusAtual"));
        assertEquals(nomeAcao, exception.getDetails().get("acao"));
    }

    private static void assertEntre(Instant antes, Instant valor, Instant depois) {
        assertTrue(!valor.isBefore(antes) && !valor.isAfter(depois), "esperava " + valor + " entre " + antes + " e " + depois);
    }

    // AVA-09 (AC 1)

    @Test
    void iniciarNumaCriadaMudaParaEmAndamentoEGravaIniciadoEm() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.CRIADA);
        Instant antes = Instant.now();

        Avaliacao resultado = service.iniciar(AVALIACAO_ID);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(StatusAvaliacao.EM_ANDAMENTO, resultado.getStatus());
        assertEntre(antes, resultado.getIniciadoEm(), Instant.now());
        assertEquals(0, resultado.getTempoAcumuladoSegundos());
        assertEntre(antes, resultado.getUltimaAtividadeEm(), Instant.now());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"PAUSADA", "FINALIZADA", "CANCELADA"})
    void iniciarForaDeCriadaRetorna409TransicaoInvalida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);

        assertTransicaoInvalida(() -> service.iniciar(AVALIACAO_ID), status, "iniciar");
        assertEquals(status, avaliacao.getStatus());
        verify(avaliacaoRepository, never()).save(any());
    }

    // AVA-10 (ACs 2 e 3)

    @Test
    void pausarNumaEmAndamentoMudaParaPausadaESomaOTrechoAoTotal() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setTempoAcumuladoSegundos(5);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(20));

        Avaliacao resultado = service.pausar(AVALIACAO_ID);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(StatusAvaliacao.PAUSADA, resultado.getStatus());
        assertEquals(25, resultado.getTempoAcumuladoSegundos());
        assertNull(resultado.getIniciadoEm());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "FINALIZADA", "CANCELADA"})
    void pausarForaDeEmAndamentoRetorna409TransicaoInvalida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);

        assertTransicaoInvalida(() -> service.pausar(AVALIACAO_ID), status, "pausar");
        assertEquals(status, avaliacao.getStatus());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void continuarNumaPausadaVoltaParaEmAndamentoSemContarOTempoPausado() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.PAUSADA);
        avaliacao.setTempoAcumuladoSegundos(12);
        Instant antes = Instant.now();

        Avaliacao resultado = service.continuar(AVALIACAO_ID);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(StatusAvaliacao.EM_ANDAMENTO, resultado.getStatus());
        assertEquals(12, resultado.getTempoAcumuladoSegundos());
        assertEntre(antes, resultado.getIniciadoEm(), Instant.now());
    }

    @Test
    void pausarContinuarEPausarDeNovoSomaSoOsTrechosEmAndamento() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(10));
        service.pausar(AVALIACAO_ID);

        service.continuar(AVALIACAO_ID);
        avaliacao.setIniciadoEm(avaliacao.getIniciadoEm().minusSeconds(4));
        service.pausar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.PAUSADA, avaliacao.getStatus());
        assertEquals(14, avaliacao.getTempoAcumuladoSegundos());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "FINALIZADA", "CANCELADA"})
    void continuarForaDePausadaRetorna409TransicaoInvalida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);

        assertTransicaoInvalida(() -> service.continuar(AVALIACAO_ID), status, "continuar");
        assertEquals(status, avaliacao.getStatus());
        verify(avaliacaoRepository, never()).save(any());
    }

    // AVA-11 (AC 4)

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"EM_ANDAMENTO", "PAUSADA"})
    void resetarVoltaParaCriadaZeraOTempoLimpaIniciadoEmEVoltaAsPalavrasParaPendente(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);
        avaliacao.setTempoAcumuladoSegundos(30);
        avaliacao.getPalavras().get(0).setStatus(StatusPalavra.CORRETA);
        avaliacao.getPalavras().get(1).setStatus(StatusPalavra.INCORRETA);
        avaliacao.getPalavras().get(2).setStatus(StatusPalavra.NAO_LIDA);

        Avaliacao resultado = service.resetar(AVALIACAO_ID);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(StatusAvaliacao.CRIADA, resultado.getStatus());
        assertEquals(0, resultado.getTempoAcumuladoSegundos());
        assertNull(resultado.getIniciadoEm());
        assertEquals(
                List.of(StatusPalavra.PENDENTE, StatusPalavra.PENDENTE, StatusPalavra.PENDENTE),
                resultado.getPalavras().stream().map(PalavraAvaliacao::getStatus).toList());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "FINALIZADA", "CANCELADA"})
    void resetarForaDeEmAndamentoOuPausadaRetorna409TransicaoInvalida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);

        assertTransicaoInvalida(() -> service.resetar(AVALIACAO_ID), status, "resetar");
        assertEquals(status, avaliacao.getStatus());
        verify(avaliacaoRepository, never()).save(any());
    }

    // AVA-14 (AC 7): ação repetida no status que ela produziria

    @Test
    void iniciarNumaEmAndamentoRetornaOEstadoAtualSemAlterar() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        Instant iniciadoEm = Instant.now().minusSeconds(8);
        avaliacao.setIniciadoEm(iniciadoEm);
        avaliacao.setTempoAcumuladoSegundos(3);

        Avaliacao resultado = service.iniciar(AVALIACAO_ID);

        assertSame(avaliacao, resultado);
        assertEquals(StatusAvaliacao.EM_ANDAMENTO, resultado.getStatus());
        assertEquals(iniciadoEm, resultado.getIniciadoEm());
        assertEquals(3, resultado.getTempoAcumuladoSegundos());
    }

    @Test
    void continuarNumaEmAndamentoRetornaOEstadoAtualSemAlterar() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        Instant iniciadoEm = Instant.now().minusSeconds(8);
        avaliacao.setIniciadoEm(iniciadoEm);
        avaliacao.setTempoAcumuladoSegundos(3);

        Avaliacao resultado = service.continuar(AVALIACAO_ID);

        assertSame(avaliacao, resultado);
        assertEquals(StatusAvaliacao.EM_ANDAMENTO, resultado.getStatus());
        assertEquals(iniciadoEm, resultado.getIniciadoEm());
        assertEquals(3, resultado.getTempoAcumuladoSegundos());
    }

    @Test
    void pausarNumaPausadaRetornaOEstadoAtualSemAlterar() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.PAUSADA);
        avaliacao.setTempoAcumuladoSegundos(17);

        Avaliacao resultado = service.pausar(AVALIACAO_ID);

        assertSame(avaliacao, resultado);
        assertEquals(StatusAvaliacao.PAUSADA, resultado.getStatus());
        assertEquals(17, resultado.getTempoAcumuladoSegundos());
        assertNull(resultado.getIniciadoEm());
    }

    // AVA-17 (AC 8): finalização preguiçosa antes do comando

    @Test
    void pausarComTempoEsgotadoFinalizaAntesERetorna409ComStatusFinalizada() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setTempoAcumuladoSegundos(40);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(25));

        assertTransicaoInvalida(() -> service.pausar(AVALIACAO_ID), StatusAvaliacao.FINALIZADA, "pausar");

        assertEquals(StatusAvaliacao.FINALIZADA, avaliacao.getStatus());
        assertEquals(60, avaliacao.getTempoUtilizadoSegundos());
        assertTrue(avaliacao.getFinalizadoEm() != null);
        assertNull(avaliacao.getIniciadoEm());
    }

    @Test
    void pausarComTempoAindaNaoEsgotadoNaoFinaliza() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setTempoAcumuladoSegundos(40);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(19));

        service.pausar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.PAUSADA, avaliacao.getStatus());
        assertEquals(59, avaliacao.getTempoAcumuladoSegundos());
        assertNull(avaliacao.getFinalizadoEm());
    }

    // Existência e pertencimento (AUTH-09)

    @Test
    void transicaoDeAvaliacaoInexistenteRetorna404() {
        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.iniciar(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RECURSO_NAO_ENCONTRADO", exception.getCode());
    }

    @Test
    void transicaoDeAvaliacaoDeOutroProfessorRetorna404SemAlterar() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.CRIADA);
        when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID + 1);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.iniciar(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RECURSO_NAO_ENCONTRADO", exception.getCode());
        assertEquals(StatusAvaliacao.CRIADA, avaliacao.getStatus());
        verify(avaliacaoRepository, never()).save(any());
    }

    // AVA-16 (AC 9): log INFO da transição

    @Test
    void transicaoRegistraLogInfoComAvaliacaoOrigemDestinoEUsuario() {
        avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        Logger logger = (Logger) LoggerFactory.getLogger(AvaliacaoService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.pausar(AVALIACAO_ID);
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(1, appender.list.size());
        ILoggingEvent evento = appender.list.get(0);
        assertEquals(Level.INFO, evento.getLevel());
        assertEquals(
                "Transição de avaliação: avaliacaoId=900 origem=EM_ANDAMENTO destino=PAUSADA usuarioId=55",
                evento.getFormattedMessage());
    }

    // ---- Finalização e resultado (AVA-12, AVA-17, AVA-20..AVA-22) --------

    /** Avaliação com as palavras nos status dados, na ordem: corretas, incorretas, não lidas, pendentes. */
    private Avaliacao avaliacaoComPalavras(StatusAvaliacao status, int corretas, int incorretas, int naoLidas, int pendentes) {
        Avaliacao avaliacao = new Avaliacao(
                aluno, professor, professor.getNome(), turma, turma.getNome(), 1, anoLetivo, ciclo,
                TipoLeituraCodigo.PALAVRA, hoje, 60);
        List<StatusPalavra> statuses = new ArrayList<>();
        statuses.addAll(Collections.nCopies(corretas, StatusPalavra.CORRETA));
        statuses.addAll(Collections.nCopies(incorretas, StatusPalavra.INCORRETA));
        statuses.addAll(Collections.nCopies(naoLidas, StatusPalavra.NAO_LIDA));
        statuses.addAll(Collections.nCopies(pendentes, StatusPalavra.PENDENTE));
        for (StatusPalavra statusPalavra : statuses) {
            avaliacao.adicionarPalavra("palavra", null);
            avaliacao.getPalavras().get(avaliacao.getPalavras().size() - 1).setStatus(statusPalavra);
        }
        ReflectionTestUtils.setField(avaliacao, "id", AVALIACAO_ID);
        avaliacao.setStatus(status);
        if (status == StatusAvaliacao.EM_ANDAMENTO) {
            avaliacao.setIniciadoEm(Instant.now());
        }
        lenient().when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        return avaliacao;
    }

    @Test
    void finalizarNumaEmAndamentoGravaFinalizadaFinalizadoEmETempoUtilizadoSomado() {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.EM_ANDAMENTO, 1, 0, 0, 2);
        avaliacao.setTempoAcumuladoSegundos(10);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(15));
        Instant antes = Instant.now();

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertEntre(antes, resultado.getFinalizadoEm(), Instant.now());
        assertEquals(25, resultado.getTempoUtilizadoSegundos());
        assertNull(resultado.getIniciadoEm());
    }

    @Test
    void finalizarNumaPausadaGravaOTempoAcumuladoComoTempoUtilizado() {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.PAUSADA, 1, 0, 0, 2);
        avaliacao.setTempoAcumuladoSegundos(33);

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertEquals(33, resultado.getTempoUtilizadoSegundos());
    }

    @Test
    void finalizarConverteAsPalavrasPendenteEmNaoLidaSemMexerNasMarcadas() {
        avaliacaoComPalavras(StatusAvaliacao.PAUSADA, 1, 1, 0, 2);

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(
                List.of(StatusPalavra.CORRETA, StatusPalavra.INCORRETA, StatusPalavra.NAO_LIDA, StatusPalavra.NAO_LIDA),
                resultado.getPalavras().stream().map(PalavraAvaliacao::getStatus).toList());
    }

    @Test
    void finalizarExemploDoSdd13GravaLidas13Percentual45ELeitorInicianteSemNivel() {
        avaliacaoComPalavras(StatusAvaliacao.EM_ANDAMENTO, 9, 4, 3, 4);
        when(regraClassificacaoService.classificar(1, 9))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(20, resultado.getQuantidadeTotal());
        assertEquals(9, resultado.getQuantidadeCorretas());
        assertEquals(4, resultado.getQuantidadeIncorretas());
        assertEquals(7, resultado.getQuantidadeNaoLidas());
        assertEquals(new BigDecimal("45.00"), resultado.getPercentualAcerto());
        assertEquals(Fase.LEITOR_INICIANTE, resultado.getFase());
        assertNull(resultado.getNivel());
        AvaliacaoResponse response = AvaliacaoResponse.from(resultado);
        assertEquals(13, response.quantidadeLidas());
        assertEquals(false, response.classificacaoPendente());
    }

    @Test
    void finalizarGravaFaseENivelDaClassificacaoPelaSerieECorretas() {
        avaliacaoComPalavras(StatusAvaliacao.PAUSADA, 5, 10, 0, 0);
        when(regraClassificacaoService.classificar(1, 5)).thenReturn(new ClassificacaoResultado(Fase.PRE_LEITOR, 2));

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(Fase.PRE_LEITOR, resultado.getFase());
        assertEquals(2, resultado.getNivel());
    }

    @Test
    void finalizarArredondaOPercentualComDuasCasasHalfUp() {
        avaliacaoComPalavras(StatusAvaliacao.PAUSADA, 2, 1, 0, 0);

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(new BigDecimal("66.67"), resultado.getPercentualAcerto());
    }

    @Test
    void finalizarSemClassificacaoFinalizaComFaseENivelNulosEClassificacaoPendente() {
        avaliacaoComPalavras(StatusAvaliacao.PAUSADA, 9, 4, 7, 0);
        when(regraClassificacaoService.classificar(1, 9)).thenReturn(new ClassificacaoResultado(null, null));

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertNull(resultado.getFase());
        assertNull(resultado.getNivel());
        assertEquals(new BigDecimal("45.00"), resultado.getPercentualAcerto());
        assertTrue(AvaliacaoResponse.from(resultado).classificacaoPendente());
    }

    @Test
    void finalizarComTodasAsPalavrasPendenteGravaZeroCorretasTodasNaoLidasEPercentualZero() {
        avaliacaoComPalavras(StatusAvaliacao.EM_ANDAMENTO, 0, 0, 0, 15);

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(0, resultado.getQuantidadeCorretas());
        assertEquals(0, resultado.getQuantidadeIncorretas());
        assertEquals(15, resultado.getQuantidadeNaoLidas());
        assertEquals(new BigDecimal("0.00"), resultado.getPercentualAcerto());
        verify(regraClassificacaoService).classificar(1, 0);
    }

    @Test
    void finalizarComTempoJaEsgotadoFinalizaPreguicosamenteComTempoUtilizadoIgualAoConfigurado() {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.EM_ANDAMENTO, 9, 4, 0, 7);
        avaliacao.setTempoAcumuladoSegundos(50);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(30));
        when(regraClassificacaoService.classificar(1, 9))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertEquals(60, resultado.getTempoUtilizadoSegundos());
        assertEquals(7, resultado.getQuantidadeNaoLidas());
        assertEquals(new BigDecimal("45.00"), resultado.getPercentualAcerto());
        assertEquals(Fase.LEITOR_INICIANTE, resultado.getFase());
        verify(regraClassificacaoService).classificar(1, 9);
    }

    @Test
    void finalizarNumaFinalizadaRetornaOEstadoAtualSemRecalcular() {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.FINALIZADA, 9, 4, 7, 0);
        Instant finalizadoEm = Instant.now().minusSeconds(100);
        avaliacao.setFinalizadoEm(finalizadoEm);
        avaliacao.setTempoUtilizadoSegundos(42);
        avaliacao.setQuantidadeCorretas(9);
        avaliacao.setFase(Fase.LEITOR_INICIANTE);

        Avaliacao resultado = service.finalizar(AVALIACAO_ID);

        assertSame(avaliacao, resultado);
        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertEquals(finalizadoEm, resultado.getFinalizadoEm());
        assertEquals(42, resultado.getTempoUtilizadoSegundos());
        assertEquals(Fase.LEITOR_INICIANTE, resultado.getFase());
        verify(regraClassificacaoService, never()).classificar(anyInt(), anyInt());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "CANCELADA"})
    void finalizarNumaCriadaOuCanceladaRetorna409TransicaoInvalida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoComPalavras(status, 0, 0, 0, 3);

        assertTransicaoInvalida(() -> service.finalizar(AVALIACAO_ID), status, "finalizar");
        assertEquals(status, avaliacao.getStatus());
        assertNull(avaliacao.getFinalizadoEm());
        verify(avaliacaoRepository, never()).save(any());
    }

    // ---- Marcação de palavras (AVA-15, AVA-18, AVA-19) -------------------

    private List<StatusPalavra> statusDasPalavras(Avaliacao avaliacao) {
        return avaliacao.getPalavras().stream().map(PalavraAvaliacao::getStatus).toList();
    }

    /** FINALIZADA com as palavras dadas e o resultado já calculado, como a finalização deixaria. */
    private Avaliacao finalizadaComPalavras(int corretas, int incorretas, int naoLidas, Fase fase, Integer nivel) {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.FINALIZADA, corretas, incorretas, naoLidas, 0);
        avaliacao.setQuantidadeCorretas(corretas);
        avaliacao.setQuantidadeIncorretas(incorretas);
        avaliacao.setQuantidadeNaoLidas(naoLidas);
        avaliacao.setFase(fase);
        avaliacao.setNivel(nivel);
        return avaliacao;
    }

    private BusinessException assertMarcacaoRejeitada(Runnable acao, HttpStatus status, String code) {
        BusinessException exception = assertThrows(BusinessException.class, acao::run);
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
        verify(avaliacaoRepository, never()).save(any());
        verify(avaliacaoAuditoriaRepository, never()).save(any());
        return exception;
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"EM_ANDAMENTO", "PAUSADA"})
    void marcarPalavraEmAndamentoOuPausadaGravaOStatusSemAuditoria(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);
        Instant antes = Instant.now();

        Avaliacao resultado = service.marcarPalavra(AVALIACAO_ID, 2, StatusPalavra.CORRETA);

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(List.of(StatusPalavra.PENDENTE, StatusPalavra.CORRETA, StatusPalavra.PENDENTE), statusDasPalavras(resultado));
        assertEquals(status, resultado.getStatus());
        assertEntre(antes, resultado.getUltimaAtividadeEm(), Instant.now());
        verify(avaliacaoAuditoriaRepository, never()).save(any());
        verify(regraClassificacaoService, never()).classificar(anyInt(), anyInt());
    }

    @Test
    void marcarPalavraEmAndamentoAceitaVoltarParaPendente() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.getPalavras().get(0).setStatus(StatusPalavra.INCORRETA);

        service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.PENDENTE);

        assertEquals(StatusPalavra.PENDENTE, avaliacao.getPalavras().get(0).getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "CANCELADA"})
    void marcarPalavraEmCriadaOuCanceladaRetorna409MarcacaoNaoPermitida(StatusAvaliacao status) {
        Avaliacao avaliacao = avaliacaoExistente(status);

        BusinessException exception = assertMarcacaoRejeitada(
                () -> service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.CORRETA),
                HttpStatus.CONFLICT, "MARCACAO_NAO_PERMITIDA");

        assertEquals(status.name(), exception.getDetails().get("statusAtual"));
        assertEquals(StatusPalavra.PENDENTE, avaliacao.getPalavras().get(0).getStatus());
    }

    @Test
    void marcarPalavraComOrdemInexistenteRetorna404() {
        avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);

        BusinessException exception = assertMarcacaoRejeitada(
                () -> service.marcarPalavra(AVALIACAO_ID, 4, StatusPalavra.CORRETA),
                HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO");

        assertEquals(4, exception.getDetails().get("ordem"));
    }

    @Test
    void marcarPalavraPendenteNumaFinalizadaRetorna422SemMudar() {
        Avaliacao avaliacao = finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);

        assertMarcacaoRejeitada(
                () -> service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.PENDENTE),
                HttpStatus.UNPROCESSABLE_ENTITY, "STATUS_PALAVRA_INVALIDO");

        assertEquals(StatusPalavra.CORRETA, avaliacao.getPalavras().get(0).getStatus());
        assertEquals(9, avaliacao.getQuantidadeCorretas());
    }

    @Test
    void marcarPalavraNumaFinalizadaRecalculaEGeraUmaAuditoria() {
        Avaliacao avaliacao = finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);
        when(regraClassificacaoService.classificar(1, 10))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));
        Instant antes = Instant.now();

        Avaliacao resultado = service.marcarPalavra(AVALIACAO_ID, 14, StatusPalavra.CORRETA);

        assertEquals(StatusPalavra.CORRETA, resultado.getPalavras().get(13).getStatus());
        assertEquals(10, resultado.getQuantidadeCorretas());
        assertEquals(4, resultado.getQuantidadeIncorretas());
        assertEquals(6, resultado.getQuantidadeNaoLidas());
        assertEquals(new BigDecimal("50.00"), resultado.getPercentualAcerto());
        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        ArgumentCaptor<AvaliacaoAuditoria> captor = ArgumentCaptor.forClass(AvaliacaoAuditoria.class);
        verify(avaliacaoAuditoriaRepository, times(1)).save(captor.capture());
        AvaliacaoAuditoria auditoria = captor.getValue();
        assertSame(avaliacao, auditoria.getAvaliacao());
        assertEquals(USUARIO_ID, auditoria.getUsuarioId());
        assertEquals(AcaoAuditoria.MARCACAO_PALAVRA, auditoria.getAcao());
        assertEquals("palavra 14: NAO_LIDA", auditoria.getValorAnterior());
        assertEquals("palavra 14: CORRETA", auditoria.getValorNovo());
        assertNull(auditoria.getJustificativa());
        assertEntre(antes, resultado.getUltimaAtividadeEm(), Instant.now());
    }

    @Test
    void marcarPalavraNumaFinalizadaQueMudaAClassificacaoRegistraAClassificacaoAnteriorEANova() {
        finalizadaComPalavras(7, 4, 9, Fase.PRE_LEITOR, 4);
        when(regraClassificacaoService.classificar(1, 8))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));

        Avaliacao resultado = service.marcarPalavra(AVALIACAO_ID, 12, StatusPalavra.CORRETA);

        assertEquals(Fase.LEITOR_INICIANTE, resultado.getFase());
        assertNull(resultado.getNivel());
        ArgumentCaptor<AvaliacaoAuditoria> captor = ArgumentCaptor.forClass(AvaliacaoAuditoria.class);
        verify(avaliacaoAuditoriaRepository).save(captor.capture());
        assertEquals("palavra 12: NAO_LIDA (classificação: PRE_LEITOR/4)", captor.getValue().getValorAnterior());
        assertEquals("palavra 12: CORRETA (classificação: LEITOR_INICIANTE/-)", captor.getValue().getValorNovo());
    }

    @Test
    void marcarPalavraComOStatusAtualRetornaSemAuditoriaNemRecalculo() {
        Avaliacao avaliacao = finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);

        Avaliacao resultado = service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.CORRETA);

        assertSame(avaliacao, resultado);
        assertEquals(9, resultado.getQuantidadeCorretas());
        verify(avaliacaoAuditoriaRepository, never()).save(any());
        verify(regraClassificacaoService, never()).classificar(anyInt(), anyInt());
    }

    @Test
    void marcarPalavrasEmLoteGravaTodosOsItens() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);

        service.marcarPalavras(AVALIACAO_ID, List.of(
                new MarcacaoItem(1, StatusPalavra.CORRETA),
                new MarcacaoItem(2, StatusPalavra.INCORRETA),
                new MarcacaoItem(3, StatusPalavra.NAO_LIDA)));

        verify(avaliacaoRepository).save(avaliacao);
        assertEquals(List.of(StatusPalavra.CORRETA, StatusPalavra.INCORRETA, StatusPalavra.NAO_LIDA), statusDasPalavras(avaliacao));
    }

    @Test
    void marcarPalavrasEmLoteComUmaOrdemInexistenteNaoGravaNenhum() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);

        assertMarcacaoRejeitada(
                () -> service.marcarPalavras(AVALIACAO_ID, List.of(
                        new MarcacaoItem(1, StatusPalavra.CORRETA),
                        new MarcacaoItem(9, StatusPalavra.CORRETA))),
                HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO");

        assertEquals(List.of(StatusPalavra.PENDENTE, StatusPalavra.PENDENTE, StatusPalavra.PENDENTE), statusDasPalavras(avaliacao));
    }

    @Test
    void marcarPalavrasEmLoteNumaFinalizadaComUmPendenteNaoGravaNenhumNemAudita() {
        Avaliacao avaliacao = finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);

        assertMarcacaoRejeitada(
                () -> service.marcarPalavras(AVALIACAO_ID, List.of(
                        new MarcacaoItem(14, StatusPalavra.CORRETA),
                        new MarcacaoItem(15, StatusPalavra.PENDENTE))),
                HttpStatus.UNPROCESSABLE_ENTITY, "STATUS_PALAVRA_INVALIDO");

        assertEquals(StatusPalavra.NAO_LIDA, avaliacao.getPalavras().get(13).getStatus());
        assertEquals(9, avaliacao.getQuantidadeCorretas());
    }

    @Test
    void marcarPalavrasEmLoteNumaFinalizadaGeraUmaAuditoriaPorPalavraAlterada() {
        Avaliacao avaliacao = finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);
        when(regraClassificacaoService.classificar(anyInt(), anyInt()))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));

        service.marcarPalavras(AVALIACAO_ID, List.of(
                new MarcacaoItem(14, StatusPalavra.CORRETA),
                new MarcacaoItem(1, StatusPalavra.CORRETA),
                new MarcacaoItem(15, StatusPalavra.INCORRETA)));

        ArgumentCaptor<AvaliacaoAuditoria> captor = ArgumentCaptor.forClass(AvaliacaoAuditoria.class);
        verify(avaliacaoAuditoriaRepository, times(2)).save(captor.capture());
        assertEquals(List.of("palavra 14: CORRETA", "palavra 15: INCORRETA"),
                captor.getAllValues().stream().map(AvaliacaoAuditoria::getValorNovo).toList());
        assertEquals(10, avaliacao.getQuantidadeCorretas());
        assertEquals(5, avaliacao.getQuantidadeIncorretas());
        assertEquals(5, avaliacao.getQuantidadeNaoLidas());
    }

    @Test
    void marcarPalavraComTempoEsgotadoFinalizaAntesEAuditaAMudanca() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(61));

        service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.CORRETA);

        assertEquals(StatusAvaliacao.FINALIZADA, avaliacao.getStatus());
        assertEquals(60, avaliacao.getTempoUtilizadoSegundos());
        assertEquals(1, avaliacao.getQuantidadeCorretas());
        assertEquals(2, avaliacao.getQuantidadeNaoLidas());
        ArgumentCaptor<AvaliacaoAuditoria> captor = ArgumentCaptor.forClass(AvaliacaoAuditoria.class);
        verify(avaliacaoAuditoriaRepository).save(captor.capture());
        assertEquals("palavra 1: NAO_LIDA", captor.getValue().getValorAnterior());
    }

    @Test
    void marcarPalavraDeAvaliacaoDeOutroProfessorRetorna404() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID + 1);

        assertMarcacaoRejeitada(
                () -> service.marcarPalavra(AVALIACAO_ID, 1, StatusPalavra.CORRETA),
                HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO");

        assertEquals(StatusPalavra.PENDENTE, avaliacao.getPalavras().get(0).getStatus());
    }

    // ---- Consulta (AVA-23) ----------------------------------------------

    @Test
    void buscarRetornaAAvaliacaoSemAlterar() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.PAUSADA);
        avaliacao.setTempoAcumuladoSegundos(70);
        Instant ultimaAtividade = avaliacao.getUltimaAtividadeEm();

        Avaliacao resultado = service.buscar(AVALIACAO_ID);

        assertSame(avaliacao, resultado);
        assertEquals(StatusAvaliacao.PAUSADA, resultado.getStatus());
        assertNull(resultado.getFinalizadoEm());
        assertEquals(ultimaAtividade, resultado.getUltimaAtividadeEm());
    }

    @Test
    void buscarAvaliacaoInexistenteRetorna404() {
        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.buscar(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RECURSO_NAO_ENCONTRADO", exception.getCode());
    }

    @Test
    void buscarEmAndamentoComTempoEsgotadoFinalizaAntesDeRetornar() {
        Avaliacao avaliacao = avaliacaoComPalavras(StatusAvaliacao.EM_ANDAMENTO, 9, 4, 0, 7);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(60));
        when(regraClassificacaoService.classificar(1, 9))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));

        Avaliacao resultado = service.buscar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.FINALIZADA, resultado.getStatus());
        assertEquals(60, resultado.getTempoUtilizadoSegundos());
        assertTrue(resultado.getFinalizadoEm() != null);
        assertEquals(7, resultado.getQuantidadeNaoLidas());
        assertEquals(Fase.LEITOR_INICIANTE, resultado.getFase());
    }

    @Test
    void buscarEmAndamentoComTempoRestanteNaoFinaliza() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.EM_ANDAMENTO);
        avaliacao.setIniciadoEm(Instant.now().minusSeconds(59));

        Avaliacao resultado = service.buscar(AVALIACAO_ID);

        assertEquals(StatusAvaliacao.EM_ANDAMENTO, resultado.getStatus());
        assertNull(resultado.getFinalizadoEm());
    }

    @Test
    void buscarAvaliacaoDeOutroProfessorRetorna404() {
        avaliacaoExistente(StatusAvaliacao.CRIADA);
        when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID + 1);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.buscar(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void buscarComoCoordenadorRetornaAvaliacaoDeQualquerProfessor() {
        Avaliacao avaliacao = avaliacaoExistente(StatusAvaliacao.CRIADA);
        when(contextoUsuario.perfilAtual()).thenReturn(Perfil.COORDENADOR);

        assertSame(avaliacao, service.buscar(AVALIACAO_ID));
    }

    // ---- Consulta da auditoria (AVA-26) ----------------------------------

    @Test
    void consultarAuditoriaDepoisDeDuasAlteracoesRetornaOsDoisRegistrosEmOrdem() {
        finalizadaComPalavras(9, 4, 7, Fase.LEITOR_INICIANTE, null);
        when(regraClassificacaoService.classificar(anyInt(), anyInt()))
                .thenReturn(new ClassificacaoResultado(Fase.LEITOR_INICIANTE, null));
        service.marcarPalavra(AVALIACAO_ID, 14, StatusPalavra.CORRETA);
        service.marcarPalavra(AVALIACAO_ID, 15, StatusPalavra.INCORRETA);
        ArgumentCaptor<AvaliacaoAuditoria> captor = ArgumentCaptor.forClass(AvaliacaoAuditoria.class);
        verify(avaliacaoAuditoriaRepository, times(2)).save(captor.capture());
        when(avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(AVALIACAO_ID))
                .thenReturn(captor.getAllValues());

        List<AvaliacaoAuditoria> auditorias = service.consultarAuditoria(AVALIACAO_ID);

        assertEquals(2, auditorias.size());
        assertEquals("palavra 14: CORRETA", auditorias.get(0).getValorNovo());
        assertEquals("palavra 15: INCORRETA", auditorias.get(1).getValorNovo());
    }

    @Test
    void consultarAuditoriaDeAvaliacaoInexistenteRetorna404() {
        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.consultarAuditoria(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RECURSO_NAO_ENCONTRADO", exception.getCode());
        verify(avaliacaoAuditoriaRepository, never()).findByAvaliacaoIdOrderByDataHoraAsc(any());
    }

    @Test
    void consultarAuditoriaDeAvaliacaoDeOutroProfessorRetorna404() {
        avaliacaoExistente(StatusAvaliacao.FINALIZADA);
        when(contextoUsuario.professorIdAtual()).thenReturn(PROFESSOR_ID + 1);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.consultarAuditoria(AVALIACAO_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(avaliacaoAuditoriaRepository, never()).findByAvaliacaoIdOrderByDataHoraAsc(any());
    }
}

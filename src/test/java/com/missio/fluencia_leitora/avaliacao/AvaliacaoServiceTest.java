package com.missio.fluencia_leitora.avaliacao;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

        service = new AvaliacaoService(
                avaliacaoRepository,
                matriculaRepository,
                anoLetivoRepository,
                configuracaoAvaliacaoRepository,
                listaPalavrasRepository,
                cicloRepository,
                new PertencimentoProfessorGuard(contextoUsuario));
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
}

package com.missio.fluencia_leitora.avaliacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavras;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavrasRepository;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoRepository;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoService;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AVA-01..AVA-08: {@code POST /api/v1/avaliacoes} contra MySQL real -
 * caminho feliz (palavras digitadas, lista e texto), um teste por regra de
 * 422 (lição L-016), pertencimento (AUTH-09: 404), autorização (só
 * PROFESSOR cria: 403) e autenticação (401). AVA-09..AVA-14, AVA-17,
 * AVA-25: rotas de transição - tabela de status inteira, efeitos de tempo e
 * reset, exemplo do SDD §13 com a seed real, finalização preguiçosa e
 * conflito de versão. AVA-15, AVA-18, AVA-19: marcação individual e em
 * lote, rollback do lote e auditoria depois de finalizar. AVA-23, AVA-26:
 * consulta da avaliação (os dois ramos do resultado - lição L-014) e da
 * auditoria, para o professor dono e o COORDENADOR.
 *
 * <p>Cada teste cria o seu próprio ano letivo e o ativa (encerrando o ativo
 * anterior - CAD-04), com período de hoje-60 a hoje+60 dias, para que
 * "hoje" seja sempre uma data válida. O contador de {@code ano} começa em
 * 2700, longe dos contadores dos outros *IT (2050-2600).
 */
@AutoConfigureMockMvc
class AvaliacaoControllerIT extends IntegrationTestBase {

    private static int anoSequencial = 2700;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    /** AVA-27..AVA-32: grava o áudio de teste num diretório temporário, em vez do `data/audios` real do projeto. */
    @DynamicPropertySource
    static void audioStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("APP_AUDIO_STORAGE_DIR",
                () -> System.getProperty("java.io.tmpdir") + "/fluencia-leitora-audio-it-" + UUID.randomUUID());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnoLetivoService anoLetivoService;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private ListaPalavrasRepository listaPalavrasRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AvaliacaoAuditoriaRepository avaliacaoAuditoriaRepository;

    @Autowired
    private AvaliacaoAudioRepository avaliacaoAudioRepository;

    @MockitoSpyBean
    private PertencimentoProfessorGuard pertencimentoProfessorGuard;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LocalDate hoje = LocalDate.now();

    private AnoLetivo anoLetivo;
    private Long cicloId;

    @BeforeEach
    void novoAnoLetivoAtivo() {
        AnoLetivo criado = anoLetivoService.criar(proximoAno(), hoje.minusDays(60), hoje.plusDays(60));
        anoLetivo = anoLetivoService.ativar(criado.getId());
        cicloId = cicloRepository.findAll().get(0).getId();
    }

    // ---- helpers -------------------------------------------------------

    private Professor novoProfessor() {
        return professorRepository.save(new Professor("Professora " + UUID.randomUUID()));
    }

    private String bearerProfessor(Professor professor) {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-aval-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    private Matricula novaMatricula(int serie, Professor professor) {
        Turma turma = turmaRepository.save(new Turma("Turma Aval " + UUID.randomUUID(), serie, anoLetivo, professor));
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Avaliacao"));
        return matriculaRepository.save(new Matricula(aluno, anoLetivo, turma, serie, professor));
    }

    private static List<Map<String, Object>> palavras(int quantidade) {
        List<Map<String, Object>> palavras = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            palavras.add(Map.of("palavra", "palavra" + (char) ('a' + i % 26)));
        }
        return palavras;
    }

    private Map<String, Object> payload(Long alunoId, String tipoLeitura) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alunoId", alunoId);
        payload.put("tipoLeitura", tipoLeitura);
        payload.put("cicloId", cicloId);
        payload.put("dataAvaliacao", hoje.toString());
        payload.put("tempoSegundos", 60);
        return payload;
    }

    private Map<String, Object> payloadComPalavras(Long alunoId, List<Map<String, Object>> palavras) {
        Map<String, Object> payload = payload(alunoId, "PALAVRA");
        payload.put("palavras", palavras);
        return payload;
    }

    private ResultActions postar(String bearer, Map<String, Object> payload) throws Exception {
        return mockMvc.perform(post("/api/v1/avaliacoes").header("Authorization", bearer)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)));
    }

    @Test
    void getPendentesListaTodasAsAvaliacoesNaoConcluidasDoAluno() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Long alunoId = matricula.getAluno().getId();
        String bearer = bearerProfessor(professor);
        postar(bearer, payloadComPalavras(alunoId, palavras(15))).andExpect(status().isCreated());
        postar(bearer, payloadComPalavras(alunoId, palavras(15))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/avaliacoes/pendentes").param("alunoId", alunoId.toString())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("CRIADA"));
    }

    // ---- AVA-01: caminho feliz -----------------------------------------

    @Test
    void postComPalavrasDigitadasRetorna201ComCorpoCompleto() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        List<Map<String, Object>> digitadas = new ArrayList<>(palavras(14));
        digitadas.add(0, Map.of("palavra", "gato", "tipoPalavra", "CANONICA"));
        Map<String, Object> payload = payloadComPalavras(matricula.getAluno().getId(), digitadas);
        payload.put("tempoSegundos", 90);

        MvcResult result = postar(bearerProfessor(professor), payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andExpect(jsonPath("$.alunoId").value(matricula.getAluno().getId()))
                .andExpect(jsonPath("$.professorId").value(professor.getId()))
                .andExpect(jsonPath("$.professorNome").value(professor.getNome()))
                .andExpect(jsonPath("$.turmaId").value(matricula.getTurma().getId()))
                .andExpect(jsonPath("$.turmaNome").value(matricula.getTurma().getNome()))
                .andExpect(jsonPath("$.serie").value(1))
                .andExpect(jsonPath("$.anoLetivoId").value(anoLetivo.getId()))
                .andExpect(jsonPath("$.cicloId").value(cicloId))
                .andExpect(jsonPath("$.tipoLeitura").value("PALAVRA"))
                .andExpect(jsonPath("$.dataAvaliacao").value(hoje.toString()))
                .andExpect(jsonPath("$.tempoConfiguradoSegundos").value(90))
                .andExpect(jsonPath("$.quantidadeTotal").value(15))
                .andExpect(jsonPath("$.palavras.length()").value(15))
                .andExpect(jsonPath("$.palavras[0].ordem").value(1))
                .andExpect(jsonPath("$.palavras[0].palavra").value("gato"))
                .andExpect(jsonPath("$.palavras[0].tipoPalavra").value("CANONICA"))
                .andExpect(jsonPath("$.palavras[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.palavras[14].ordem").value(15))
                .andExpect(jsonPath("$.palavras[14].status").value("PENDENTE"))
                .andExpect(jsonPath("$.quantidadeCorretas").value(nullValue()))
                .andExpect(jsonPath("$.fase").value(nullValue()))
                .andExpect(jsonPath("$.classificacaoPendente").value(false))
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertEquals(StatusAvaliacao.CRIADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void postComListaRetorna201ComAsPalavrasDaLista() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        ListaPalavras lista = new ListaPalavras("Lista Aval " + UUID.randomUUID(), 1, TipoLeituraCodigo.PALAVRA, null, null);
        for (int i = 1; i <= 15; i++) {
            lista.adicionarItem("item" + (char) ('a' + i), TipoPalavra.CANONICA, i);
        }
        lista = listaPalavrasRepository.save(lista);
        Map<String, Object> payload = payload(matricula.getAluno().getId(), "PALAVRA");
        payload.put("listaPalavrasId", lista.getId());

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.palavras.length()").value(15))
                .andExpect(jsonPath("$.palavras[0].palavra").value("itemb"))
                .andExpect(jsonPath("$.palavras[0].ordem").value(1))
                .andExpect(jsonPath("$.palavras[0].status").value("PENDENTE"));
    }

    // ---- AVA-08 (AC 9): texto tokenizado --------------------------------

    @Test
    void postComTextoCurtoRetorna201ComOTextoTokenizado() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(2, professor);
        Map<String, Object> payload = payload(matricula.getAluno().getId(), "TEXTO_CURTO");
        StringBuilder texto = new StringBuilder("O guarda-chuva, caiu!");
        for (int i = 0; i < 17; i++) {
            texto.append(" mais");
        }
        payload.put("texto", texto.toString());

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeTotal").value(20))
                .andExpect(jsonPath("$.palavras[0].palavra").value("O"))
                .andExpect(jsonPath("$.palavras[1].palavra").value("guarda-chuva"))
                .andExpect(jsonPath("$.palavras[2].palavra").value("caiu"))
                .andExpect(jsonPath("$.palavras[2].ordem").value(3));
    }

    // ---- AVA-02: aluno avaliável ----------------------------------------

    @Test
    void postParaAlunoSemMatriculaNoAnoAtivoRetorna422AlunoNaoAvaliavel() throws Exception {
        Professor professor = novoProfessor();
        Aluno semMatricula = alunoRepository.save(new Aluno("Aluno Sem Matricula"));

        postar(bearerProfessor(professor), payloadComPalavras(semMatricula.getId(), palavras(15)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALUNO_NAO_AVALIAVEL"));
    }

    @Test
    void postParaMatriculaComAnoFinalizadoRetorna422AlunoNaoAvaliavel() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        matricula.setAnoFinalizado(true);
        matriculaRepository.save(matricula);

        postar(bearerProfessor(professor), payloadComPalavras(matricula.getAluno().getId(), palavras(15)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALUNO_NAO_AVALIAVEL"));
    }

    @Test
    void postParaAlunoInativoRetorna422AlunoNaoAvaliavel() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Aluno aluno = matricula.getAluno();
        aluno.setAtivo(false);
        alunoRepository.save(aluno);

        postar(bearerProfessor(professor), payloadComPalavras(aluno.getId(), palavras(15)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ALUNO_NAO_AVALIAVEL"));
    }

    // ---- AVA-03: quantidade --------------------------------------------

    @Test
    void postPrimeiroAnoCom14PalavrasRetorna422ComMinimoMaximoEInformado() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);

        postar(bearerProfessor(professor), payloadComPalavras(matricula.getAluno().getId(), palavras(14)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUANTIDADE_PALAVRAS_FORA_DO_LIMITE"))
                .andExpect(jsonPath("$.minimo").value(15))
                .andExpect(jsonPath("$.maximo").value(20))
                .andExpect(jsonPath("$.informado").value(14));
    }

    // ---- AVA-04: fonte de conteúdo -------------------------------------

    @Test
    void postSemFonteDeConteudoRetorna422ConteudoInvalido() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);

        postar(bearerProfessor(professor), payload(matricula.getAluno().getId(), "PALAVRA"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTEUDO_INVALIDO"));
    }

    @Test
    void postComTextoEmTipoPalavraRetorna422ConteudoInvalido() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Map<String, Object> payload = payload(matricula.getAluno().getId(), "PALAVRA");
        payload.put("texto", "o gato pulou o muro");

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTEUDO_INVALIDO"));
    }

    @Test
    void postComListaDeOutraSerieRetorna422ListaIncompativel() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        ListaPalavras lista = new ListaPalavras("Lista Serie 2 " + UUID.randomUUID(), 2, TipoLeituraCodigo.PALAVRA, null, null);
        lista.adicionarItem("gato", TipoPalavra.CANONICA, 1);
        lista = listaPalavrasRepository.save(lista);
        Map<String, Object> payload = payload(matricula.getAluno().getId(), "PALAVRA");
        payload.put("listaPalavrasId", lista.getId());

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LISTA_INCOMPATIVEL"));
    }

    // ---- AVA-05: tempo -------------------------------------------------

    @Test
    void postComTempoForaDe10a600Retorna422() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Map<String, Object> payload = payloadComPalavras(matricula.getAluno().getId(), palavras(15));
        payload.put("tempoSegundos", 601);

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    // ---- AVA-06: data --------------------------------------------------

    @Test
    void postComDataFuturaRetorna422() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Map<String, Object> payload = payloadComPalavras(matricula.getAluno().getId(), palavras(15));
        payload.put("dataAvaliacao", hoje.plusDays(1).toString());

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DATA_AVALIACAO_INVALIDA"));
    }

    @Test
    void postComDataForaDoAnoLetivoAtivoRetorna422() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        Map<String, Object> payload = payloadComPalavras(matricula.getAluno().getId(), palavras(15));
        payload.put("dataAvaliacao", hoje.minusDays(61).toString());

        postar(bearerProfessor(professor), payload)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DATA_AVALIACAO_INVALIDA"));
    }

    // ---- AVA-07: 1º ano sem não canônicas ------------------------------

    @Test
    void postPrimeiroAnoComPalavraNaoCanonicaRetorna422ComPosicoes() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        List<Map<String, Object>> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(3, Map.of("palavra", "blusa", "tipoPalavra", "NAO_CANONICA"));

        postar(bearerProfessor(professor), payloadComPalavras(matricula.getAluno().getId(), digitadas))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NAO_CANONICA_PROIBIDA_1_ANO"))
                .andExpect(jsonPath("$.posicoes[0]").value(4));
    }

    // ---- AVA-08 (AC 10): formato de palavra ----------------------------

    @Test
    void postComPalavraInvalidaRetorna422ComPosicao() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(1, professor);
        List<Map<String, Object>> digitadas = new ArrayList<>(palavras(15));
        digitadas.set(5, Map.of("palavra", "gat0"));

        postar(bearerProfessor(professor), payloadComPalavras(matricula.getAluno().getId(), digitadas))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PALAVRA_INVALIDA"))
                .andExpect(jsonPath("$.posicao").value(6));
    }

    // ---- Edge cases: pertencimento (AUTH-09) ----------------------------

    @Test
    void postParaAlunoDeOutroProfessorRetorna404() throws Exception {
        Matricula matricula = novaMatricula(1, novoProfessor());

        postar(bearerProfessor(novoProfessor()), payloadComPalavras(matricula.getAluno().getId(), palavras(15)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void postParaMatriculaSemProfessorRetorna404() throws Exception {
        Matricula matricula = novaMatricula(1, null);

        postar(bearerProfessor(novoProfessor()), payloadComPalavras(matricula.getAluno().getId(), palavras(15)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    // ---- Autorização / autenticação ------------------------------------

    @Test
    void postComoCoordenadorFunciona() throws Exception {
        Matricula matricula = novaMatricula(1, novoProfessor());

        postar(bearerCoordenador(), payloadComPalavras(matricula.getAluno().getId(), palavras(15)))
                .andExpect(status().isCreated());
    }

    @Test
    void postSemAutenticacaoRetorna401() throws Exception {
        Matricula matricula = novaMatricula(1, novoProfessor());

        mockMvc.perform(post("/api/v1/avaliacoes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                payloadComPalavras(matricula.getAluno().getId(), palavras(15)))))
                .andExpect(status().isUnauthorized());
    }

    // ---- Transições (AVA-09..AVA-14, AVA-17, AVA-25) --------------------

    private Long criarAvaliacao(String bearer, Matricula matricula, int quantidadePalavras) throws Exception {
        MvcResult result = postar(bearer, payloadComPalavras(matricula.getAluno().getId(), palavras(quantidadePalavras)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions acao(String bearer, Long id, String acao) throws Exception {
        return mockMvc.perform(post("/api/v1/avaliacoes/" + id + "/" + acao).header("Authorization", bearer));
    }

    private void alterar(Long id, Consumer<Avaliacao> alteracao) {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                tx -> alteracao.accept(avaliacaoRepository.findById(id).orElseThrow()));
    }

    /** Avaliação de 15 palavras do 1º ano levada ao status pedido, pela própria API em cada passo. */
    private Long avaliacaoNoStatus(String bearer, Professor professor, StatusAvaliacao status) throws Exception {
        Long id = criarAvaliacao(bearer, novaMatricula(1, professor), 15);
        switch (status) {
            case EM_ANDAMENTO -> acao(bearer, id, "iniciar").andExpect(status().isOk());
            case PAUSADA -> {
                acao(bearer, id, "iniciar").andExpect(status().isOk());
                acao(bearer, id, "pausar").andExpect(status().isOk());
            }
            case FINALIZADA -> {
                acao(bearer, id, "iniciar").andExpect(status().isOk());
                acao(bearer, id, "finalizar").andExpect(status().isOk());
            }
            case CANCELADA -> cancelar(bearer, id, "Avaliação criada por engano, cancelando para teste.")
                    .andExpect(status().isOk());
            default -> {
            }
        }
        return id;
    }

    // ---- Cancelamento (AVA-24) -------------------------------------------

    private ResultActions cancelar(String bearer, Long id, String justificativa) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("justificativa", justificativa);
        return mockMvc.perform(post("/api/v1/avaliacoes/" + id + "/cancelar").header("Authorization", bearer)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(corpo)));
    }

    @ParameterizedTest
    @EnumSource(value = StatusAvaliacao.class, names = {"CRIADA", "EM_ANDAMENTO", "PAUSADA", "FINALIZADA"})
    void cancelarAPartirDeQualquerStatusNaoCanceladaRetorna200EGravaAuditoria(StatusAvaliacao origem) throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, origem);

        cancelar(bearer, id, "Avaliação aplicada por engano, cancelando.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertEquals(StatusAvaliacao.CANCELADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
        List<AvaliacaoAuditoria> auditorias = avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id);
        assertEquals(1, auditorias.size());
        assertEquals(AcaoAuditoria.CANCELAMENTO, auditorias.get(0).getAcao());
        assertEquals(origem.name(), auditorias.get(0).getValorAnterior());
        assertEquals("CANCELADA", auditorias.get(0).getValorNovo());
        assertEquals("Avaliação aplicada por engano, cancelando.", auditorias.get(0).getJustificativa());
    }

    @Test
    void cancelarComJustificativaCurtaRetorna422SemAlterar() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.CRIADA);

        cancelar(bearer, id, "curta")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));

        assertEquals(StatusAvaliacao.CRIADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    /** Tabela de status (spec.md): repetir "cancelar" numa já CANCELADA é idempotente (200), não 409. */
    @Test
    void cancelarUmaJaCanceladaRetorna200IdempotenteSemNovaAuditoria() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.CANCELADA);

        cancelar(bearer, id, "Avaliação aplicada por engano, cancelando de novo.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertEquals(1, avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id).size());
    }

    /** AVA-24 (AC 3): depois de cancelada, qualquer OUTRA ação continua 409 (a tabela de status já cobre isso em T16). */
    @Test
    void depoisDeCancelarQualquerOutraAcaoRetorna409TransicaoInvalida() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);

        cancelar(bearer, id, "Avaliação aplicada por engano, cancelando.").andExpect(status().isOk());

        acao(bearer, id, "iniciar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSICAO_INVALIDA"));
    }

    @Test
    void cancelarUmaFinalizadaComAudioMantemORegistroDeAudioRecuperavel() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        Avaliacao avaliacao = avaliacaoRepository.findById(id).orElseThrow();
        avaliacaoAudioRepository.save(new AvaliacaoAudio(avaliacao, "ref-teste.wav", "audio/wav", 1024L));

        cancelar(bearer, id, "Avaliação aplicada por engano, cancelando.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        assertTrue(avaliacaoAudioRepository.findByAvaliacaoId(id).isPresent());
    }

    @Test
    void cancelarAvaliacaoDeOutroProfessorRetorna404SemAlterar() throws Exception {
        Professor dono = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(dono), dono, StatusAvaliacao.CRIADA);

        cancelar(bearerProfessor(novoProfessor()), id, "Avaliação aplicada por engano, cancelando.")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));

        assertEquals(StatusAvaliacao.CRIADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelarComoCoordenadorFunciona() throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.CRIADA);

        cancelar(bearerCoordenador(), id, "Avaliação aplicada por engano, cancelando.")
                .andExpect(status().isOk());

        assertEquals(StatusAvaliacao.CANCELADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/api/v1/avaliacoes/1/cancelar")
                        .contentType("application/json")
                        .content("{\"justificativa\":\"Avaliação aplicada por engano, cancelando.\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** Tabela de status (spec.md), menos a coluna {@code cancelar} (T23): toda célula, com o status gravado depois. */
    @ParameterizedTest(name = "{0} + {1} -> {2} {3}")
    @CsvSource({
            "CRIADA,       iniciar,   200, EM_ANDAMENTO",
            "CRIADA,       pausar,    409, CRIADA",
            "CRIADA,       continuar, 409, CRIADA",
            "CRIADA,       resetar,   409, CRIADA",
            "CRIADA,       finalizar, 409, CRIADA",
            "EM_ANDAMENTO, iniciar,   200, EM_ANDAMENTO",
            "EM_ANDAMENTO, pausar,    200, PAUSADA",
            "EM_ANDAMENTO, continuar, 200, EM_ANDAMENTO",
            "EM_ANDAMENTO, resetar,   200, CRIADA",
            "EM_ANDAMENTO, finalizar, 200, FINALIZADA",
            "PAUSADA,      iniciar,   409, PAUSADA",
            "PAUSADA,      pausar,    200, PAUSADA",
            "PAUSADA,      continuar, 200, EM_ANDAMENTO",
            "PAUSADA,      resetar,   200, CRIADA",
            "PAUSADA,      finalizar, 200, FINALIZADA",
            "FINALIZADA,   iniciar,   409, FINALIZADA",
            "FINALIZADA,   pausar,    409, FINALIZADA",
            "FINALIZADA,   continuar, 409, FINALIZADA",
            "FINALIZADA,   resetar,   409, FINALIZADA",
            "FINALIZADA,   finalizar, 200, FINALIZADA",
            "CANCELADA,    iniciar,   409, CANCELADA",
            "CANCELADA,    pausar,    409, CANCELADA",
            "CANCELADA,    continuar, 409, CANCELADA",
            "CANCELADA,    resetar,   409, CANCELADA",
            "CANCELADA,    finalizar, 409, CANCELADA"
    })
    void tabelaDeStatus(StatusAvaliacao origem, String acao, int httpEsperado, StatusAvaliacao statusEsperado)
            throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, origem);
        Avaliacao antes = avaliacaoRepository.findById(id).orElseThrow();

        ResultActions resposta = acao(bearer, id, acao).andExpect(status().is(httpEsperado));

        if (httpEsperado == 409) {
            resposta.andExpect(jsonPath("$.code").value("TRANSICAO_INVALIDA"))
                    .andExpect(jsonPath("$.statusAtual").value(origem.name()))
                    .andExpect(jsonPath("$.acao").value(acao));
        } else {
            resposta.andExpect(jsonPath("$.status").value(statusEsperado.name()));
        }
        Avaliacao depois = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(statusEsperado, depois.getStatus());
        if (origem == statusEsperado) {
            // 409 e ação idempotente (AVA-14): nada muda.
            assertEquals(antes.getIniciadoEm(), depois.getIniciadoEm());
            assertEquals(antes.getTempoAcumuladoSegundos(), depois.getTempoAcumuladoSegundos());
            assertEquals(antes.getFinalizadoEm(), depois.getFinalizadoEm());
            assertEquals(antes.getTempoUtilizadoSegundos(), depois.getTempoUtilizadoSegundos());
        }
    }

    @Test
    void iniciarRetorna200ComIniciadoEmGravado() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.CRIADA);

        acao(bearer, id, "iniciar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.iniciadoEm").value(notNullValue()));

        assertNotNull(avaliacaoRepository.findById(id).orElseThrow().getIniciadoEm());
    }

    @Test
    void iniciarPausarContinuarEFinalizarNaoContaOTempoPausado() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(avaliacao.getIniciadoEm().minusSeconds(3)));
        acao(bearer, id, "pausar").andExpect(status().isOk());
        acao(bearer, id, "continuar").andExpect(status().isOk());

        acao(bearer, id, "finalizar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.tempoUtilizadoSegundos").value(3))
                .andExpect(jsonPath("$.finalizadoEm").value(notNullValue()));
    }

    @Test
    void resetarVoltaParaCriadaZeraOTempoELimpaIniciadoEmEAsMarcacoes() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> {
            avaliacao.setTempoAcumuladoSegundos(20);
            avaliacao.getPalavras().get(0).setStatus(StatusPalavra.CORRETA);
            avaliacao.getPalavras().get(1).setStatus(StatusPalavra.NAO_LIDA);
        });

        acao(bearer, id, "resetar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andExpect(jsonPath("$.iniciadoEm").value(nullValue()))
                .andExpect(jsonPath("$.palavras[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.palavras[1].status").value("PENDENTE"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(0, gravada.getTempoAcumuladoSegundos());
        assertNull(gravada.getIniciadoEm());
    }

    @Test
    void finalizarExemploDoSdd13ComASeedRealRetornaOResultadoCompleto() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = criarAvaliacao(bearer, novaMatricula(1, professor), 20);
        acao(bearer, id, "iniciar").andExpect(status().isOk());
        alterar(id, avaliacao -> {
            for (int i = 0; i < 20; i++) {
                StatusPalavra status = i < 9 ? StatusPalavra.CORRETA
                        : i < 13 ? StatusPalavra.INCORRETA
                        : i < 16 ? StatusPalavra.NAO_LIDA
                        : StatusPalavra.PENDENTE;
                avaliacao.getPalavras().get(i).setStatus(status);
            }
        });

        acao(bearer, id, "finalizar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.quantidadeTotal").value(20))
                .andExpect(jsonPath("$.quantidadeCorretas").value(9))
                .andExpect(jsonPath("$.quantidadeIncorretas").value(4))
                .andExpect(jsonPath("$.quantidadeNaoLidas").value(7))
                .andExpect(jsonPath("$.quantidadeLidas").value(13))
                .andExpect(jsonPath("$.percentualAcerto").value(45.00))
                .andExpect(jsonPath("$.fase").value("LEITOR_INICIANTE"))
                .andExpect(jsonPath("$.nivel").value(nullValue()))
                .andExpect(jsonPath("$.classificacaoPendente").value(false))
                .andExpect(jsonPath("$.palavras[19].status").value("NAO_LIDA"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(0, new BigDecimal("45.00").compareTo(gravada.getPercentualAcerto()));
        assertEquals(2, gravada.getPercentualAcerto().scale());
        assertEquals(7, gravada.getQuantidadeNaoLidas());
    }

    @Test
    void finalizarComTempoJaEsgotadoRetorna200ComTempoUtilizadoIgualAoConfigurado() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        acao(bearer, id, "finalizar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.tempoUtilizadoSegundos").value(60));
    }

    @Test
    void pausarComTempoJaEsgotadoFinalizaEGravaAntesDeResponder409() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        acao(bearer, id, "pausar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSICAO_INVALIDA"))
                .andExpect(jsonPath("$.statusAtual").value("FINALIZADA"))
                .andExpect(jsonPath("$.acao").value("pausar"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(StatusAvaliacao.FINALIZADA, gravada.getStatus());
        assertEquals(60, gravada.getTempoUtilizadoSegundos());
        assertEquals(15, gravada.getQuantidadeNaoLidas());
    }

    @ParameterizedTest
    @ValueSource(strings = {"iniciar", "pausar", "continuar", "resetar", "finalizar"})
    void transicaoEmAvaliacaoDeOutroProfessorRetorna404SemAlterar(String acao) throws Exception {
        Professor dono = novoProfessor();
        String bearerDono = bearerProfessor(dono);
        Long id = avaliacaoNoStatus(bearerDono, dono, StatusAvaliacao.EM_ANDAMENTO);

        acao(bearerProfessor(novoProfessor()), id, acao)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));

        assertEquals(StatusAvaliacao.EM_ANDAMENTO, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void transicaoEmAvaliacaoInexistenteRetorna404() throws Exception {
        acao(bearerProfessor(novoProfessor()), Long.MAX_VALUE, "iniciar")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void iniciarComoCoordenadorFunciona() throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.CRIADA);

        acao(bearerCoordenador(), id, "iniciar").andExpect(status().isOk());

        assertEquals(StatusAvaliacao.EM_ANDAMENTO, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void refazerCriaNovaAvaliacaoComAsMesmasPalavras() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        MvcResult result = acao(bearer, id, "refazer").andExpect(status().isCreated()).andReturn();
        Long novoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        assertNotEquals(id, novoId);
        assertEquals(StatusAvaliacao.CRIADA, avaliacaoRepository.findById(novoId).orElseThrow().getStatus());
        assertEquals(false, avaliacaoRepository.findById(id).orElseThrow().isAtiva());
        assertEquals(true, avaliacaoRepository.findById(novoId).orElseThrow().isAtiva());
    }

    @Test
    void refazerDeNovoInativaAnteriorEMantemUmaAtiva() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        MvcResult r1 = acao(bearer, id, "refazer").andExpect(status().isCreated()).andReturn();
        Long primeiraCopia = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();

        MvcResult r2 = acao(bearer, id, "refazer").andExpect(status().isCreated()).andReturn();
        Long segundaCopia = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        assertEquals(false, avaliacaoRepository.findById(id).orElseThrow().isAtiva());
        assertEquals(false, avaliacaoRepository.findById(primeiraCopia).orElseThrow().isAtiva());
        assertEquals(true, avaliacaoRepository.findById(segundaCopia).orElseThrow().isAtiva());
    }

    @Test
    void refazerAvaliacaoNaoFinalizadaRetorna409() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.CRIADA);

        acao(bearer, id, "refazer").andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"iniciar", "pausar", "continuar", "resetar", "finalizar"})
    void transicaoSemAutenticacaoRetorna401(String acao) throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.CRIADA);

        mockMvc.perform(post("/api/v1/avaliacoes/" + id + "/" + acao)).andExpect(status().isUnauthorized());
    }

    /**
     * AVA-25: as duas requisições leem a mesma versão (a barreira segura as
     * duas logo depois da leitura, dentro da transação) e só uma consegue
     * gravar; a outra recebe 409 {@code CONFLITO_DE_VERSAO}.
     */
    @Test
    void duasTransicoesConcorrentesNaMesmaAvaliacaoUmaRecebe200EAOutra409ConflitoDeVersao() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            barrier.await(10, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(pertencimentoProfessorGuard).verificar(any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MockHttpServletResponse> primeira = executor.submit(() -> acao(bearer, id, "pausar").andReturn().getResponse());
            Future<MockHttpServletResponse> segunda = executor.submit(() -> acao(bearer, id, "pausar").andReturn().getResponse());
            MockHttpServletResponse resposta1 = primeira.get(30, TimeUnit.SECONDS);
            MockHttpServletResponse resposta2 = segunda.get(30, TimeUnit.SECONDS);

            List<Integer> statuses = List.of(resposta1.getStatus(), resposta2.getStatus());
            assertTrue(statuses.contains(200), "esperava um 200, recebeu: " + statuses);
            assertTrue(statuses.contains(409), "esperava um 409, recebeu: " + statuses);
            MockHttpServletResponse conflito = resposta1.getStatus() == 409 ? resposta1 : resposta2;
            assertEquals("CONFLITO_DE_VERSAO", objectMapper.readTree(conflito.getContentAsString()).get("code").asText());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(StatusAvaliacao.PAUSADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    // ---- Marcação de palavras (AVA-15, AVA-18, AVA-19) -------------------

    private ResultActions marcar(String bearer, Long id, int ordem, Object status) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("status", status);
        return mockMvc.perform(put("/api/v1/avaliacoes/" + id + "/palavras/" + ordem).header("Authorization", bearer)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private ResultActions marcarLote(String bearer, Long id, List<?> itens) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("itens", itens);
        return mockMvc.perform(put("/api/v1/avaliacoes/" + id + "/palavras").header("Authorization", bearer)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private List<StatusPalavra> statusGravados(Long id) {
        return new TransactionTemplate(transactionManager).execute(tx -> avaliacaoRepository.findById(id).orElseThrow()
                .getPalavras().stream().map(PalavraAvaliacao::getStatus).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_ANDAMENTO", "PAUSADA"})
    void putPalavraEmAndamentoOuPausadaRetorna200ComOStatusGravado(StatusAvaliacao status) throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, status);

        marcar(bearer, id, 2, "CORRETA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()))
                .andExpect(jsonPath("$.palavras[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.palavras[1].status").value("CORRETA"));

        assertEquals(StatusPalavra.CORRETA, statusGravados(id).get(1));
        assertEquals(StatusPalavra.PENDENTE, statusGravados(id).get(0));
        assertTrue(avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id).isEmpty());
    }

    @Test
    void putPalavrasEmLoteRetorna200ComTodosOsItensGravados() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);

        marcarLote(bearer, id, List.of(
                        Map.of("ordem", 1, "status", "CORRETA"),
                        Map.of("ordem", 2, "status", "INCORRETA"),
                        Map.of("ordem", 3, "status", "NAO_LIDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.palavras[0].status").value("CORRETA"))
                .andExpect(jsonPath("$.palavras[1].status").value("INCORRETA"))
                .andExpect(jsonPath("$.palavras[2].status").value("NAO_LIDA"));

        assertEquals(List.of(StatusPalavra.CORRETA, StatusPalavra.INCORRETA, StatusPalavra.NAO_LIDA),
                statusGravados(id).subList(0, 3));
    }

    @Test
    void putPalavrasEmLoteComUmaOrdemInexistenteRetorna404ENaoGravaNenhum() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);

        marcarLote(bearer, id, List.of(
                        Map.of("ordem", 1, "status", "CORRETA"),
                        Map.of("ordem", 99, "status", "CORRETA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.ordem").value(99));

        assertTrue(statusGravados(id).stream().allMatch(status -> status == StatusPalavra.PENDENTE));
    }

    @Test
    void putPalavrasEmLoteNumaFinalizadaComUmPendenteRetorna422ENaoGravaNemAudita() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        marcarLote(bearer, id, List.of(
                        Map.of("ordem", 1, "status", "CORRETA"),
                        Map.of("ordem", 2, "status", "PENDENTE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STATUS_PALAVRA_INVALIDO"));

        assertEquals(StatusPalavra.NAO_LIDA, statusGravados(id).get(0));
        assertEquals(0, avaliacaoRepository.findById(id).orElseThrow().getQuantidadeCorretas());
        assertTrue(avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id).isEmpty());
    }

    @Test
    void putPalavraComOrdemInexistenteRetorna404() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);

        marcar(bearer, id, 16, "CORRETA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CRIADA", "CANCELADA"})
    void putPalavraEmCriadaOuCanceladaRetorna409MarcacaoNaoPermitida(StatusAvaliacao status) throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, status);

        marcar(bearer, id, 1, "CORRETA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MARCACAO_NAO_PERMITIDA"))
                .andExpect(jsonPath("$.statusAtual").value(status.name()));

        assertEquals(StatusPalavra.PENDENTE, statusGravados(id).get(0));
    }

    @Test
    void putPalavraPendenteNumaFinalizadaRetorna422() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        marcar(bearer, id, 1, "PENDENTE")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("STATUS_PALAVRA_INVALIDO"));

        assertEquals(StatusPalavra.NAO_LIDA, statusGravados(id).get(0));
    }

    @Test
    void putPalavraNumaFinalizadaDeNaoLidaParaCorretaAumentaCorretasEGeraUmaAuditoria() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        marcar(bearer, id, 3, "CORRETA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.palavras[2].status").value("CORRETA"))
                .andExpect(jsonPath("$.quantidadeCorretas").value(1))
                .andExpect(jsonPath("$.quantidadeNaoLidas").value(14));

        List<AvaliacaoAuditoria> auditorias = avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id);
        assertEquals(1, auditorias.size());
        assertEquals(AcaoAuditoria.MARCACAO_PALAVRA, auditorias.get(0).getAcao());
        assertEquals("palavra 3: NAO_LIDA", auditorias.get(0).getValorAnterior());
        assertEquals("palavra 3: CORRETA", auditorias.get(0).getValorNovo());
        assertNotNull(auditorias.get(0).getDataHora());
        assertEquals(1, avaliacaoRepository.findById(id).orElseThrow().getQuantidadeCorretas());
    }

    @Test
    void putPalavraComOStatusAtualNumaFinalizadaRetorna200SemAuditoria() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        marcar(bearer, id, 3, "NAO_LIDA").andExpect(status().isOk());

        assertTrue(avaliacaoAuditoriaRepository.findByAvaliacaoIdOrderByDataHoraAsc(id).isEmpty());
    }

    @Test
    void putPalavraSemStatusRetorna422() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);

        marcar(bearer, id, 1, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void putPalavrasComItemNuloRetorna422() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        List<Object> itens = new ArrayList<>();
        itens.add(null);

        marcarLote(bearer, id, itens)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void putPalavrasEmAvaliacaoDeOutroProfessorRetorna404NasDuasRotas() throws Exception {
        Professor dono = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(dono), dono, StatusAvaliacao.EM_ANDAMENTO);
        String outro = bearerProfessor(novoProfessor());

        marcar(outro, id, 1, "CORRETA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        marcarLote(outro, id, List.of(Map.of("ordem", 1, "status", "CORRETA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));

        assertEquals(StatusPalavra.PENDENTE, statusGravados(id).get(0));
    }

    @Test
    void putPalavrasComoCoordenadorFuncionaNasDuasRotas() throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.EM_ANDAMENTO);

        marcar(bearerCoordenador(), id, 1, "CORRETA").andExpect(status().isOk());
        marcarLote(bearerCoordenador(), id, List.of(Map.of("ordem", 1, "status", "CORRETA")))
                .andExpect(status().isOk());

        assertEquals(StatusPalavra.CORRETA, statusGravados(id).get(0));
    }

    @Test
    void putPalavrasSemAutenticacaoRetorna401NasDuasRotas() throws Exception {
        mockMvc.perform(put("/api/v1/avaliacoes/1/palavras/1").contentType("application/json").content("{\"status\":\"CORRETA\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/avaliacoes/1/palavras").contentType("application/json").content("{\"itens\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void marcarPalavraComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaOrdemInexistente() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        marcar(bearer, id, 999, "CORRETA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(StatusAvaliacao.FINALIZADA, gravada.getStatus());
        assertEquals(60, gravada.getTempoUtilizadoSegundos());
        assertEquals(15, gravada.getQuantidadeNaoLidas());
    }

    @Test
    void marcarPalavrasEmLoteComTempoJaEsgotadoFinalizaEGravaAntesDeResponder404ParaItemInvalido() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        marcarLote(bearer, id, List.of(Map.of("ordem", 999, "status", "CORRETA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(StatusAvaliacao.FINALIZADA, gravada.getStatus());
        assertEquals(60, gravada.getTempoUtilizadoSegundos());
        assertEquals(15, gravada.getQuantidadeNaoLidas());
    }

    // ---- Consultas (AVA-23, AVA-26) --------------------------------------

    private ResultActions buscar(String bearer, Long id) throws Exception {
        return mockMvc.perform(get("/api/v1/avaliacoes/" + id).header("Authorization", bearer));
    }

    private ResultActions auditoria(String bearer, Long id) throws Exception {
        return mockMvc.perform(get("/api/v1/avaliacoes/" + id + "/auditoria").header("Authorization", bearer));
    }

    @Test
    void getDeAvaliacaoNaoFinalizadaRetornaConfiguracaoCopiasEPalavrasSemResultado() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Matricula matricula = novaMatricula(1, professor);
        Long id = criarAvaliacao(bearer, matricula, 15);
        acao(bearer, id, "iniciar").andExpect(status().isOk());
        marcar(bearer, id, 1, "CORRETA").andExpect(status().isOk());

        buscar(bearer, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.alunoId").value(matricula.getAluno().getId()))
                .andExpect(jsonPath("$.professorId").value(professor.getId()))
                .andExpect(jsonPath("$.professorNome").value(professor.getNome()))
                .andExpect(jsonPath("$.turmaId").value(matricula.getTurma().getId()))
                .andExpect(jsonPath("$.turmaNome").value(matricula.getTurma().getNome()))
                .andExpect(jsonPath("$.serie").value(1))
                .andExpect(jsonPath("$.anoLetivoId").value(anoLetivo.getId()))
                .andExpect(jsonPath("$.cicloId").value(cicloId))
                .andExpect(jsonPath("$.tipoLeitura").value("PALAVRA"))
                .andExpect(jsonPath("$.dataAvaliacao").value(hoje.toString()))
                .andExpect(jsonPath("$.tempoConfiguradoSegundos").value(60))
                .andExpect(jsonPath("$.quantidadeTotal").value(15))
                .andExpect(jsonPath("$.iniciadoEm").value(notNullValue()))
                .andExpect(jsonPath("$.palavras.length()").value(15))
                .andExpect(jsonPath("$.palavras[0].ordem").value(1))
                .andExpect(jsonPath("$.palavras[0].palavra").value("palavraa"))
                .andExpect(jsonPath("$.palavras[0].status").value("CORRETA"))
                .andExpect(jsonPath("$.palavras[1].status").value("PENDENTE"))
                .andExpect(jsonPath("$.finalizadoEm").value(nullValue()))
                .andExpect(jsonPath("$.tempoUtilizadoSegundos").value(nullValue()))
                .andExpect(jsonPath("$.quantidadeCorretas").value(nullValue()))
                .andExpect(jsonPath("$.quantidadeIncorretas").value(nullValue()))
                .andExpect(jsonPath("$.quantidadeNaoLidas").value(nullValue()))
                .andExpect(jsonPath("$.quantidadeLidas").value(nullValue()))
                .andExpect(jsonPath("$.percentualAcerto").value(nullValue()))
                .andExpect(jsonPath("$.fase").value(nullValue()))
                .andExpect(jsonPath("$.nivel").value(nullValue()))
                .andExpect(jsonPath("$.classificacaoPendente").value(false));
    }

    @Test
    void getDeAvaliacaoFinalizadaRetornaOResultadoCompleto() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        marcarLote(bearer, id, List.of(
                        Map.of("ordem", 1, "status", "CORRETA"),
                        Map.of("ordem", 2, "status", "CORRETA"),
                        Map.of("ordem", 3, "status", "INCORRETA")))
                .andExpect(status().isOk());
        acao(bearer, id, "finalizar").andExpect(status().isOk());

        buscar(bearer, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.finalizadoEm").value(notNullValue()))
                .andExpect(jsonPath("$.tempoUtilizadoSegundos").value(notNullValue()))
                .andExpect(jsonPath("$.quantidadeTotal").value(15))
                .andExpect(jsonPath("$.quantidadeCorretas").value(2))
                .andExpect(jsonPath("$.quantidadeIncorretas").value(1))
                .andExpect(jsonPath("$.quantidadeNaoLidas").value(12))
                .andExpect(jsonPath("$.quantidadeLidas").value(3))
                .andExpect(jsonPath("$.percentualAcerto").value(13.33))
                .andExpect(jsonPath("$.fase").value("PRE_LEITOR"))
                .andExpect(jsonPath("$.nivel").value(1))
                .andExpect(jsonPath("$.classificacaoPendente").value(false))
                .andExpect(jsonPath("$.palavras[3].status").value("NAO_LIDA"));
    }

    @Test
    void getDeAvaliacaoEmAndamentoComTempoEsgotadoRetornaEGravaFinalizada() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(75)));

        buscar(bearer, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.tempoUtilizadoSegundos").value(60))
                .andExpect(jsonPath("$.quantidadeNaoLidas").value(15));

        assertEquals(StatusAvaliacao.FINALIZADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void getComoCoordenadorRetornaAvaliacaoDeQualquerProfessor() throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.CRIADA);

        buscar(bearerCoordenador(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.professorId").value(professor.getId()));
    }

    @Test
    void getAuditoriaDepoisDeDuasAlteracoesRetornaOsDoisRegistrosEmOrdemCronologica() throws Exception {
        Professor professor = novoProfessor();
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-aval-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        String bearer = "Bearer " + jwtService.emitir(usuario.getId());
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        marcar(bearer, id, 5, "CORRETA").andExpect(status().isOk());
        marcar(bearer, id, 3, "INCORRETA").andExpect(status().isOk());

        auditoria(bearer, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].usuario").value(usuario.getId()))
                .andExpect(jsonPath("$[0].dataHora").value(notNullValue()))
                .andExpect(jsonPath("$[0].acao").value("MARCACAO_PALAVRA"))
                .andExpect(jsonPath("$[0].valorAnterior").value("palavra 5: NAO_LIDA"))
                .andExpect(jsonPath("$[0].valorNovo").value("palavra 5: CORRETA"))
                .andExpect(jsonPath("$[0].justificativa").value(nullValue()))
                .andExpect(jsonPath("$[1].usuario").value(usuario.getId()))
                .andExpect(jsonPath("$[1].acao").value("MARCACAO_PALAVRA"))
                .andExpect(jsonPath("$[1].valorAnterior").value("palavra 3: NAO_LIDA"))
                .andExpect(jsonPath("$[1].valorNovo").value("palavra 3: INCORRETA"));
    }

    @Test
    void getAuditoriaComoCoordenadorRetornaOsRegistros() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        marcar(bearer, id, 1, "CORRETA").andExpect(status().isOk());

        auditoria(bearerCoordenador(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorNovo").value("palavra 1: CORRETA"));
    }

    @Test
    void getDeAvaliacaoDeOutroProfessorRetorna404NasDuasRotas() throws Exception {
        Professor dono = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(dono), dono, StatusAvaliacao.FINALIZADA);
        String outro = bearerProfessor(novoProfessor());

        buscar(outro, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        auditoria(outro, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void getDeAvaliacaoInexistenteRetorna404NasDuasRotas() throws Exception {
        buscar(bearerCoordenador(), Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        auditoria(bearerCoordenador(), Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void getSemAutenticacaoRetorna401NasDuasRotas() throws Exception {
        mockMvc.perform(get("/api/v1/avaliacoes/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/avaliacoes/1/auditoria")).andExpect(status().isUnauthorized());
    }

    // ---- Áudio (AVA-27..AVA-32) -------------------------------------------

    private ResultActions enviarAudio(String bearer, Long id, byte[] conteudo, String mimeType) throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("audio", "gravacao.wav", mimeType, conteudo);
        return mockMvc.perform(multipart("/api/v1/avaliacoes/" + id + "/audio")
                .file(arquivo)
                .header("Authorization", bearer));
    }

    private ResultActions baixarAudio(String bearer, Long id) throws Exception {
        return mockMvc.perform(get("/api/v1/avaliacoes/" + id + "/audio").header("Authorization", bearer));
    }

    @Test
    void enviarAudioNumaFinalizadaRetorna201EDownloadDevolveOsMesmosBytesComOContentTypeCorreto() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        byte[] conteudo = "conteudo-de-audio-fake".getBytes();

        enviarAudio(bearer, id, conteudo, "audio/wav").andExpect(status().isCreated());

        baixarAudio(bearer, id)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/wav"))
                .andExpect(content().bytes(conteudo));
    }

    @Test
    void enviarAudioDuasVezesNaMesmaAvaliacaoRetorna409NoSegundoEnvio() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        enviarAudio(bearer, id, "primeiro".getBytes(), "audio/wav").andExpect(status().isCreated());

        enviarAudio(bearer, id, "segundo".getBytes(), "audio/wav")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUDIO_JA_ENVIADO"));
    }

    @Test
    void enviarAudioForaDeFinalizadaRetorna409AudioEnvioNaoPermitido() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.CRIADA);

        enviarAudio(bearer, id, "conteudo".getBytes(), "audio/wav")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUDIO_ENVIO_NAO_PERMITIDO"))
                .andExpect(jsonPath("$.statusAtual").value("CRIADA"));
    }

    @Test
    void enviarAudioComMimeTypeNaoPermitidoRetorna422AudioFormatoInvalido() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        enviarAudio(bearer, id, "conteudo".getBytes(), "text/plain")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AUDIO_FORMATO_INVALIDO"));
    }

    @Test
    void enviarAudioComTempoJaEsgotadoFinalizaAntesEAceitaOEnvio() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        enviarAudio(bearer, id, "conteudo-de-audio-fake".getBytes(), "audio/wav")
                .andExpect(status().isCreated());

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(StatusAvaliacao.FINALIZADA, gravada.getStatus());
        assertEquals(60, gravada.getTempoUtilizadoSegundos());
    }

    @Test
    void enviarAudioComTempoJaEsgotadoFinalizaEGravaAntesDeResponder422ParaFormatoInvalido() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.EM_ANDAMENTO);
        alterar(id, avaliacao -> avaliacao.setIniciadoEm(Instant.now().minusSeconds(90)));

        enviarAudio(bearer, id, "conteudo".getBytes(), "text/plain")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AUDIO_FORMATO_INVALIDO"));

        Avaliacao gravada = avaliacaoRepository.findById(id).orElseThrow();
        assertEquals(StatusAvaliacao.FINALIZADA, gravada.getStatus());
        assertEquals(60, gravada.getTempoUtilizadoSegundos());
    }

    @Test
    void baixarAudioSemAudioGravadoRetorna404() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);

        baixarAudio(bearer, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void audioDeAvaliacaoDeOutroProfessorRetorna404NasDuasRotas() throws Exception {
        Professor dono = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(dono), dono, StatusAvaliacao.FINALIZADA);
        String outro = bearerProfessor(novoProfessor());

        enviarAudio(outro, id, "conteudo".getBytes(), "audio/wav")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
        baixarAudio(outro, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void baixarAudioComoCoordenadorRetornaOsBytes() throws Exception {
        Professor professor = novoProfessor();
        String bearer = bearerProfessor(professor);
        Long id = avaliacaoNoStatus(bearer, professor, StatusAvaliacao.FINALIZADA);
        byte[] conteudo = "conteudo".getBytes();
        enviarAudio(bearer, id, conteudo, "audio/wav").andExpect(status().isCreated());

        baixarAudio(bearerCoordenador(), id)
                .andExpect(status().isOk())
                .andExpect(content().bytes(conteudo));
    }

    @Test
    void enviarAudioComoCoordenadorFunciona() throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.FINALIZADA);

        enviarAudio(bearerCoordenador(), id, "conteudo".getBytes(), "audio/wav")
                .andExpect(status().isCreated());
    }

    @Test
    void audioSemAutenticacaoRetorna401NasDuasRotas() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("audio", "gravacao.wav", "audio/wav", "conteudo".getBytes());
        mockMvc.perform(multipart("/api/v1/avaliacoes/1/audio").file(arquivo)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/avaliacoes/1/audio")).andExpect(status().isUnauthorized());
    }
}

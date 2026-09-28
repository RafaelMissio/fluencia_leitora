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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AVA-01..AVA-08: {@code POST /api/v1/avaliacoes} contra MySQL real -
 * caminho feliz (palavras digitadas, lista e texto), um teste por regra de
 * 422 (lição L-016), pertencimento (AUTH-09: 404), autorização (só
 * PROFESSOR cria: 403) e autenticação (401). AVA-09..AVA-14, AVA-17,
 * AVA-25: rotas de transição - tabela de status inteira, efeitos de tempo e
 * reset, exemplo do SDD §13 com a seed real, finalização preguiçosa e
 * conflito de versão.
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
    void postComoCoordenadorRetorna403() throws Exception {
        Matricula matricula = novaMatricula(1, novoProfessor());

        postar(bearerCoordenador(), payloadComPalavras(matricula.getAluno().getId(), palavras(15)))
                .andExpect(status().isForbidden());
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

    /** Avaliação de 15 palavras do 1º ano levada ao status pedido pela própria API (CANCELADA direto no banco - T22 ainda não existe). */
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
            case CANCELADA -> alterar(id, avaliacao -> avaliacao.setStatus(StatusAvaliacao.CANCELADA));
            default -> {
            }
        }
        return id;
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

    @ParameterizedTest
    @ValueSource(strings = {"iniciar", "pausar", "continuar", "resetar", "finalizar"})
    void transicaoComoCoordenadorRetorna403(String acao) throws Exception {
        Professor professor = novoProfessor();
        Long id = avaliacaoNoStatus(bearerProfessor(professor), professor, StatusAvaliacao.CRIADA);

        acao(bearerCoordenador(), id, acao).andExpect(status().isForbidden());

        assertEquals(StatusAvaliacao.CRIADA, avaliacaoRepository.findById(id).orElseThrow().getStatus());
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
}

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
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AVA-01..AVA-08: {@code POST /api/v1/avaliacoes} contra MySQL real -
 * caminho feliz (palavras digitadas, lista e texto), um teste por regra de
 * 422 (lição L-016), pertencimento (AUTH-09: 404), autorização (só
 * PROFESSOR cria: 403) e autenticação (401).
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
}

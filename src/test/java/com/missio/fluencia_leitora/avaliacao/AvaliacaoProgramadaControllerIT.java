package com.missio.fluencia_leitora.avaliacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O coordenador configura a avaliação por série; ela aparece como pendente
 * para os alunos da série (e só deles) e o professor a aplica ao aluno.
 */
@AutoConfigureMockMvc
class AvaliacaoProgramadaControllerIT extends IntegrationTestBase {

    private static int anoSequencial = 2900;

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
    private CicloRepository cicloRepository;

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

    private String bearerProfessor(Professor professor) {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-prog-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    private Professor novoProfessor() {
        return professorRepository.save(new Professor("Professora " + UUID.randomUUID()));
    }

    private Matricula novaMatricula(int serie, Professor professor) {
        Turma turma = turmaRepository.save(new Turma("Turma Prog " + UUID.randomUUID(), serie, anoLetivo, professor));
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Programada"));
        return matriculaRepository.save(new Matricula(aluno, anoLetivo, turma, serie, professor));
    }

    private static String vintePalavras() {
        StringBuilder palavras = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            palavras.append("palavra").append((char) ('a' + i)).append(' ');
        }
        return palavras.toString().trim();
    }

    private Map<String, Object> programada(int serie) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", "Avaliação " + serie);
        payload.put("serie", serie);
        payload.put("tipoLeitura", "PALAVRA");
        payload.put("cicloId", cicloId);
        payload.put("tempoSegundos", 45);
        payload.put("palavras", vintePalavras());
        return payload;
    }

    private ResultActions cadastrar(String bearer, Map<String, Object> payload) throws Exception {
        return mockMvc.perform(post("/api/v1/avaliacoes-programadas").header("Authorization", bearer)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)));
    }

    private ResultActions pendentes(String bearer, Long alunoId) throws Exception {
        return mockMvc.perform(get("/api/v1/avaliacoes/pendentes").param("alunoId", alunoId.toString())
                .header("Authorization", bearer));
    }

    @Test
    void avaliacaoDaSerieApareceSoParaOsAlunosDaquelaSerie() throws Exception {
        Professor professor = novoProfessor();
        Matricula segundoAno = novaMatricula(2, professor);
        Matricula terceiroAno = novaMatricula(3, professor);
        String prof = bearerProfessor(professor);

        cadastrar(bearerCoordenador(), programada(2)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.serie").value(2));

        pendentes(prof, segundoAno.getAluno().getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].avaliacaoId").doesNotExist())
                .andExpect(jsonPath("$[0].tempoSegundos").value(45));
        pendentes(prof, terceiroAno.getAluno().getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void professorAplicaAoAlunoECriaAAvaliacaoComOConteudoConfigurado() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(2, professor);
        Long alunoId = matricula.getAluno().getId();
        String prof = bearerProfessor(professor);
        String respostaCriada = cadastrar(bearerCoordenador(), programada(2)).andReturn().getResponse().getContentAsString();
        long programadaId = objectMapper.readTree(respostaCriada).get("id").asLong();

        String corpo = objectMapper.writeValueAsString(Map.of("alunoId", alunoId));
        mockMvc.perform(post("/api/v1/avaliacoes-programadas/{id}/aplicar", programadaId)
                        .header("Authorization", prof).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andExpect(jsonPath("$.quantidadeTotal").value(20))
                .andExpect(jsonPath("$.tempoConfiguradoSegundos").value(45));
        // repetir devolve a mesma, sem duplicar
        mockMvc.perform(post("/api/v1/avaliacoes-programadas/{id}/aplicar", programadaId)
                        .header("Authorization", prof).contentType("application/json").content(corpo))
                .andExpect(status().isOk());

        pendentes(prof, alunoId).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programadaId").value(programadaId))
                .andExpect(jsonPath("$[0].status").value("CRIADA"));
    }

    @Test
    void alunoDeOutraSerieNaoPodeReceberAAvaliacao() throws Exception {
        Professor professor = novoProfessor();
        Matricula terceiroAno = novaMatricula(3, professor);
        String respostaCriada = cadastrar(bearerCoordenador(), programada(2)).andReturn().getResponse().getContentAsString();
        long programadaId = objectMapper.readTree(respostaCriada).get("id").asLong();

        mockMvc.perform(post("/api/v1/avaliacoes-programadas/{id}/aplicar", programadaId)
                        .header("Authorization", bearerProfessor(professor)).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("alunoId", terceiroAno.getAluno().getId()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void desativarRemoveDasPendenciasDosAlunos() throws Exception {
        Professor professor = novoProfessor();
        Matricula matricula = novaMatricula(2, professor);
        String coordenador = bearerCoordenador();
        String respostaCriada = cadastrar(coordenador, programada(2)).andReturn().getResponse().getContentAsString();
        long programadaId = objectMapper.readTree(respostaCriada).get("id").asLong();

        mockMvc.perform(delete("/api/v1/avaliacoes-programadas/{id}", programadaId).header("Authorization", coordenador))
                .andExpect(status().isNoContent());

        pendentes(bearerProfessor(professor), matricula.getAluno().getId())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void somenteOCoordenadorCadastraEquantidadeForaDoLimiteE422() throws Exception {
        Professor professor = novoProfessor();
        cadastrar(bearerProfessor(professor), programada(2)).andExpect(status().isForbidden());

        Map<String, Object> poucas = programada(2);
        poucas.put("palavras", "a b c");
        cadastrar(bearerCoordenador(), poucas).andExpect(status().isUnprocessableEntity());
    }
}

package com.missio.fluencia_leitora.cadastros.aluno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-11/CAD-15/CAD-16/CAD-19: exercises the whole AlunoController surface
 * against real MySQL.
 */
@AutoConfigureMockMvc
class AlunoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @MockitoBean
    private HistoricoAvaliacaoPort historicoAvaliacaoPort;

    // static: shared, never-rolled-back database across every @Test method,
    // same rationale as AnoLetivoControllerIT.anoSequencial. Kept within
    // [2000, 2100] (CriarAnoLetivoRequest's @Min/@Max) and away from
    // AnoLetivoControllerIT (starts 2050) and TurmaControllerIT (starts 2090).
    private static int anoSequencial = 2065;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    // SPEC_DEVIATION: ativar um ano letivo encerra qualquer outro que
    // estivesse ATIVO (CAD-04, único ATIVO por vez) - por isso um teste que
    // precisa de duas turmas simultaneamente ativas (ex.: escopo por
    // professor, CAD-16) cria UM ano letivo ativo e duas turmas dentro dele,
    // em vez de chamar novoAnoLetivoAtivo() duas vezes.
    private Long novoAnoLetivoAtivo() throws Exception {
        int ano = proximoAno();
        MvcResult anoResult = mockMvc.perform(post("/api/v1/anos-letivos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ano", ano,
                                "dataInicio", "%d-02-01".formatted(ano),
                                "dataFim", "%d-12-15".formatted(ano)))))
                .andExpect(status().isCreated())
                .andReturn();
        Long anoLetivoId = objectMapper.readTree(anoResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/v1/anos-letivos/" + anoLetivoId + "/ativar")).andExpect(status().isOk());
        return anoLetivoId;
    }

    private Long novaTurma(Long anoLetivoId, Long professorId) throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "nome", "Turma " + anoLetivoId + "-" + professorId, "serie", 3, "anoLetivoId", anoLetivoId));
        payload.put("professorId", professorId);
        MvcResult turmaResult = mockMvc.perform(post("/api/v1/turmas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(turmaResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long novaTurmaAtiva(Long professorId) throws Exception {
        return novaTurma(novoAnoLetivoAtivo(), professorId);
    }

    private Long novoProfessor(String nome) {
        return professorRepository.save(new Professor(nome)).getId();
    }

    private Long criarAluno(String nome, Long turmaId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/alunos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome, "turmaId", turmaId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("alunoId").asLong();
    }

    @Test
    void postCriaAlunoComMatriculaComSucesso() throws Exception {
        Long professorId = novoProfessor("Professor Criacao");
        Long turmaId = novaTurmaAtiva(professorId);

        mockMvc.perform(post("/api/v1/alunos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Aluno Criado", "turmaId", turmaId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alunoId").isNumber())
                .andExpect(jsonPath("$.matriculaId").isNumber());
    }

    @Test
    void getBuscaPorNomeEncontraJoaoSemIncluirJoanaEIncluiCamposDaMatriculaAtiva() throws Exception {
        Long professorId = novoProfessor("Professor Busca");
        Long turmaId = novaTurmaAtiva(professorId);
        criarAluno("João Busca Silva", turmaId);
        criarAluno("Joana Busca", turmaId);

        mockMvc.perform(get("/api/v1/alunos").param("nome", "joao busca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("João Busca Silva"))
                .andExpect(jsonPath("$.content[0].turma").exists())
                .andExpect(jsonPath("$.content[0].serie").value(3))
                .andExpect(jsonPath("$.content[0].professor").exists())
                .andExpect(jsonPath("$.content[0].anoLetivo").isNumber())
                .andExpect(jsonPath("$.content[0].situacao").value("EM_ANDAMENTO"));
    }

    @Test
    void getComTermoCurtoRetorna422() throws Exception {
        mockMvc.perform(get("/api/v1/alunos").param("nome", "a")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getComPerfilProfessorRetornaSoAlunosDaqueleProfessor() throws Exception {
        Long professorA = novoProfessor("Professor Escopo A");
        Long professorB = novoProfessor("Professor Escopo B");
        Long anoLetivoAtivo = novoAnoLetivoAtivo();
        Long turmaA = novaTurma(anoLetivoAtivo, professorA);
        Long turmaB = novaTurma(anoLetivoAtivo, professorB);
        criarAluno("Escopo Aluno A", turmaA);
        criarAluno("Escopo Aluno B", turmaB);

        mockMvc.perform(get("/api/v1/alunos")
                        .param("nome", "Escopo")
                        .header("X-Perfil", "PROFESSOR")
                        .header("X-Professor-Id", String.valueOf(professorA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Escopo Aluno A"));
    }

    @Test
    void putAlteraNomeComSucessoQuandoSemAvaliacao() throws Exception {
        when(historicoAvaliacaoPort.existeAvaliacaoNaoCancelada(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        Long professorId = novoProfessor("Professor Nome");
        Long turmaId = novaTurmaAtiva(professorId);
        Long alunoId = criarAluno("Nome Original", turmaId);

        mockMvc.perform(put("/api/v1/alunos/" + alunoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Nome Alterado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Alterado"));
    }

    @Test
    void putRetorna409QuandoBloqueadoPorAvaliacaoNaoCancelada() throws Exception {
        Long professorId = novoProfessor("Professor Bloqueio");
        Long turmaId = novaTurmaAtiva(professorId);
        Long alunoId = criarAluno("Nome Bloqueado", turmaId);
        when(historicoAvaliacaoPort.existeAvaliacaoNaoCancelada(alunoId)).thenReturn(true);

        mockMvc.perform(put("/api/v1/alunos/" + alunoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Nome Tentativa"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALUNO_COM_AVALIACAO"));

        Aluno aluno = alunoRepository.findById(alunoId).orElseThrow();
        assertTrue(aluno.getNome().equals("Nome Bloqueado"));
    }

    @Test
    void deleteRetorna204EMantemALinhaComAtivoFalse() throws Exception {
        Long professorId = novoProfessor("Professor Delete");
        Long turmaId = novaTurmaAtiva(professorId);
        Long alunoId = criarAluno("Aluno Para Inativar", turmaId);

        mockMvc.perform(delete("/api/v1/alunos/" + alunoId)).andExpect(status().isNoContent());

        Optional<Aluno> aluno = alunoRepository.findById(alunoId);
        assertTrue(aluno.isPresent());
        assertFalse(aluno.get().isAtivo());
    }
}

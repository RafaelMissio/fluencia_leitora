package com.missio.fluencia_leitora.cadastros.aluno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-12/CAD-13/CAD-14/CAD-17: exercises the whole MatriculaController
 * surface against real MySQL.
 */
@AutoConfigureMockMvc
class MatriculaControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    // static: shared, never-rolled-back database across every @Test method,
    // same rationale as AnoLetivoControllerIT.anoSequencial. Kept within
    // [2000, 2100] and away from other *ControllerIT counters (Aluno
    // starts at 2065; this class needs up to 2 ativar cycles per test).
    private static int anoSequencial = 2075;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    private static int turmaSequencial = 0;

    private static synchronized int proximaTurmaSequencial() {
        return turmaSequencial++;
    }

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

    private Long novoProfessor(String nome) {
        return professorRepository.save(new Professor(nome)).getId();
    }

    private Long novaTurma(Long anoLetivoId, Long professorId) throws Exception {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "nome", "Turma " + anoLetivoId + "-" + professorId + "-" + proximaTurmaSequencial(),
                "serie", 3,
                "anoLetivoId", anoLetivoId));
        payload.put("professorId", professorId);
        MvcResult result = mockMvc.perform(post("/api/v1/turmas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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
    void postNovaMatriculaEmOutroAnoLetivoCriaComSucessoLigadaAoMesmoAluno() throws Exception {
        Long professorId = novoProfessor("Professor Matricula");
        Long turma2026Id = novaTurma(novoAnoLetivoAtivo(), professorId);
        Long alunoId = criarAluno("Aluno Duas Matriculas", turma2026Id);
        Long turma2027Id = novaTurma(novoAnoLetivoAtivo(), professorId);

        mockMvc.perform(post("/api/v1/alunos/" + alunoId + "/matriculas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("turmaId", turma2027Id))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alunoId").value(alunoId));

        List<Matricula> matriculas = matriculaRepository.findByAlunoId(alunoId);
        assertEquals(2, matriculas.size());
    }

    @Test
    void postMatriculaDuplicadaNoMesmoAnoLetivoRetorna409() throws Exception {
        Long professorId = novoProfessor("Professor Duplicada");
        Long anoLetivoId = novoAnoLetivoAtivo();
        Long turmaId = novaTurma(anoLetivoId, professorId);
        Long outraTurmaId = novaTurma(anoLetivoId, professorId);
        Long alunoId = criarAluno("Aluno Duplicado", turmaId);

        mockMvc.perform(post("/api/v1/alunos/" + alunoId + "/matriculas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("turmaId", outraTurmaId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATRICULA_DUPLICADA"));
    }

    @Test
    void patchTrocaProfessorETurmaAtualizaAmbos() throws Exception {
        Long anoLetivoId = novoAnoLetivoAtivo();
        Long professorOriginal = novoProfessor("Professor Original Patch");
        Long professorNovo = novoProfessor("Professor Novo Patch");
        Long turmaOriginal = novaTurma(anoLetivoId, professorOriginal);
        Long turmaNova = novaTurma(anoLetivoId, professorOriginal);
        Long alunoId = criarAluno("Aluno Patch", turmaOriginal);
        Long matriculaId = matriculaRepository.findByAlunoId(alunoId).get(0).getId();

        mockMvc.perform(patch("/api/v1/matriculas/" + matriculaId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("professorId", professorNovo, "turmaId", turmaNova))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professorId").value(professorNovo))
                .andExpect(jsonPath("$.turmaId").value(turmaNova));
    }

    @Test
    void patchMarcaAnoFinalizadoTrue() throws Exception {
        Long anoLetivoId = novoAnoLetivoAtivo();
        Long professorId = novoProfessor("Professor AnoFinalizado");
        Long turmaId = novaTurma(anoLetivoId, professorId);
        Long alunoId = criarAluno("Aluno AnoFinalizado", turmaId);
        Long matriculaId = matriculaRepository.findByAlunoId(alunoId).get(0).getId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("anoFinalizado", true);
        mockMvc.perform(patch("/api/v1/matriculas/" + matriculaId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anoFinalizado").value(true));
    }
}

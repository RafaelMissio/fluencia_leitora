package com.missio.fluencia_leitora.cadastros.turma;

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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-07/CAD-10/CAD-19: exercises the whole TurmaController surface against
 * real MySQL.
 */
@AutoConfigureMockMvc
class TurmaControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    // static: shared, never-rolled-back database across every @Test method,
    // same rationale as AnoLetivoControllerIT.anoSequencial.
    // Kept within [2000, 2100] (CriarAnoLetivoRequest's @Min/@Max) and away
    // from AnoLetivoControllerIT's own counter (starts at 2050).
    private static int anoSequencial = 2090;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    private Long novoAnoLetivo() throws Exception {
        int ano = proximoAno();
        MvcResult result = mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ano", ano,
                                "dataInicio", "%d-02-01".formatted(ano),
                                "dataFim", "%d-12-15".formatted(ano)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long novoProfessor(String nome) {
        return professorRepository.save(new Professor(nome)).getId();
    }

    private String turmaPayload(String nome, int serie, Long anoLetivoId, Long professorId) throws Exception {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "nome", nome,
                "serie", serie,
                "anoLetivoId", anoLetivoId));
        payload.put("professorId", professorId);
        return objectMapper.writeValueAsString(payload);
    }

    @Test
    void postCriaTurmaComSucesso() throws Exception {
        Long anoLetivoId = novoAnoLetivo();
        Long professorId = novoProfessor("Maria Silva");

        mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("Turma A", 3, anoLetivoId, professorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Turma A"))
                .andExpect(jsonPath("$.serie").value(3))
                .andExpect(jsonPath("$.anoLetivoId").value(anoLetivoId))
                .andExpect(jsonPath("$.professorId").value(professorId))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void postComNomeDuplicadoNoMesmoAnoRetorna409() throws Exception {
        Long anoLetivoId = novoAnoLetivo();
        mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("Turma B", 1, anoLetivoId, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("turma b", 2, anoLetivoId, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TURMA_DUPLICADA"));
    }

    @Test
    void postComAnoLetivoInexistenteRetorna422() throws Exception {
        mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("Turma C", 1, 999999L, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REFERENCIA_INVALIDA"));
    }

    @Test
    void putTrocaProfessorSemAlterarOutrosCampos() throws Exception {
        Long anoLetivoId = novoAnoLetivo();
        Long professorOriginal = novoProfessor("Original");
        Long novoProfessorId = novoProfessor("Substituto");
        MvcResult result = mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("Turma D", 4, anoLetivoId, professorOriginal)))
                .andExpect(status().isCreated())
                .andReturn();
        Long turmaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/turmas/" + turmaId).header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("professorId", novoProfessorId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professorId").value(novoProfessorId))
                .andExpect(jsonPath("$.nome").value("Turma D"))
                .andExpect(jsonPath("$.serie").value(4))
                .andExpect(jsonPath("$.anoLetivoId").value(anoLetivoId));
    }

    @Test
    void deleteRetorna204EMantemALinhaComAtivoFalse() throws Exception {
        Long anoLetivoId = novoAnoLetivo();
        MvcResult result = mockMvc.perform(post("/api/v1/turmas").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(turmaPayload("Turma E", 5, anoLetivoId, null)))
                .andExpect(status().isCreated())
                .andReturn();
        Long turmaId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/turmas/" + turmaId).header("Authorization", bearerCoordenador())).andExpect(status().isNoContent());

        Optional<Turma> turma = turmaRepository.findById(turmaId);
        assertTrue(turma.isPresent());
        assertFalse(turma.get().isAtivo());
    }
}

package com.missio.fluencia_leitora.cadastros.professor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-07/CAD-08/CAD-09/CAD-19: exercises the whole ProfessorController
 * surface against real MySQL. AUTH-07: PROFESSOR gets 403 on POST/DELETE.
 */
@AutoConfigureMockMvc
class ProfessorControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private String bearerProfessor() {
        Professor professor = professorRepository.save(new Professor("Professor Sem Permissao"));
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    // static: shared, never-rolled-back database across every @Test method,
    // same rationale as AnoLetivoControllerIT.anoSequencial.
    private static int anoSequencial = 2500;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    private Long criarProfessor(String nome) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professores").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void postCriaProfessorComSucesso() throws Exception {
        mockMvc.perform(post("/api/v1/professores").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Maria Silva"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void getRetornaProfessorComAsTurmasAtivasAssociadas() throws Exception {
        Long professorId = criarProfessor("João Souza");
        AnoLetivo anoLetivo = anoLetivoRepository.save(
                new AnoLetivo(proximoAno(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15)));
        Professor professor = professorRepository.findById(professorId).orElseThrow();
        turmaRepository.save(new Turma("Turma A", 1, anoLetivo, professor));
        Turma turmaInativa = turmaRepository.save(new Turma("Turma B", 2, anoLetivo, professor));
        turmaInativa.setAtivo(false);
        turmaRepository.save(turmaInativa);

        mockMvc.perform(get("/api/v1/professores/" + professorId).header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turmas.length()").value(1))
                .andExpect(jsonPath("$.turmas[0].nome").value("Turma A"));
    }

    @Test
    void deleteRetorna204EMantemALinhaComAtivoFalseQuandoSemTurmaAtiva() throws Exception {
        Long professorId = criarProfessor("Sem Turma");

        mockMvc.perform(delete("/api/v1/professores/" + professorId).header("Authorization", bearerCoordenador())).andExpect(status().isNoContent());

        Optional<Professor> professor = professorRepository.findById(professorId);
        assertTrue(professor.isPresent());
        assertFalse(professor.get().isAtivo());
    }

    @Test
    void deleteComTurmaAtivaRetorna409SemInativar() throws Exception {
        Long professorId = criarProfessor("Com Turma");
        AnoLetivo anoLetivo = anoLetivoRepository.save(
                new AnoLetivo(proximoAno(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15)));
        Professor professor = professorRepository.findById(professorId).orElseThrow();
        turmaRepository.save(new Turma("Turma C", 1, anoLetivo, professor));

        mockMvc.perform(delete("/api/v1/professores/" + professorId).header("Authorization", bearerCoordenador()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSOR_COM_TURMA_ATIVA"));

        assertTrue(professorRepository.findById(professorId).orElseThrow().isAtivo());
    }

    @Test
    void professorRecebe403EmPostEDeleteSemAlterarNada() throws Exception {
        String bearerProfessor = bearerProfessor();
        Long alvoId = criarProfessor("Alvo Intocado");
        long totalAntes = professorRepository.count();

        mockMvc.perform(post("/api/v1/professores").header("Authorization", bearerProfessor)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Nao Deve Existir"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        mockMvc.perform(delete("/api/v1/professores/" + alvoId).header("Authorization", bearerProfessor))
                .andExpect(status().isForbidden());

        assertEquals(totalAntes, professorRepository.count());
        assertTrue(professorRepository.findById(alvoId).orElseThrow().isAtivo());
    }
}

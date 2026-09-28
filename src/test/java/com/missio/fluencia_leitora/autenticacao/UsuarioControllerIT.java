package com.missio.fluencia_leitora.autenticacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUTH-07/AUTH-11/AUTH-12/AUTH-13: {@code POST /api/v1/usuarios} e
 * {@code PUT /api/v1/usuarios/{id}/senha} contra MySQL real - 201 sem
 * senha/hash, 422 para perfil x professorId e senha curta, 409
 * EMAIL_DUPLICADO sem diferenciar maiúsculas, troca de senha com desbloqueio
 * e 403 para PROFESSOR nos dois endpoints.
 */
@AutoConfigureMockMvc
class UsuarioControllerIT extends IntegrationTestBase {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String emailUnico() {
        return "usuario" + SEQUENCIA.incrementAndGet() + "@escola.com";
    }

    private String json(String email, String senha, String perfil, Long professorId) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("email", email);
        corpo.put("senha", senha);
        corpo.put("perfil", perfil);
        corpo.put("professorId", professorId);
        return objectMapper.writeValueAsString(corpo);
    }

    private String bearerProfessor() {
        Professor professor = professorRepository.save(new Professor("Professor Sem Permissao"));
        Usuario usuario = usuarioRepository.save(
                new Usuario(emailUnico(), "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    @Test
    void postCriaUsuarioProfessorAtivoCom201SemSenhaNemHashNaResposta() throws Exception {
        Professor professor = professorRepository.save(new Professor("Professor Novo"));
        String email = emailUnico();

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(email, "SenhaForte1", "PROFESSOR", professor.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.perfil").value("PROFESSOR"))
                .andExpect(jsonPath("$.professorId").value(professor.getId()))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario gravado = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(passwordEncoder.matches("SenhaForte1", gravado.getSenhaHash()));
    }

    @Test
    void postComPerfilInconsistenteOuSenhaCurtaRetorna422() throws Exception {
        Long professorId = professorRepository.save(new Professor("Professor Vinculo")).getId();

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(emailUnico(), "SenhaForte1", "PROFESSOR", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PROFESSOR_ID_INVALIDO"));
        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(emailUnico(), "SenhaForte1", "COORDENADOR", professorId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PROFESSOR_ID_INVALIDO"));
        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(emailUnico(), "1234567", "COORDENADOR", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SENHA_INVALIDA"));
    }

    @Test
    void postComEmailJaEmUsoEmOutraCaixaRetorna409EmailDuplicado() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(email, "SenhaForte1", "COORDENADOR", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(json(email.toUpperCase(), "SenhaForte1", "COORDENADOR", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICADO"));
    }

    @Test
    void putSenhaGravaNovoHashEDesbloqueiaAConta() throws Exception {
        Usuario usuario = usuarioRepository.save(
                new Usuario(emailUnico(), passwordEncoder.encode("SenhaAntiga1"), Perfil.COORDENADOR, null));
        for (int i = 0; i < 5; i++) {
            usuarioRepository.registrarFalha(usuario.getId(), Instant.now().plusSeconds(900), 5);
        }

        mockMvc.perform(put("/api/v1/usuarios/" + usuario.getId() + "/senha")
                        .header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("senha", "SenhaNova123"))))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("SenhaNova123", atualizado.getSenhaHash()));
        assertEquals(0, atualizado.getTentativasFalhas());
        assertNull(atualizado.getBloqueadoAte());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", usuario.getEmail(), "senha", "SenhaNova123"))))
                .andExpect(status().isOk());
    }

    @Test
    void professorRecebe403EmPostEPutDeUsuarios() throws Exception {
        String bearerProfessor = bearerProfessor();
        Usuario alvo = usuarioRepository.save(
                new Usuario(emailUnico(), passwordEncoder.encode("SenhaAntiga1"), Perfil.COORDENADOR, null));

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearerProfessor)
                        .contentType("application/json")
                        .content(json(emailUnico(), "SenhaForte1", "COORDENADOR", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/usuarios/" + alvo.getId() + "/senha").header("Authorization", bearerProfessor)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("senha", "SenhaNova123"))))
                .andExpect(status().isForbidden());

        assertTrue(passwordEncoder.matches(
                "SenhaAntiga1", usuarioRepository.findById(alvo.getId()).orElseThrow().getSenhaHash()));
    }
}

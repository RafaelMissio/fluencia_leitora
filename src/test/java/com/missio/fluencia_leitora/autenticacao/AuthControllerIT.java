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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUTH-01..AUTH-04: {@code POST /api/v1/auth/login} sem token, contra MySQL
 * real. 200 com token/expiresIn/perfil/professorId; 401 genérico e idêntico
 * para e-mail inexistente, senha errada e usuário inativo; 429 com
 * {@code Retry-After} na 6ª tentativa após 5 falhas, mesmo com a senha certa.
 */
@AutoConfigureMockMvc
class AuthControllerIT extends IntegrationTestBase {

    private static final String SENHA = "SenhaCorreta#1";
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
        return "login" + SEQUENCIA.incrementAndGet() + "@escola.com";
    }

    private Usuario criarUsuario(String email, Perfil perfil, Long professorId) {
        return usuarioRepository.save(new Usuario(email, passwordEncoder.encode(SENHA), perfil, professorId));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))));
    }

    @Test
    void loginCertoDeProfessorRetorna200ComTokenExpiresInPerfilEProfessorId() throws Exception {
        Professor professor = professorRepository.save(new Professor("Professor Login"));
        Usuario usuario = criarUsuario(emailUnico(), Perfil.PROFESSOR, professor.getId());

        MvcResult resultado = login(usuario.getEmail().toUpperCase(), SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(28800))
                .andExpect(jsonPath("$.perfil").value("PROFESSOR"))
                .andExpect(jsonPath("$.professorId").value(professor.getId()))
                .andReturn();

        String token = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("accessToken").asText();
        assertEquals(Optional.of(usuario.getId()), jwtService.validarERetornarUsuarioId(token));
    }

    @Test
    void emailInexistenteESenhaErradaRetornam401ComAMesmaMensagemGenerica() throws Exception {
        Usuario usuario = criarUsuario(emailUnico(), Perfil.COORDENADOR, null);

        String inexistente = login("ninguem-" + emailUnico(), SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"))
                .andReturn().getResponse().getContentAsString();
        String senhaErrada = login(usuario.getEmail(), "SenhaErrada#1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(inexistente, senhaErrada);
    }

    @Test
    void usuarioInativoRetorna401ComCredenciaisInvalidas() throws Exception {
        Usuario usuario = criarUsuario(emailUnico(), Perfil.COORDENADOR, null);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        login(usuario.getEmail(), SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
    }

    @Test
    void sextaTentativaApos5FalhasRetorna429ComRetryAfterMesmoComSenhaCerta() throws Exception {
        Usuario usuario = criarUsuario(emailUnico(), Perfil.COORDENADOR, null);
        for (int i = 1; i <= 5; i++) {
            login(usuario.getEmail(), "SenhaErrada#" + i).andExpect(status().isUnauthorized());
        }

        String retryAfter = login(usuario.getEmail(), SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn().getResponse().getHeader("Retry-After");

        long segundos = Long.parseLong(retryAfter);
        assertTrue(segundos > 0 && segundos <= 900, "Retry-After fora de 15 minutos: " + segundos);
    }
}

package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUTH-06/AUTH-15: rota protegida sem token (ou com token inválido) → 401 em
 * ProblemDetail; perfil sem permissão num endpoint com {@code @PreAuthorize}
 * → 403 em ProblemDetail; rotas públicas acessíveis sem token.
 */
@AutoConfigureMockMvc
@Import(SecurityConfigIT.EndpointSomenteCoordenador.class)
class SecurityConfigIT extends IntegrationTestBase {

    @RestController
    static class EndpointSomenteCoordenador {

        @GetMapping("/api/v1/teste-seguranca/somente-coordenador")
        @PreAuthorize("hasRole('COORDENADOR')")
        String somenteCoordenador() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void rotaProtegidaSemTokenOuComTokenInvalidoRetorna401EmProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/ciclos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"));

        mockMvc.perform(get("/api/v1/ciclos").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"));
    }

    @Test
    void perfilSemPermissaoRetorna403EmProblemDetail() throws Exception {
        Usuario professor = usuarioRepository.findByEmailIgnoreCase("t5-professor@escola.com")
                .orElseGet(() -> usuarioRepository.save(
                        new Usuario("t5-professor@escola.com", "hash", Perfil.PROFESSOR, null)));
        String token = jwtService.emitir(professor.getId());

        mockMvc.perform(get("/api/v1/teste-seguranca/somente-coordenador")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
    }

    @Test
    void rotasPublicasAcessiveisSemToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        // Liberada pela segurança (senão seria 401); 404 porque o actuator
        // não é dependência do projeto - a requisição chegou ao MVC.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }
}

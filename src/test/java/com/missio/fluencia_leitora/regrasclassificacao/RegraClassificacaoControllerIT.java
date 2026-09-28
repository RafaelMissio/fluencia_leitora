package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REG-06: exercita {@code GET /api/v1/regras-classificacao} contra MySQL
 * real (seed da migração V7 - design.md, Data Models) - caminho feliz,
 * autenticação e o novo mapeamento de {@code @RequestParam} inválido para
 * {@code VALIDACAO_INVALIDA} (T2).
 */
@AutoConfigureMockMvc
class RegraClassificacaoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private String bearerProfessor() {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-regra-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, null));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    // Série 1: só lida por estes testes (nunca substituída) - seed da migração V7 tem 6 faixas ativas.

    @Test
    void getComProfessorRetorna200ComAsSeisFaixasAtivasDoSeed() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao").header("Authorization", bearerProfessor())
                        .param("serie", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].quantidadeMinimaAcertos").value(0))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void getComCoordenadorRetorna200ComAsSeisFaixasAtivasDoSeed() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao").header("Authorization", bearerCoordenador())
                        .param("serie", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void getSemAuthorizationRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao").param("serie", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getComSerieForaDoIntervaloRetorna422ValidacaoInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao").header("Authorization", bearerCoordenador())
                        .param("serie", "6"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }
}

package com.missio.fluencia_leitora.regrasclassificacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REG-06..REG-13: exercita {@code GET}/{@code PUT
 * .../regras-classificacao} contra MySQL real (seed da migração V7 -
 * design.md, Data Models) - caminho feliz, autorização, autenticação e o
 * novo mapeamento de {@code @RequestParam}/{@code @PathVariable} inválido
 * para {@code VALIDACAO_INVALIDA} (T2).
 *
 * <p>Toda mutação real de {@code PUT} usa a série 5 - a única não lida por
 * {@code RegraClassificacaoRepositoryIT} (T6), que depende do seed
 * intocado das séries 1-4. Uma falha de validação nunca toca o
 * repositório (REG-12), então os testes de série 1/2/3/6 abaixo (só
 * validação/autorização) não mutam nada e podem compartilhar série com
 * outras classes de teste. Os dois testes que realmente substituem a
 * série 5 (sucesso do PUT; histórico com duas substituições) não
 * assumem ordem entre si - o teste de histórico usa uma contagem relativa
 * (grupos antes/depois), não um total absoluto.
 */
@AutoConfigureMockMvc
class RegraClassificacaoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String bearerProfessor() {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-regra-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, null));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    private Map<String, Object> faixa(int minimo, Integer maximo, String fase, Integer nivel) {
        Map<String, Object> faixa = new HashMap<>();
        faixa.put("quantidadeMinimaAcertos", minimo);
        faixa.put("quantidadeMaximaAcertos", maximo);
        faixa.put("fase", fase);
        faixa.put("nivel", nivel);
        return faixa;
    }

    private Map<String, Object> payloadValido() {
        return Map.of("faixas", List.of(
                faixa(0, 4, "PRE_LEITOR", 1),
                faixa(5, 10, "LEITOR_INICIANTE", null),
                faixa(11, null, "LEITOR_FLUENTE", null)));
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

    // --- PUT /series/{serie} (T13, REG-07..REG-13) - série 5, exclusiva deste teste. ---

    @Test
    void putComCoordenadorSubstituiFaixasDaSerieERetorna200EGetPosteriorReflete() throws Exception {
        mockMvc.perform(put("/api/v1/regras-classificacao/series/5").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].quantidadeMinimaAcertos").value(0))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[2].fase").value("LEITOR_FLUENTE"));

        // Efeito observável sem reiniciar a aplicação: o GET seguinte reflete o novo conjunto.
        mockMvc.perform(get("/api/v1/regras-classificacao").header("Authorization", bearerCoordenador())
                        .param("serie", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].quantidadeMinimaAcertos").value(5))
                .andExpect(jsonPath("$[1].fase").value("LEITOR_INICIANTE"));
    }

    // --- PUT /series/{serie} - testes que não mutam estado (falha antes do repositório ou bloqueados antes do service). ---

    @Test
    void putComProfessorRetorna403() throws Exception {
        mockMvc.perform(put("/api/v1/regras-classificacao/series/2").header("Authorization", bearerProfessor())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    void putSemAuthorizationRetorna401() throws Exception {
        mockMvc.perform(put("/api/v1/regras-classificacao/series/3")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putComSerieForaDoIntervaloNoPathRetorna422ValidacaoInvalida() throws Exception {
        mockMvc.perform(put("/api/v1/regras-classificacao/series/6").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payloadValido())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void putComListaVaziaRetorna422FaixaNaoIniciaEmZero() throws Exception {
        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("faixas", List.of()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FAIXA_NAO_INICIA_EM_ZERO"));
    }

    @Test
    void putComLacunaEntreFaixasRetorna422FaixaComLacunaComValor() throws Exception {
        Map<String, Object> payload = Map.of("faixas", List.of(
                faixa(0, 3, "PRE_LEITOR", 1),
                faixa(5, null, "LEITOR_FLUENTE", null)));

        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FAIXA_COM_LACUNA"))
                .andExpect(jsonPath("$.valor").value(4));
    }

    @Test
    void putComFaixasSobrepostasRetorna422FaixaSobrepostaComValor() throws Exception {
        Map<String, Object> payload = Map.of("faixas", List.of(
                faixa(0, 5, "PRE_LEITOR", 1),
                faixa(4, null, "LEITOR_FLUENTE", null)));

        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FAIXA_SOBREPOSTA"))
                .andExpect(jsonPath("$.valor").value(4));
    }

    @Test
    void putComUltimaFaixaComMaximoRetorna422FaixaFinalLimitada() throws Exception {
        Map<String, Object> payload = Map.of("faixas", List.of(
                faixa(0, 5, "PRE_LEITOR", 1),
                faixa(6, 10, "LEITOR_FLUENTE", null)));

        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FAIXA_FINAL_LIMITADA"));
    }

    @Test
    void putComNivelIncoerenteComAFaseRetorna422FaixaNivelIncoerente() throws Exception {
        Map<String, Object> payload = Map.of("faixas", List.of(faixa(0, null, "PRE_LEITOR", null)));

        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FAIXA_NIVEL_INCOERENTE"));
    }

    @Test
    void putComQuantidadeMinimaAcertosNegativaRetorna422ValidacaoInvalida() throws Exception {
        Map<String, Object> payload = Map.of("faixas", List.of(faixa(-1, null, "LEITOR_FLUENTE", null)));

        mockMvc.perform(put("/api/v1/regras-classificacao/series/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    // --- GET /historico (T14, REG-15) ---

    @Test
    void historicoComProfessorRetorna200() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao/historico").header("Authorization", bearerProfessor())
                        .param("serie", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void historicoComCoordenadorRetorna200() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao/historico").header("Authorization", bearerCoordenador())
                        .param("serie", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void historicoComSerieForaDoIntervaloRetorna422ValidacaoInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/regras-classificacao/historico").header("Authorization", bearerCoordenador())
                        .param("serie", "6"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void historicoAposDuasSubstituicoesGanhaDoisGruposNovosComOCorrentePrimeiro() throws Exception {
        // Série 5 também é usada pelo teste de sucesso do PUT (acima) - a ordem entre os dois
        // testes não é garantida, então esta asserção é relativa (quantos grupos a mais surgiram
        // com estas duas substituições), não um total absoluto.
        MvcResult antes = mockMvc.perform(get("/api/v1/regras-classificacao/historico")
                        .header("Authorization", bearerCoordenador())
                        .param("serie", "5"))
                .andExpect(status().isOk())
                .andReturn();
        int gruposAntes = objectMapper.readTree(antes.getResponse().getContentAsString()).size();

        Map<String, Object> primeiraSubstituicao = Map.of("faixas", List.of(
                faixa(0, 9, "PRE_LEITOR", 1),
                faixa(10, null, "LEITOR_FLUENTE", null)));
        mockMvc.perform(put("/api/v1/regras-classificacao/series/5").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(primeiraSubstituicao)))
                .andExpect(status().isOk());

        Map<String, Object> segundaSubstituicao = Map.of("faixas", List.of(faixa(0, null, "LEITOR_FLUENTE", null)));
        mockMvc.perform(put("/api/v1/regras-classificacao/series/5").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(segundaSubstituicao)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/regras-classificacao/historico").header("Authorization", bearerCoordenador())
                        .param("serie", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(gruposAntes + 2))
                // Grupo corrente (a 2ª substituição) primeiro: alteradoEm=null.
                .andExpect(jsonPath("$[0].alteradoEm").doesNotExist())
                .andExpect(jsonPath("$[0].faixas.length()").value(1))
                .andExpect(jsonPath("$[0].faixas[0].fase").value("LEITOR_FLUENTE"))
                // Logo depois, a 1ª substituição (inativada pela 2ª).
                .andExpect(jsonPath("$[1].alteradoEm").exists())
                .andExpect(jsonPath("$[1].faixas.length()").value(2));
    }
}

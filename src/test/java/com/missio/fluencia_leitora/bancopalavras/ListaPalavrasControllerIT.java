package com.missio.fluencia_leitora.bancopalavras;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PAL-01..PAL-12: exercita {@code POST /api/v1/listas-palavras} contra MySQL
 * real - caminho feliz, autorização (AUTH-07: só COORDENADOR escreve) e cada
 * erro 422 do spec.
 */
@AutoConfigureMockMvc
class ListaPalavrasControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String bearerProfessor() {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-lista-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, null));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    private Map<String, Object> item(String palavra, String tipoPalavra) {
        Map<String, Object> item = new HashMap<>();
        item.put("palavra", palavra);
        item.put("tipoPalavra", tipoPalavra);
        return item;
    }

    private Map<String, Object> payloadPalavra(String nome, int serie, List<Map<String, Object>> itens) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", nome);
        payload.put("serie", serie);
        payload.put("tipoLeitura", "PALAVRA");
        payload.put("itens", itens);
        return payload;
    }

    @Test
    void postComCoordenadorCriaListaPalavraValidaRetorna201ComCorpoEsperado() throws Exception {
        Map<String, Object> payload = payloadPalavra(
                "Lista Válida " + UUID.randomUUID(), 2,
                List.of(item("gato", "CANONICA"), item("bola", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(payload.get("nome")))
                .andExpect(jsonPath("$.quantidadePalavras").value(2))
                .andExpect(jsonPath("$.itens[0].palavra").value("gato"))
                .andExpect(jsonPath("$.itens[0].ordem").value(1))
                .andExpect(jsonPath("$.itens[1].palavra").value("bola"))
                .andExpect(jsonPath("$.itens[1].ordem").value(2));
    }

    @Test
    void postComProfessorRetorna403() throws Exception {
        Map<String, Object> payload =
                payloadPalavra("Lista Professor " + UUID.randomUUID(), 2, List.of(item("gato", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerProfessor())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postSemAuthorizationRetorna401() throws Exception {
        Map<String, Object> payload =
                payloadPalavra("Lista Sem Auth " + UUID.randomUUID(), 2, List.of(item("gato", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSerie1ComItemNaoCanonicaRetorna422ComPosicoes() throws Exception {
        Map<String, Object> payload = payloadPalavra(
                "Lista 1º Ano " + UUID.randomUUID(), 1,
                List.of(item("gato", "CANONICA"), item("blicar", "NAO_CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NAO_CANONICA_PROIBIDA_1_ANO"))
                .andExpect(jsonPath("$.posicoes[0]").value(2));
    }

    @Test
    void postComPalavraDuplicadaRetorna422PalavraDuplicada() throws Exception {
        Map<String, Object> payload = payloadPalavra(
                "Lista Duplicada " + UUID.randomUUID(), 2,
                List.of(item("Gato", "CANONICA"), item("gato", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PALAVRA_DUPLICADA"));
    }

    @Test
    void postTextoCurtoComItensEnviadosRetorna422ConteudoIncompativelComTipo() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", "Texto Errado " + UUID.randomUUID());
        payload.put("serie", 2);
        payload.put("tipoLeitura", "TEXTO_CURTO");
        payload.put("tipoPalavra", "CANONICA");
        payload.put("itens", List.of(item("gato", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTEUDO_INCOMPATIVEL_COM_TIPO"));
    }

    @Test
    void postComFormatoDePalavraInvalidoRetorna422ComCampoDoItem() throws Exception {
        Map<String, Object> payload = payloadPalavra(
                "Lista Palavra Inválida " + UUID.randomUUID(), 2, List.of(item("gato123", "CANONICA")));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.errors[0].field").value("itens[0].palavra"));
    }

    @Test
    void postComItensVazioRetorna422PorTamanhoForaDaFaixa() throws Exception {
        Map<String, Object> payload = payloadPalavra("Lista Sem Itens " + UUID.randomUUID(), 2, List.of());

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void postTextoCurtoComMaisDe200TokensRetorna422() throws Exception {
        List<String> palavras = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            palavras.add("p" + i);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", "Texto Longo " + UUID.randomUUID());
        payload.put("serie", 2);
        payload.put("tipoLeitura", "TEXTO_CURTO");
        payload.put("tipoPalavra", "CANONICA");
        payload.put("texto", String.join(" ", palavras));

        mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode criarListaERetornarCorpo(String nome, int serie, List<Map<String, Object>> itens) throws Exception {
        Map<String, Object> payload = payloadPalavra(nome, serie, itens);
        MvcResult result = mockMvc.perform(post("/api/v1/listas-palavras").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> payloadAtualizacao(
            String nome, int serie, List<Map<String, Object>> itens, long version) {
        Map<String, Object> payload = payloadPalavra(nome, serie, itens);
        payload.put("version", version);
        return payload;
    }

    @Test
    void putComCoordenadorAtualizaListaExistenteRetorna200ComNovoConteudo() throws Exception {
        JsonNode criada = criarListaERetornarCorpo(
                "Lista Original " + UUID.randomUUID(), 2, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();
        // ListaPalavrasResponse não expõe `version` (T9); uma lista recém-criada
        // sempre começa com version=0 (semântica do @Version Long do Hibernate).
        long version = 0L;

        Map<String, Object> payload = payloadAtualizacao(
                "Lista Editada " + UUID.randomUUID(), 2,
                List.of(item("gato", "CANONICA"), item("bola", "CANONICA")), version);

        mockMvc.perform(put("/api/v1/listas-palavras/" + id).header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(payload.get("nome")))
                .andExpect(jsonPath("$.quantidadePalavras").value(2));
    }

    @Test
    void putComVersionDivergenteRetorna409ConflitoDeVersao() throws Exception {
        JsonNode criada = criarListaERetornarCorpo(
                "Lista Conflito " + UUID.randomUUID(), 2, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();

        Map<String, Object> payload =
                payloadAtualizacao("Lista Conflito Editada", 2, List.of(item("gato", "CANONICA")), 999L);

        mockMvc.perform(put("/api/v1/listas-palavras/" + id).header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLITO_DE_VERSAO"));
    }

    @Test
    void putComProfessorRetorna403() throws Exception {
        JsonNode criada = criarListaERetornarCorpo(
                "Lista Professor Put " + UUID.randomUUID(), 2, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();
        long version = 0L;

        Map<String, Object> payload =
                payloadAtualizacao("Lista Professor Editada", 2, List.of(item("gato", "CANONICA")), version);

        mockMvc.perform(put("/api/v1/listas-palavras/" + id).header("Authorization", bearerProfessor())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    private boolean filtradaContemNome(JsonNode resposta, String nome) {
        return StreamSupport.stream(resposta.spliterator(), false)
                .anyMatch(no -> no.get("nome").asText().equals(nome));
    }

    @Test
    void getFiltradoComCoordenadorRetorna200ComListaCriada() throws Exception {
        String nome = "Lista Filtro Coordenador " + UUID.randomUUID();
        criarListaERetornarCorpo(nome, 4, List.of(item("gato", "CANONICA")));

        MvcResult result = mockMvc.perform(get("/api/v1/listas-palavras")
                        .header("Authorization", bearerCoordenador())
                        .param("serie", "4")
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(filtradaContemNome(objectMapper.readTree(result.getResponse().getContentAsString()), nome));
    }

    @Test
    void getFiltradoComProfessorRetorna200ComListaCriada() throws Exception {
        String nome = "Lista Filtro Professor " + UUID.randomUUID();
        criarListaERetornarCorpo(nome, 4, List.of(item("gato", "CANONICA")));

        MvcResult result = mockMvc.perform(get("/api/v1/listas-palavras")
                        .header("Authorization", bearerProfessor())
                        .param("serie", "4")
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(filtradaContemNome(objectMapper.readTree(result.getResponse().getContentAsString()), nome));
    }

    @Test
    void getFiltradoNaoRetornaListaDeOutraSerie() throws Exception {
        String nome = "Lista Serie Diferente " + UUID.randomUUID();
        criarListaERetornarCorpo(nome, 4, List.of(item("gato", "CANONICA")));

        MvcResult result = mockMvc.perform(get("/api/v1/listas-palavras")
                        .header("Authorization", bearerCoordenador())
                        .param("serie", "5")
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(filtradaContemNome(objectMapper.readTree(result.getResponse().getContentAsString()), nome));
    }

    @Test
    void deleteComProfessorRetorna403() throws Exception {
        JsonNode criada = criarListaERetornarCorpo(
                "Lista Delete Professor " + UUID.randomUUID(), 4, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();

        mockMvc.perform(delete("/api/v1/listas-palavras/" + id).header("Authorization", bearerProfessor()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComCoordenadorRetorna204EListaSomeDoFiltrado() throws Exception {
        String nome = "Lista Delete Coordenador " + UUID.randomUUID();
        JsonNode criada = criarListaERetornarCorpo(nome, 4, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();

        mockMvc.perform(delete("/api/v1/listas-palavras/" + id).header("Authorization", bearerCoordenador()))
                .andExpect(status().isNoContent());

        MvcResult resultFiltrado = mockMvc.perform(get("/api/v1/listas-palavras")
                        .header("Authorization", bearerCoordenador())
                        .param("serie", "4")
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(filtradaContemNome(objectMapper.readTree(resultFiltrado.getResponse().getContentAsString()), nome));
    }

    @Test
    void listaInativadaContinuaAcessivelPorId() throws Exception {
        String nome = "Lista Inativada Por Id " + UUID.randomUUID();
        JsonNode criada = criarListaERetornarCorpo(nome, 4, List.of(item("gato", "CANONICA")));
        long id = criada.get("id").asLong();

        mockMvc.perform(delete("/api/v1/listas-palavras/" + id).header("Authorization", bearerCoordenador()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/listas-palavras/" + id).header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.nome").value(nome));
    }

    @Test
    void getPorIdDeIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/listas-palavras/999999999").header("Authorization", bearerCoordenador()))
                .andExpect(status().isNotFound());
    }
}

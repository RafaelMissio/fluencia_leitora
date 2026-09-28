package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-01/CAD-02/CAD-03/CAD-04/CAD-06/CAD-19/CAD-20: exercises the whole
 * AnoLetivoController surface against real MySQL. AUTH-07: PROFESSOR gets
 * 403 on every write endpoint.
 */
@AutoConfigureMockMvc
class AnoLetivoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private JwtService jwtService;

    // static: JUnit creates a fresh test instance per @Test method, but every
    // method shares the same (non-rolled-back) database, so the counter must
    // survive across instances to avoid ANO_LETIVO_DUPLICADO collisions.
    private static int anoSequencial = 2050;

    private static synchronized int proximoAno() {
        return anoSequencial++;
    }

    private String criarAnoLetivoPayload(int ano) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "ano", ano,
                "dataInicio", "%d-02-01".formatted(ano),
                "dataFim", "%d-12-15".formatted(ano)));
    }

    private Long criarAnoLetivo(int ano) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(criarAnoLetivoPayload(ano)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void postCriaAnoLetivoComSucessoEAsCincoConfiguracoesDoSeed() throws Exception {
        int ano = proximoAno();

        Long id = criarAnoLetivo(ano);

        List<ConfiguracaoAvaliacao> configuracoes = List.of(1, 2, 3, 4, 5).stream()
                .map(serie -> configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(id, serie).orElseThrow())
                .toList();
        assertEquals(5, configuracoes.size());
        assertEquals(15, configuracoes.get(0).getQuantidadeMinima());
        assertEquals(20, configuracoes.get(0).getQuantidadeMaxima());
        for (int i = 1; i < 5; i++) {
            assertEquals(20, configuracoes.get(i).getQuantidadeMinima());
            assertEquals(60, configuracoes.get(i).getQuantidadeMaxima());
        }
    }

    @Test
    void postComAnoDuplicadoRetorna409() throws Exception {
        int ano = proximoAno();
        criarAnoLetivo(ano);

        mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(criarAnoLetivoPayload(ano)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANO_LETIVO_DUPLICADO"));
    }

    @Test
    void postComAnoOuDatasInvalidasRetorna422() throws Exception {
        String anoForaDaFaixa = objectMapper.writeValueAsString(Map.of(
                "ano", 1999,
                "dataInicio", "1999-02-01",
                "dataFim", "1999-12-15"));
        mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerCoordenador()).contentType("application/json").content(anoForaDaFaixa))
                .andExpect(status().isUnprocessableEntity());

        int ano = proximoAno();
        String dataFimAnteriorADataInicio = objectMapper.writeValueAsString(Map.of(
                "ano", ano,
                "dataInicio", "%d-12-15".formatted(ano),
                "dataFim", "%d-02-01".formatted(ano)));
        mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(dataFimAnteriorADataInicio))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void postAtivarEncerraOAtivoAnteriorEAtivaONovo() throws Exception {
        Long idAntigo = criarAnoLetivo(proximoAno());
        mockMvc.perform(post("/api/v1/anos-letivos/" + idAntigo + "/ativar").header("Authorization", bearerCoordenador())).andExpect(status().isOk());

        Long idNovo = criarAnoLetivo(proximoAno());
        mockMvc.perform(post("/api/v1/anos-letivos/" + idNovo + "/ativar").header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"));

        AnoLetivo antigo = anoLetivoRepository.findById(idAntigo).orElseThrow();
        assertEquals(SituacaoAnoLetivo.ENCERRADO, antigo.getSituacao());
    }

    @Test
    void putConfiguracaoAtualizaComSucesso() throws Exception {
        Long id = criarAnoLetivo(proximoAno());
        String payload = objectMapper.writeValueAsString(Map.of(
                "quantidadeMinima", 10,
                "quantidadeMaxima", 25));

        mockMvc.perform(put("/api/v1/anos-letivos/" + id + "/configuracoes/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeMinima").value(10))
                .andExpect(jsonPath("$.quantidadeMaxima").value(25));
    }

    @Test
    void putConfiguracaoInvalidaRetorna422SemAlterarRegistro() throws Exception {
        Long id = criarAnoLetivo(proximoAno());
        String payloadInvalido = objectMapper.writeValueAsString(Map.of(
                "quantidadeMinima", 30,
                "quantidadeMaxima", 20));

        mockMvc.perform(put("/api/v1/anos-letivos/" + id + "/configuracoes/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(payloadInvalido))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INTERVALO_INVALIDO"));

        ConfiguracaoAvaliacao configuracao = configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(id, 1)
                .orElseThrow();
        assertEquals(15, configuracao.getQuantidadeMinima());
        assertEquals(20, configuracao.getQuantidadeMaxima());
    }

    @Test
    void deleteRetorna204EMantemALinhaComAtivoFalse() throws Exception {
        Long id = criarAnoLetivo(proximoAno());

        mockMvc.perform(delete("/api/v1/anos-letivos/" + id).header("Authorization", bearerCoordenador())).andExpect(status().isNoContent());

        Optional<AnoLetivo> anoLetivo = anoLetivoRepository.findById(id);
        assertTrue(anoLetivo.isPresent());
        assertFalse(anoLetivo.get().isAtivo());
    }

    @Test
    void duasRequisicoesPutConcorrentesNaMesmaConfiguracao_segundaRecebe409() throws Exception {
        Long id = criarAnoLetivo(proximoAno());
        String payload = objectMapper.writeValueAsString(Map.of(
                "quantidadeMinima", 10,
                "quantidadeMaxima", 25));

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<MockHttpServletResponse>> futures = List.of(
                    executor.submit(() -> putConfiguracao(id, payload, barrier)),
                    executor.submit(() -> putConfiguracao(id, payload, barrier)));

            MockHttpServletResponse response1 = futures.get(0).get();
            MockHttpServletResponse response2 = futures.get(1).get();

            List<Integer> statuses = List.of(response1.getStatus(), response2.getStatus());
            assertTrue(statuses.contains(200), "esperava que uma das requisições recebesse 200, recebeu: " + statuses);
            assertTrue(statuses.contains(409), "esperava que uma das requisições recebesse 409, recebeu: " + statuses);

            MockHttpServletResponse respostaComConflito = response1.getStatus() == 409 ? response1 : response2;
            String corpoConflito = respostaComConflito.getContentAsString();
            assertTrue(corpoConflito.contains("CONFLITO_DE_VERSAO"),
                    "esperava code=CONFLITO_DE_VERSAO no corpo da resposta 409, recebeu: " + corpoConflito);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void professorRecebe403EmTodosOsEndpointsDeEscritaSemAlterarNada() throws Exception {
        Professor professor = professorRepository.save(new Professor("Professor Ano Letivo"));
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        String bearerProfessor = "Bearer " + jwtService.emitir(usuario.getId());
        int anoNaoCriado = proximoAno();
        Long id = criarAnoLetivo(proximoAno());

        mockMvc.perform(post("/api/v1/anos-letivos").header("Authorization", bearerProfessor)
                        .contentType("application/json")
                        .content(criarAnoLetivoPayload(anoNaoCriado)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/api/v1/anos-letivos/" + id + "/ativar").header("Authorization", bearerProfessor))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/anos-letivos/" + id + "/configuracoes/1").header("Authorization", bearerProfessor)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("quantidadeMinima", 10, "quantidadeMaxima", 25))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/anos-letivos/" + id).header("Authorization", bearerProfessor))
                .andExpect(status().isForbidden());

        assertFalse(anoLetivoRepository.existsByAno(anoNaoCriado));
        AnoLetivo anoLetivo = anoLetivoRepository.findById(id).orElseThrow();
        assertTrue(anoLetivo.isAtivo());
        assertTrue(anoLetivo.getSituacao() != SituacaoAnoLetivo.ATIVO);
        assertEquals(15, configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(id, 1).orElseThrow().getQuantidadeMinima());
    }

    private MockHttpServletResponse putConfiguracao(Long id, String payload, CyclicBarrier barrier) throws Exception {
        barrier.await();
        return mockMvc.perform(put("/api/v1/anos-letivos/" + id + "/configuracoes/1").header("Authorization", bearerCoordenador())
                        .contentType("application/json")
                        .content(payload))
                .andReturn()
                .getResponse();
    }
}

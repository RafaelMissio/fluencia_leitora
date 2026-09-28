package com.missio.fluencia_leitora.cadastros.dominio;

import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAD-18: GET /api/v1/ciclos and GET /api/v1/tipos-leitura expose the exact
 * seed rows created by V1__dominios_fixos.sql, in seed order; write verbs on
 * those read-only paths return 405.
 */
@AutoConfigureMockMvc
class DominioFixoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCiclosRetornaOsTresCiclosDoSeedNaOrdem() throws Exception {
        mockMvc.perform(get("/api/v1/ciclos").header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("ENTRADA"))
                .andExpect(jsonPath("$[0].descricao").value("Entrada"))
                .andExpect(jsonPath("$[1].codigo").value("ACOMPANHAMENTO"))
                .andExpect(jsonPath("$[1].descricao").value("Acompanhamento"))
                .andExpect(jsonPath("$[2].codigo").value("SAIDA"))
                .andExpect(jsonPath("$[2].descricao").value("Saída"));
    }

    @Test
    void getTiposLeituraRetornaOsTresTiposDoSeedNaOrdem() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-leitura").header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("PALAVRA"))
                .andExpect(jsonPath("$[0].descricao").value("Leitura de Palavras"))
                .andExpect(jsonPath("$[1].codigo").value("PSEUDOPALAVRA"))
                .andExpect(jsonPath("$[1].descricao").value("Leitura de Pseudopalavras"))
                .andExpect(jsonPath("$[2].codigo").value("TEXTO_CURTO"))
                .andExpect(jsonPath("$[2].descricao").value("Leitura de Texto Curto"));
    }

    @Test
    void postEmCiclosRetorna405() throws Exception {
        mockMvc.perform(post("/api/v1/ciclos").header("Authorization", bearerCoordenador()))
                .andExpect(status().isMethodNotAllowed());
    }
}

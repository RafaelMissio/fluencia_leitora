package com.missio.fluencia_leitora.common.texto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PAL-08: cobre a tokenização de texto corrido usada pelas listas
 * {@code TEXTO_CURTO} (T10, feature {@code banco-palavras}).
 */
class TokenizadorTextoTest {

    @Test
    void tokenizarOGatoAVirgulaABolaPontoGeraExatamenteAsQuatroPalavrasNaOrdem() {
        List<String> tokens = TokenizadorTexto.tokenizar("O gato, a bola.");

        assertEquals(List.of("O", "gato", "a", "bola"), tokens);
    }

    @Test
    void espacosMultiplosEntrePalavrasNaoGeramTokensVazios() {
        List<String> tokens = TokenizadorTexto.tokenizar("O   gato    pula");

        assertEquals(List.of("O", "gato", "pula"), tokens);
    }

    @Test
    void pontuacaoSoNasBordasEhRemovidaEHifenInternoNaoEhAfetado() {
        List<String> tokens = TokenizadorTexto.tokenizar("\"bem-vindo,\" disse-ele!");

        assertEquals(List.of("bem-vindo", "disse-ele"), tokens);
    }

    @Test
    void grafiaOriginalMaiusculasEAcentosEhPreservadaEmCadaToken() {
        List<String> tokens = TokenizadorTexto.tokenizar("A Ação começa às CINCO horas.");

        assertEquals(List.of("A", "Ação", "começa", "às", "CINCO", "horas"), tokens);
    }

    @Test
    void tokenQueFicaVazioAposRemoverPontuacaoEhDescartado() {
        List<String> tokens = TokenizadorTexto.tokenizar("gato ... bola");

        assertEquals(List.of("gato", "bola"), tokens);
    }
}

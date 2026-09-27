package com.missio.fluencia_leitora.cadastros.aluno;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * CAD-15: HistoricoAvaliacaoPortStub sempre retorna false, já que não há
 * avaliações possíveis enquanto a feature `avaliacao` não existir.
 */
class HistoricoAvaliacaoPortStubTest {

    @Test
    void existeAvaliacaoNaoCanceladaRetornaFalseParaQualquerAlunoId() {
        HistoricoAvaliacaoPortStub stub = new HistoricoAvaliacaoPortStub();

        assertFalse(stub.existeAvaliacaoNaoCancelada(1L));
        assertFalse(stub.existeAvaliacaoNaoCancelada(999L));
    }
}

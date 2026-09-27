package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-02/CAD-04: existsByAno detects duplicate years; findBySituacao locates
 * the (at most one) ATIVO year, exercised against real MySQL.
 */
@Transactional
class AnoLetivoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Test
    void existsByAnoRetornaTrueQuandoAnoJaCadastradoEFalseCasoContrario() {
        anoLetivoRepository.save(new AnoLetivo(2030, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 12, 15)));

        assertTrue(anoLetivoRepository.existsByAno(2030));
        assertFalse(anoLetivoRepository.existsByAno(2031));
    }

    @Test
    void findBySituacaoRetornaListaVaziaQuandoNaoHaAnoNaSituacao() {
        AnoLetivo planejado = new AnoLetivo(2032, LocalDate.of(2032, 2, 1), LocalDate.of(2032, 12, 15));
        anoLetivoRepository.save(planejado);

        List<AnoLetivo> ativos = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO);

        assertTrue(ativos.isEmpty());
    }

    @Test
    void findBySituacaoRetornaOAnoQuandoHaUmAtivo() {
        AnoLetivo ativo = new AnoLetivo(2033, LocalDate.of(2033, 2, 1), LocalDate.of(2033, 12, 15));
        ativo.setSituacao(SituacaoAnoLetivo.ATIVO);
        anoLetivoRepository.save(ativo);

        List<AnoLetivo> ativos = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO);

        assertEquals(1, ativos.size());
        assertEquals(2033, ativos.get(0).getAno());
    }
}

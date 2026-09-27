package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-01/CAD-02/CAD-04: existsByAno detects duplicate years; findBySituacao
 * locates the (at most one) ATIVO year; a saved AnoLetivo round-trips all its
 * mapped fields, including the PLANEJADO/ativo defaults, exercised against
 * real MySQL.
 */
@Transactional
class AnoLetivoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Test
    void existsByAnoRetornaTrueQuandoAnoJaCadastradoEFalseCasoContrario() {
        LocalDate dataInicio = LocalDate.of(2030, 2, 1);
        LocalDate dataFim = LocalDate.of(2030, 12, 15);
        AnoLetivo salvo = anoLetivoRepository.save(new AnoLetivo(2030, dataInicio, dataFim));

        assertTrue(anoLetivoRepository.existsByAno(2030));
        assertFalse(anoLetivoRepository.existsByAno(2031));
        assertEquals(dataInicio, salvo.getDataInicio());
        assertEquals(dataFim, salvo.getDataFim());
        assertEquals(SituacaoAnoLetivo.PLANEJADO, salvo.getSituacao());
        assertTrue(salvo.isAtivo());
        assertNotNull(salvo.getCriadoEm());
        assertNotNull(salvo.getAtualizadoEm());
    }

    @Test
    void findBySituacaoRetornaListaVaziaQuandoNaoHaAnoNaSituacao() {
        AnoLetivo planejado = new AnoLetivo(2032, LocalDate.of(2032, 2, 1), LocalDate.of(2032, 12, 15));
        anoLetivoRepository.save(planejado);

        List<AnoLetivo> ativos = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO);

        // Other IT classes sharing this database may leave their own ATIVO
        // rows committed (e.g. AnoLetivoControllerIT's /ativar endpoint), so
        // this asserts the PLANEJADO year itself is absent from ATIVO,
        // instead of assuming the whole table is empty.
        assertFalse(ativos.stream().anyMatch(anoLetivo -> anoLetivo.getAno() == 2032));
    }

    @Test
    void findBySituacaoRetornaOAnoQuandoHaUmAtivo() {
        AnoLetivo ativo = new AnoLetivo(2033, LocalDate.of(2033, 2, 1), LocalDate.of(2033, 12, 15));
        ativo.setSituacao(SituacaoAnoLetivo.ATIVO);
        anoLetivoRepository.save(ativo);

        List<AnoLetivo> ativos = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO);

        assertTrue(ativos.stream().anyMatch(anoLetivo -> anoLetivo.getAno() == 2033));
    }
}

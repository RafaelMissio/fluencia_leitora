package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAD-05/CAD-06: findByAnoLetivoIdAndSerie locates a series' configuration
 * (or nothing when absent); uk_config_ano_serie rejects a second row for the
 * same (ano_letivo_id, serie) pair, exercised against real MySQL.
 */
@Transactional
class ConfiguracaoAvaliacaoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    @Test
    void findByAnoLetivoIdAndSerieRetornaConfiguracaoQuandoExisteEVazioCasoContrario() {
        AnoLetivo anoLetivo = anoLetivoRepository.save(
                new AnoLetivo(2040, LocalDate.of(2040, 2, 1), LocalDate.of(2040, 12, 15)));
        configuracaoAvaliacaoRepository.save(new ConfiguracaoAvaliacao(anoLetivo, 1, 15, 20));

        Optional<ConfiguracaoAvaliacao> encontrada =
                configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(anoLetivo.getId(), 1);
        Optional<ConfiguracaoAvaliacao> vazia =
                configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(anoLetivo.getId(), 2);

        assertTrue(encontrada.isPresent());
        assertEquals(anoLetivo.getId(), encontrada.get().getAnoLetivo().getId());
        assertEquals(1, encontrada.get().getSerie());
        assertEquals(15, encontrada.get().getQuantidadeMinima());
        assertEquals(20, encontrada.get().getQuantidadeMaxima());
        assertNotNull(encontrada.get().getId());
        assertTrue(vazia.isEmpty());
    }

    @Test
    void segundaConfiguracaoComMesmoAnoESerieViolaConstraintUnica() {
        AnoLetivo anoLetivo = anoLetivoRepository.save(
                new AnoLetivo(2041, LocalDate.of(2041, 2, 1), LocalDate.of(2041, 12, 15)));
        configuracaoAvaliacaoRepository.saveAndFlush(new ConfiguracaoAvaliacao(anoLetivo, 1, 15, 20));

        assertThrows(DataIntegrityViolationException.class, () ->
                configuracaoAvaliacaoRepository.saveAndFlush(new ConfiguracaoAvaliacao(anoLetivo, 1, 10, 25)));
    }
}

package com.missio.fluencia_leitora.cadastros.aluno;

import org.springframework.stereotype.Component;

/**
 * Implementação provisória de {@link HistoricoAvaliacaoPort} que sempre
 * retorna {@code false} - não há avaliações possíveis enquanto a feature
 * {@code avaliacao} não existir.
 *
 * <p>Quando a feature {@code avaliacao} for implementada, ela deve fornecer
 * um bean {@code @Primary} real que consulta sua própria tabela, substituindo
 * este stub sem que os consumidores de {@link HistoricoAvaliacaoPort}
 * precisem mudar.
 */
@Component
public class HistoricoAvaliacaoPortStub implements HistoricoAvaliacaoPort {

    @Override
    public boolean existeAvaliacaoNaoCancelada(Long alunoId) {
        return false;
    }
}

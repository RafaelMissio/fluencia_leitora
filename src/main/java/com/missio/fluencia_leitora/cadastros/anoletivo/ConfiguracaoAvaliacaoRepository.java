package com.missio.fluencia_leitora.cadastros.anoletivo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoAvaliacaoRepository extends JpaRepository<ConfiguracaoAvaliacao, Long> {

    Optional<ConfiguracaoAvaliacao> findByAnoLetivoIdAndSerie(Long anoLetivoId, int serie);
}

package com.missio.fluencia_leitora.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoProgramadaRepository extends JpaRepository<AvaliacaoProgramada, Long> {

    List<AvaliacaoProgramada> findByAnoLetivoIdAndSerieAndAtivaTrueOrderByIdAsc(Long anoLetivoId, int serie);

    List<AvaliacaoProgramada> findByAnoLetivoIdAndAtivaTrueOrderBySerieAscIdAsc(Long anoLetivoId);
}

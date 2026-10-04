package com.missio.fluencia_leitora.cadastros.anoletivo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnoLetivoRepository extends JpaRepository<AnoLetivo, Long> {

    boolean existsByAno(int ano);

    List<AnoLetivo> findBySituacao(SituacaoAnoLetivo situacao);

    List<AnoLetivo> findByAtivoTrueOrderByAnoDesc();
}

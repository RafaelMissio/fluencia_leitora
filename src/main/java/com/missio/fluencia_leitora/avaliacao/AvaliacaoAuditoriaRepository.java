package com.missio.fluencia_leitora.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** AVA-26: registros de auditoria de uma avaliação, em ordem cronológica. */
public interface AvaliacaoAuditoriaRepository extends JpaRepository<AvaliacaoAuditoria, Long> {

    List<AvaliacaoAuditoria> findByAvaliacaoIdOrderByDataHoraAsc(Long avaliacaoId);
}

package com.missio.fluencia_leitora.regrasclassificacao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Consultas por série (ativas, para classificar/consultar) e histórico
 * completo, mais o lock pessimista da substituição (design.md, Components).
 */
public interface RegraClassificacaoRepository extends JpaRepository<RegraClassificacao, Long> {

    List<RegraClassificacao> findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(int serie);

    /**
     * Usada só dentro de {@code RegraClassificacaoService.substituir}:
     * serializa dois {@code PUT} concorrentes na mesma série (spec: "o
     * último a confirmar vence") via {@code SELECT ... FOR UPDATE}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegraClassificacao r where r.serie = :serie and r.ativo = true")
    List<RegraClassificacao> buscarAtivasParaAtualizarComLock(@Param("serie") int serie);

    /**
     * Todas as faixas (ativas e inativas) da série, com o grupo corrente
     * ({@code alterado_em IS NULL}) primeiro, depois {@code alterado_em
     * DESC} (design.md, Tech Decisions: MySQL ordena NULL como o menor
     * valor por padrão, por isso o {@code CASE} explícito em vez de um
     * {@code ORDER BY alterado_em DESC} simples).
     */
    @Query(value = "SELECT * FROM regra_classificacao WHERE serie = :serie "
            + "ORDER BY (alterado_em IS NULL) DESC, alterado_em DESC", nativeQuery = true)
    List<RegraClassificacao> buscarHistoricoPorSerie(@Param("serie") int serie);
}

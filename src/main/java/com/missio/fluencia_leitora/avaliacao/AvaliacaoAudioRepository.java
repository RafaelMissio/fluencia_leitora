package com.missio.fluencia_leitora.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * AVA-27..AVA-32: referência do áudio (única por avaliação). {@code
 * findAvaliacaoIdByAvaliacaoIdIn} alimenta a feature {@code
 * historicoevolucao} (HIST-02): marca {@code temAudio} numa página de
 * histórico com 1 query {@code IN} em vez de N+1 (design.md, Tech
 * Decisions).
 *
 * <p>SPEC_DEVIATION: design.md descreve {@code findAvaliacaoIdByAvaliacaoIdIn}
 * como método derivado, mas Spring Data JPA não reconhece o nome de
 * propriedade antes de {@code By} como projeção (só {@code Distinct}/{@code
 * First}/{@code Top} são keywords aí) - a versão puramente derivada tentava
 * devolver a entidade inteira e falhava ao converter para {@code List<Long>}.
 * Reason: {@code @Query} explícito projetando {@code a.avaliacao.id} produz
 * exatamente o {@code SELECT} pretendido, mantendo a mesma assinatura.
 */
public interface AvaliacaoAudioRepository extends JpaRepository<AvaliacaoAudio, Long> {

    Optional<AvaliacaoAudio> findByAvaliacaoId(Long avaliacaoId);

    boolean existsByAvaliacaoId(Long avaliacaoId);

    @Query("select a.avaliacao.id from AvaliacaoAudio a where a.avaliacao.id in :avaliacaoIds")
    List<Long> findAvaliacaoIdByAvaliacaoIdIn(@Param("avaliacaoIds") List<Long> avaliacaoIds);
}

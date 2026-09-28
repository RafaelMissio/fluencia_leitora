package com.missio.fluencia_leitora.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** AVA-27..AVA-32: referência do áudio (única por avaliação). */
public interface AvaliacaoAudioRepository extends JpaRepository<AvaliacaoAudio, Long> {

    Optional<AvaliacaoAudio> findByAvaliacaoId(Long avaliacaoId);

    boolean existsByAvaliacaoId(Long avaliacaoId);
}

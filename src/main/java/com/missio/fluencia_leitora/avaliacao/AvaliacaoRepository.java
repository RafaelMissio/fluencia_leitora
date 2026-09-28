package com.missio.fluencia_leitora.avaliacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * {@code existsByAlunoIdAndStatusNot} alimenta o {@code HistoricoAvaliacaoPort}
 * real (aluno com avaliação não cancelada); {@code
 * findByStatusAndUltimaAtividadeEmBefore} alimenta a rotina de hora em hora
 * que finaliza avaliações {@code EM_ANDAMENTO} paradas há mais de 24h
 * (AVA-17) - filtra por {@code ultimaAtividadeEm}, não {@code iniciadoEm}
 * (design.md, Tech Decisions).
 */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsByAlunoIdAndStatusNot(Long alunoId, StatusAvaliacao status);

    List<Avaliacao> findByStatusAndUltimaAtividadeEmBefore(StatusAvaliacao status, Instant limite);
}

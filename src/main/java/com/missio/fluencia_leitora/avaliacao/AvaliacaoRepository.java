package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * {@code existsByAlunoIdAndStatusNot} alimenta o {@code HistoricoAvaliacaoPort}
 * real (aluno com avaliação não cancelada); {@code
 * findByStatusAndUltimaAtividadeEmBefore} alimenta a rotina de hora em hora
 * que finaliza avaliações {@code EM_ANDAMENTO} paradas há mais de 24h
 * (AVA-17) - filtra por {@code ultimaAtividadeEm}, não {@code iniciadoEm}
 * (design.md, Tech Decisions).
 *
 * <p>{@code buscarHistorico}/{@code buscarFinalizadasPorAnoETipo}/{@code
 * buscarFinalizadasPorTipo} alimentam a feature {@code historicoevolucao}
 * (HIST-01, HIST-03..06): a primeira devolve a página de histórico com
 * filtros opcionais; as outras duas devolvem todas as {@code FINALIZADA} de
 * um escopo, já ordenadas para que o service agrupe "mais recente por
 * grupo" em memória (design.md, Tech Decisions).
 */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsByAlunoIdAndStatusNot(Long alunoId, StatusAvaliacao status);

    List<Avaliacao> findByAlunoIdAndAtivaTrueAndStatusInOrderByDataAvaliacaoAscIdAsc(Long alunoId, List<StatusAvaliacao> status);

    List<Avaliacao> findByAlunoIdAndProgramadaIdNotNullAndStatusNot(Long alunoId, StatusAvaliacao status);

    List<Avaliacao> findByAtivaTrueAndIdOrAtivaTrueAndRefeitaDeId(Long id, Long refeitaDeId);

    /** Quantas avaliações (a original e as refeitas, exceto canceladas) existem na cadeia de {@code raizId}. */
    @Query("select count(a) from Avaliacao a where (a.id = :raizId or a.refeitaDeId = :raizId) "
            + "and a.status <> com.missio.fluencia_leitora.avaliacao.StatusAvaliacao.CANCELADA")
    long contarCadeia(@Param("raizId") Long raizId);

    /** Quantas avaliações da cadeia de {@code raizId} já estão finalizadas. */
    @Query("select count(a) from Avaliacao a where (a.id = :raizId or a.refeitaDeId = :raizId) "
            + "and a.status = com.missio.fluencia_leitora.avaliacao.StatusAvaliacao.FINALIZADA")
    long contarFinalizadasCadeia(@Param("raizId") Long raizId);

    List<Avaliacao> findByStatusAndUltimaAtividadeEmBefore(StatusAvaliacao status, Instant limite);

    @Query("select a from Avaliacao a where a.aluno.id = :alunoId and a.status = :status "
            + "and (:anoLetivoId is null or a.anoLetivo.id = :anoLetivoId) "
            + "and (:tipoLeitura is null or a.tipoLeitura = :tipoLeitura) "
            + "and (:cicloId is null or a.ciclo.id = :cicloId) "
            + "order by a.dataAvaliacao desc, a.finalizadoEm desc")
    Page<Avaliacao> buscarHistorico(
            @Param("alunoId") Long alunoId,
            @Param("status") StatusAvaliacao status,
            @Param("anoLetivoId") Long anoLetivoId,
            @Param("tipoLeitura") TipoLeituraCodigo tipoLeitura,
            @Param("cicloId") Long cicloId,
            Pageable pageable);

    @Query("select a from Avaliacao a where a.aluno.id = :alunoId and a.status = :status "
            + "and a.anoLetivo.id = :anoLetivoId and a.tipoLeitura = :tipoLeitura "
            + "order by a.ciclo.id, a.finalizadoEm desc")
    List<Avaliacao> buscarFinalizadasPorAnoETipo(
            @Param("alunoId") Long alunoId,
            @Param("status") StatusAvaliacao status,
            @Param("anoLetivoId") Long anoLetivoId,
            @Param("tipoLeitura") TipoLeituraCodigo tipoLeitura);

    @Query("select a from Avaliacao a where a.aluno.id = :alunoId and a.status = :status "
            + "and a.tipoLeitura = :tipoLeitura "
            + "order by a.anoLetivo.ano, a.ciclo.id, a.finalizadoEm desc")
    List<Avaliacao> buscarFinalizadasPorTipo(
            @Param("alunoId") Long alunoId,
            @Param("status") StatusAvaliacao status,
            @Param("tipoLeitura") TipoLeituraCodigo tipoLeitura);
}

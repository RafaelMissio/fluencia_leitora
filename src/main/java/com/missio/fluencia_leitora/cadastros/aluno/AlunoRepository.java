package com.missio.fluencia_leitora.cadastros.aluno;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * CAD-16: busca de aluno por nome, sem diferenciar maiúsculas nem acentos.
 *
 * <p>SPEC_DEVIATION: a busca usa {@code LIKE} puro (sem {@code IgnoreCase}
 * nem {@code LOWER(...)}), para que a comparação use a collation
 * {@code utf8mb4_0900_ai_ci} definida na coluna {@code aluno.nome} (migração
 * V4, ver design.md "Risks &amp; Concerns"). Um método derivado com o sufixo
 * {@code IgnoreCase} geraria {@code LOWER(nome) LIKE LOWER(?)}, que resolve
 * maiúsculas/minúsculas via SQL {@code LOWER()} mas não remove acentos -
 * derrotando o propósito da collation.
 *
 * <p>SPEC_DEVIATION: o filtro por professor usa {@code nativeQuery} com um
 * JOIN direto nas tabelas {@code matricula}/{@code ano_letivo} (em vez de
 * JPQL sobre uma entidade {@code Matricula}), porque a entidade
 * {@code Matricula} só é criada na T21 - esta task (T20) depende apenas da
 * T19 (migração). O filtro reproduz CAD-16 por completo: só a matrícula do
 * ano letivo {@code ATIVO} do professor informado conta.
 */
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    @Query(
            value = "SELECT * FROM aluno WHERE nome LIKE CONCAT('%', :termo, '%') ORDER BY nome",
            countQuery = "SELECT count(*) FROM aluno WHERE nome LIKE CONCAT('%', :termo, '%')",
            nativeQuery = true)
    Page<Aluno> buscarPorNome(@Param("termo") String termo, Pageable pageable);

    @Query(
            value = "SELECT DISTINCT a.* FROM aluno a "
                    + "JOIN matricula m ON m.aluno_id = a.id "
                    + "JOIN ano_letivo al ON al.id = m.ano_letivo_id "
                    + "WHERE a.nome LIKE CONCAT('%', :termo, '%') "
                    + "AND m.professor_id = :professorId "
                    + "AND al.situacao = 'ATIVO' "
                    + "ORDER BY a.nome",
            countQuery = "SELECT count(DISTINCT a.id) FROM aluno a "
                    + "JOIN matricula m ON m.aluno_id = a.id "
                    + "JOIN ano_letivo al ON al.id = m.ano_letivo_id "
                    + "WHERE a.nome LIKE CONCAT('%', :termo, '%') "
                    + "AND m.professor_id = :professorId "
                    + "AND al.situacao = 'ATIVO'",
            nativeQuery = true)
    Page<Aluno> buscarPorNomeEProfessor(
            @Param("termo") String termo, @Param("professorId") Long professorId, Pageable pageable);
}

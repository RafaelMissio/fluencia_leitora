package com.missio.fluencia_leitora.bancopalavras;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PAL-10: {@code buscarResumo} filtra por série + tipo de leitura, só listas
 * ativas, e conta os itens via {@code COUNT}/{@code GROUP BY} (projeção
 * Spring Data), sem carregar a coleção {@code itens} inteira - ver
 * design.md, Tech Decisions.
 */
public interface ListaPalavrasRepository extends JpaRepository<ListaPalavras, Long> {

    @Query("select l.id as id, l.nome as nome, count(i) as quantidadePalavras "
            + "from ListaPalavras l join l.itens i "
            + "where l.serie = :serie and l.tipoLeitura = :tipo and l.ativo = true "
            + "group by l.id, l.nome")
    List<ListaPalavrasResumoProjection> buscarResumo(@Param("serie") int serie, @Param("tipo") TipoLeituraCodigo tipo);
}

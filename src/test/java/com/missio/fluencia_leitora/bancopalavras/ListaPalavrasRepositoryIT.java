package com.missio.fluencia_leitora.bancopalavras;

import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PAL-10: {@code buscarResumo} filtra por série + tipo de leitura, só
 * devolve listas ativas, e conta os itens via {@code COUNT}/{@code
 * GROUP BY}, exercitado contra MySQL real.
 */
@Transactional
class ListaPalavrasRepositoryIT extends IntegrationTestBase {

    @Autowired
    private ListaPalavrasRepository listaPalavrasRepository;

    private ListaPalavras novaLista(String nome, int serie, TipoLeituraCodigo tipoLeitura, int quantidadeItens) {
        ListaPalavras lista = new ListaPalavras(nome, serie, tipoLeitura, null, null);
        for (int i = 1; i <= quantidadeItens; i++) {
            lista.adicionarItem("palavra" + i, TipoPalavra.CANONICA, i);
        }
        return listaPalavrasRepository.save(lista);
    }

    @Test
    void buscarResumoRetornaListaAtivaDaSerieETipoComQuantidadeDeItensCorreta() {
        ListaPalavras lista = novaLista("Lista Série 2 Pseudopalavra", 2, TipoLeituraCodigo.PSEUDOPALAVRA, 3);

        List<ListaPalavrasResumoProjection> resumo =
                listaPalavrasRepository.buscarResumo(2, TipoLeituraCodigo.PSEUDOPALAVRA);

        assertEquals(1, resumo.size());
        assertEquals(lista.getId(), resumo.get(0).getId());
        assertEquals("Lista Série 2 Pseudopalavra", resumo.get(0).getNome());
        assertEquals(3L, resumo.get(0).getQuantidadePalavras());
    }

    @Test
    void buscarResumoNaoRetornaListaInativaMesmoCasandoSerieETipo() {
        ListaPalavras lista = novaLista("Lista Inativada", 3, TipoLeituraCodigo.PALAVRA, 2);
        lista.setAtivo(false);
        listaPalavrasRepository.save(lista);

        List<ListaPalavrasResumoProjection> resumo = listaPalavrasRepository.buscarResumo(3, TipoLeituraCodigo.PALAVRA);

        assertTrue(resumo.isEmpty());
    }

    @Test
    void buscarResumoNaoRetornaListaDeSerieDiferente() {
        novaLista("Lista Série 4", 4, TipoLeituraCodigo.PALAVRA, 2);

        List<ListaPalavrasResumoProjection> resumo = listaPalavrasRepository.buscarResumo(5, TipoLeituraCodigo.PALAVRA);

        assertTrue(resumo.isEmpty());
    }

    @Test
    void buscarResumoNaoRetornaListaDeTipoDeLeituraDiferente() {
        novaLista("Lista Palavra Série 1", 1, TipoLeituraCodigo.PALAVRA, 2);

        List<ListaPalavrasResumoProjection> resumo =
                listaPalavrasRepository.buscarResumo(1, TipoLeituraCodigo.PSEUDOPALAVRA);

        assertTrue(resumo.isEmpty());
    }
}

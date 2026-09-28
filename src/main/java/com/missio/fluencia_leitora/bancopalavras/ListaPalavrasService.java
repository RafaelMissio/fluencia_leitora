package com.missio.fluencia_leitora.bancopalavras;

import com.missio.fluencia_leitora.bancopalavras.dto.AtualizarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.CriarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.ItemPalavraRequest;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.texto.TokenizadorTexto;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * PAL-01..PAL-12: validações cruzadas (formato já coberto por Bean
 * Validation nos DTOs - T8), duplicidade, série×canônica e compatibilidade
 * conteúdo×tipo, além da tokenização de texto curto (PAL-07/PAL-08) e a
 * orquestração transacional de {@code criar}/{@code atualizar}.
 */
@Service
public class ListaPalavrasService {

    private static final int SERIE_PROIBE_NAO_CANONICA = 1;
    private static final int LIMITE_TOKENS_TEXTO_CURTO = 200;

    private final ListaPalavrasRepository repository;

    public ListaPalavrasService(ListaPalavrasRepository repository) {
        this.repository = repository;
    }

    /** PAL-01/PAL-02/PAL-03/PAL-04/PAL-05/PAL-07/PAL-08/PAL-09/PAL-12. */
    @Transactional
    public ListaPalavras criar(CriarListaPalavrasRequest request) {
        List<ItemDados> itensDados = validarEMontarItens(
                request.tipoLeitura(), request.texto(), request.tipoPalavra(), request.itens(), request.serie());

        ListaPalavras lista = new ListaPalavras(
                request.nome(), request.serie(), request.tipoLeitura(), request.tipoPalavra(), request.texto());
        itensDados.forEach(item -> lista.adicionarItem(item.palavra(), item.tipoPalavra(), item.ordem()));

        return repository.save(lista);
    }

    /**
     * PAL-06/PAL-12: reaplica as mesmas validações de {@link #criar} sobre o
     * novo conteúdo e substitui a coleção de itens (o {@code orphanRemoval}
     * de {@link ListaPalavras#substituirItens} cuida da exclusão dos
     * antigos). O {@code version} do request precisa bater com o persistido
     * - divergente dispara {@link ObjectOptimisticLockingFailureException}
     * (409 {@code CONFLITO_DE_VERSAO} via {@code GlobalExceptionHandler}).
     */
    @Transactional
    public ListaPalavras atualizar(Long id, AtualizarListaPalavrasRequest request) {
        ListaPalavras lista = buscarExistente(id);

        if (!Objects.equals(lista.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException("lista_palavras", id);
        }

        List<ItemDados> itensDados = validarEMontarItens(
                request.tipoLeitura(), request.texto(), request.tipoPalavra(), request.itens(), request.serie());

        lista.setNome(request.nome());
        lista.setSerie(request.serie());
        lista.setTipoLeitura(request.tipoLeitura());
        lista.setTipoPalavra(request.tipoPalavra());
        lista.setTexto(request.texto());

        List<ItemListaPalavras> novosItens = itensDados.stream()
                .map(item -> new ItemListaPalavras(lista, item.palavra(), item.tipoPalavra(), item.ordem()))
                .toList();
        lista.substituirItens(novosItens);

        return repository.save(lista);
    }

    /** LISTA_NAO_ENCONTRADA (404) quando o id não existe - inclui listas inativas. */
    private ListaPalavras buscarExistente(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "LISTA_NAO_ENCONTRADA", "Lista de palavras não encontrada"));
    }

    /**
     * Valida conteúdo×tipo (PAL-12), monta os itens (tokenizando o texto
     * quando {@code TEXTO_CURTO}), valida série×canônica (PAL-02/PAL-09) e
     * duplicidade (PAL-04, só para PALAVRA/PSEUDOPALAVRA). Reaproveitado por
     * {@code atualizar} (T11).
     */
    private List<ItemDados> validarEMontarItens(
            TipoLeituraCodigo tipoLeitura,
            String texto,
            TipoPalavra tipoPalavraLista,
            List<ItemPalavraRequest> itensRequest,
            int serie) {
        validarConteudoCompativel(tipoLeitura, texto, itensRequest);

        List<ItemDados> itensDados = tipoLeitura == TipoLeituraCodigo.TEXTO_CURTO
                ? montarItensDeTexto(texto, tipoPalavraLista)
                : montarItensDePalavras(itensRequest);

        validarSerieCanonica(serie, itensDados);

        if (tipoLeitura != TipoLeituraCodigo.TEXTO_CURTO) {
            validarDuplicidade(itensDados);
        }

        return itensDados;
    }

    /**
     * PAL-12: o conteúdo enviado precisa bater com {@code tipoLeitura} -
     * {@code TEXTO_CURTO} exige {@code texto} e proíbe {@code itens};
     * {@code PALAVRA}/{@code PSEUDOPALAVRA} exigem {@code itens} e proíbem
     * {@code texto} (spec.md, Edge Cases).
     */
    private void validarConteudoCompativel(TipoLeituraCodigo tipoLeitura, String texto, List<ItemPalavraRequest> itens) {
        boolean temTexto = texto != null && !texto.isBlank();
        boolean temItens = itens != null && !itens.isEmpty();

        boolean incompativel = tipoLeitura == TipoLeituraCodigo.TEXTO_CURTO
                ? (!temTexto || temItens)
                : (!temItens || temTexto);

        if (incompativel) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CONTEUDO_INCOMPATIVEL_COM_TIPO",
                    "O conteúdo enviado não é compatível com o tipo de leitura da lista");
        }
    }

    /** PAL-07/PAL-08: tokeniza o texto e aplica o `tipoPalavra` da lista a cada token, na ordem 1..n. */
    private List<ItemDados> montarItensDeTexto(String texto, TipoPalavra tipoPalavraLista) {
        List<String> tokens = TokenizadorTexto.tokenizar(texto);
        if (tokens.size() > LIMITE_TOKENS_TEXTO_CURTO) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDACAO_INVALIDA",
                    "O texto gera mais de " + LIMITE_TOKENS_TEXTO_CURTO + " palavras");
        }

        List<ItemDados> itens = new ArrayList<>();
        int ordem = 1;
        for (String token : tokens) {
            itens.add(new ItemDados(token, tipoPalavraLista, ordem++));
        }
        return itens;
    }

    /** PAL-01: cada item do payload vira um {@link ItemDados} na ordem enviada (1..n). */
    private List<ItemDados> montarItensDePalavras(List<ItemPalavraRequest> itensRequest) {
        List<ItemDados> itens = new ArrayList<>();
        int ordem = 1;
        for (ItemPalavraRequest itemRequest : itensRequest) {
            itens.add(new ItemDados(itemRequest.palavra(), itemRequest.tipoPalavra(), ordem++));
        }
        return itens;
    }

    /** PAL-02/PAL-09: 1º ano não aceita item NAO_CANONICA; a exceção carrega as posições inválidas (AD-008). */
    private void validarSerieCanonica(int serie, List<ItemDados> itens) {
        if (serie != SERIE_PROIBE_NAO_CANONICA) {
            return;
        }

        List<Integer> posicoesInvalidas = itens.stream()
                .filter(item -> item.tipoPalavra() == TipoPalavra.NAO_CANONICA)
                .map(ItemDados::ordem)
                .toList();

        if (!posicoesInvalidas.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NAO_CANONICA_PROIBIDA_1_ANO",
                    "Lista do 1º ano não pode conter palavra não canônica",
                    java.util.Map.of("posicoes", posicoesInvalidas));
        }
    }

    /** PAL-04: comparação case-insensitive após trim; duplicidade é permitida em TEXTO_CURTO. */
    private void validarDuplicidade(List<ItemDados> itens) {
        Set<String> vistas = new HashSet<>();
        for (ItemDados item : itens) {
            String chave = item.palavra().trim().toLowerCase();
            if (!vistas.add(chave)) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "PALAVRA_DUPLICADA", "A lista contém palavras repetidas");
            }
        }
    }

    /** Representação intermediária de um item antes de virar {@link ItemListaPalavras}. */
    private record ItemDados(String palavra, TipoPalavra tipoPalavra, int ordem) {
    }
}

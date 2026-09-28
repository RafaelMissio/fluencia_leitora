package com.missio.fluencia_leitora.bancopalavras;

import com.missio.fluencia_leitora.bancopalavras.dto.AtualizarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.CriarListaPalavrasRequest;
import com.missio.fluencia_leitora.bancopalavras.dto.ItemPalavraRequest;
import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAL-01..PAL-12: {@link ListaPalavrasService#criar} - validações cruzadas
 * (conteúdo×tipo, série×canônica, duplicidade) e tokenização de texto curto.
 */
@ExtendWith(MockitoExtension.class)
class ListaPalavrasServiceTest {

    @Mock
    private ListaPalavrasRepository repository;

    private ListaPalavrasService service() {
        return new ListaPalavrasService(repository);
    }

    private void mockSalvaIgual() {
        when(repository.save(any(ListaPalavras.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void criarListaPalavraValidaGravaItensNaOrdemEnviada() {
        mockSalvaIgual();
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Lista de Palavras", 2, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(
                        new ItemPalavraRequest("gato", TipoPalavra.CANONICA),
                        new ItemPalavraRequest("bola", TipoPalavra.CANONICA)));

        ListaPalavras criada = service().criar(request);

        assertEquals(2, criada.getItens().size());
        assertEquals("gato", criada.getItens().get(0).getPalavra());
        assertEquals(1, criada.getItens().get(0).getOrdem());
        assertEquals("bola", criada.getItens().get(1).getPalavra());
        assertEquals(2, criada.getItens().get(1).getOrdem());
    }

    @Test
    void criarListaPseudopalavraValidaGrava() {
        mockSalvaIgual();
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Lista de Pseudopalavras", 3, TipoLeituraCodigo.PSEUDOPALAVRA, null, null,
                List.of(new ItemPalavraRequest("blicar", TipoPalavra.NAO_CANONICA)));

        ListaPalavras criada = service().criar(request);

        assertEquals(1, criada.getItens().size());
        assertEquals(TipoPalavra.NAO_CANONICA, criada.getItens().get(0).getTipoPalavra());
    }

    @Test
    void criarSerie1ComItemNaoCanonicaLanca422ComPosicoesCorretas() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Lista 1º ano", 1, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(
                        new ItemPalavraRequest("gato", TipoPalavra.NAO_CANONICA),
                        new ItemPalavraRequest("bola", TipoPalavra.CANONICA),
                        new ItemPalavraRequest("casa", TipoPalavra.NAO_CANONICA)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("NAO_CANONICA_PROIBIDA_1_ANO", exception.getCode());
        assertEquals(List.of(1, 3), exception.getDetails().get("posicoes"));
        verify(repository, never()).save(any());
    }

    @Test
    void criarComPalavraDuplicadaCaseInsensitiveAposTrimLanca422() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Lista Duplicada", 2, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(
                        new ItemPalavraRequest("Gato", TipoPalavra.CANONICA),
                        new ItemPalavraRequest("gato", TipoPalavra.CANONICA)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("PALAVRA_DUPLICADA", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void criarTextoCurtoComItensEnviadosLanca422ConteudoIncompativel() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Texto Errado", 2, TipoLeituraCodigo.TEXTO_CURTO, TipoPalavra.CANONICA, null,
                List.of(new ItemPalavraRequest("gato", TipoPalavra.CANONICA)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("CONTEUDO_INCOMPATIVEL_COM_TIPO", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void criarPalavraComTextoEnviadoLanca422ConteudoIncompativel() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Palavra Com Texto", 2, TipoLeituraCodigo.PALAVRA, null, "texto indevido",
                List.of(new ItemPalavraRequest("gato", TipoPalavra.CANONICA)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals("CONTEUDO_INCOMPATIVEL_COM_TIPO", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void criarPalavraSemItensLanca422ConteudoIncompativel() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Palavra Sem Itens", 2, TipoLeituraCodigo.PALAVRA, null, null, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals("CONTEUDO_INCOMPATIVEL_COM_TIPO", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void criarTextoCurtoValidoTokenizaGerandoOrdemEPreservandoTipoPalavra() {
        mockSalvaIgual();
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Texto Curto", 2, TipoLeituraCodigo.TEXTO_CURTO, TipoPalavra.CANONICA, "O gato, a bola.", null);

        ListaPalavras criada = service().criar(request);

        assertEquals(4, criada.getItens().size());
        assertEquals("O", criada.getItens().get(0).getPalavra());
        assertEquals(1, criada.getItens().get(0).getOrdem());
        assertEquals("gato", criada.getItens().get(1).getPalavra());
        assertEquals(2, criada.getItens().get(1).getOrdem());
        assertEquals("a", criada.getItens().get(2).getPalavra());
        assertEquals(3, criada.getItens().get(2).getOrdem());
        assertEquals("bola", criada.getItens().get(3).getPalavra());
        assertEquals(4, criada.getItens().get(3).getOrdem());
        criada.getItens().forEach(item -> assertEquals(TipoPalavra.CANONICA, item.getTipoPalavra()));
    }

    @Test
    void criarTextoCurtoComMaisDe200TokensLanca422() {
        List<String> palavras = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            palavras.add("p" + i);
        }
        String texto = String.join(" ", palavras);
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Texto Longo", 2, TipoLeituraCodigo.TEXTO_CURTO, TipoPalavra.CANONICA, texto, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("VALIDACAO_INVALIDA", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void criarSerie1TextoCurtoNaoCanonicaLanca422NaoCanonicaProibida1Ano() {
        CriarListaPalavrasRequest request = new CriarListaPalavrasRequest(
                "Texto 1º ano", 1, TipoLeituraCodigo.TEXTO_CURTO, TipoPalavra.NAO_CANONICA, "O gato corre.", null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service().criar(request));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("NAO_CANONICA_PROIBIDA_1_ANO", exception.getCode());
        assertEquals(List.of(1, 2, 3), exception.getDetails().get("posicoes"));
        verify(repository, never()).save(any());
    }

    private ListaPalavras listaExistente(Long id, long version, String... palavras) {
        ListaPalavras lista = new ListaPalavras("Lista Original", 2, TipoLeituraCodigo.PALAVRA, null, null);
        ReflectionTestUtils.setField(lista, "id", id);
        ReflectionTestUtils.setField(lista, "version", version);
        int ordem = 1;
        for (String palavra : palavras) {
            lista.adicionarItem(palavra, TipoPalavra.CANONICA, ordem++);
        }
        return lista;
    }

    @Test
    void atualizarComVersaoCorretaTrocaNomeEItensReexecutandoValidacoes() {
        ListaPalavras existente = listaExistente(10L, 0L, "antiga");
        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        mockSalvaIgual();
        AtualizarListaPalavrasRequest request = new AtualizarListaPalavrasRequest(
                "Lista Editada", 2, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(
                        new ItemPalavraRequest("novaPalavra", TipoPalavra.CANONICA),
                        new ItemPalavraRequest("outraPalavra", TipoPalavra.CANONICA)),
                0L);

        ListaPalavras atualizada = service().atualizar(10L, request);

        assertEquals("Lista Editada", atualizada.getNome());
        assertEquals(2, atualizada.getItens().size());
        assertEquals("novaPalavra", atualizada.getItens().get(0).getPalavra());
        assertEquals(1, atualizada.getItens().get(0).getOrdem());
        assertEquals("outraPalavra", atualizada.getItens().get(1).getPalavra());
        assertEquals(2, atualizada.getItens().get(1).getOrdem());
    }

    @Test
    void atualizarComVersaoDivergenteLancaObjectOptimisticLockingFailureException() {
        ListaPalavras existente = listaExistente(11L, 5L, "antiga");
        when(repository.findById(11L)).thenReturn(Optional.of(existente));
        AtualizarListaPalavrasRequest request = new AtualizarListaPalavrasRequest(
                "Lista Editada", 2, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(new ItemPalavraRequest("palavra", TipoPalavra.CANONICA)),
                0L);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> service().atualizar(11L, request));

        verify(repository, never()).save(any());
    }

    @Test
    void atualizarReaplicaValidacaoSerieCanonica() {
        ListaPalavras existente = listaExistente(12L, 0L, "antiga");
        when(repository.findById(12L)).thenReturn(Optional.of(existente));
        AtualizarListaPalavrasRequest request = new AtualizarListaPalavrasRequest(
                "Lista 1º ano", 1, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(new ItemPalavraRequest("naoCanonica", TipoPalavra.NAO_CANONICA)),
                0L);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service().atualizar(12L, request));

        assertEquals("NAO_CANONICA_PROIBIDA_1_ANO", exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarSubstituiItensAntigosPorNovosComOrdemCorreta() {
        ListaPalavras existente = listaExistente(13L, 0L, "velha1", "velha2");
        when(repository.findById(13L)).thenReturn(Optional.of(existente));
        mockSalvaIgual();
        AtualizarListaPalavrasRequest request = new AtualizarListaPalavrasRequest(
                "Lista Editada", 2, TipoLeituraCodigo.PALAVRA, null, null,
                List.of(new ItemPalavraRequest("nova", TipoPalavra.CANONICA)),
                0L);

        ListaPalavras atualizada = service().atualizar(13L, request);

        assertEquals(1, atualizada.getItens().size());
        assertEquals("nova", atualizada.getItens().get(0).getPalavra());
        assertTrue(atualizada.getItens().stream().noneMatch(item -> item.getPalavra().startsWith("velha")));
    }
}

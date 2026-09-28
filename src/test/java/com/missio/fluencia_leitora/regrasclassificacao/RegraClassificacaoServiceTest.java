package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.regrasclassificacao.dto.FaixaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REG-01..REG-13/REG-15 (REG-14 deferido, ver spec.md): validação 1:1 dos
 * branches de {@link RegraClassificacaoService}.
 */
@ExtendWith(MockitoExtension.class)
class RegraClassificacaoServiceTest {

    @Mock
    private RegraClassificacaoRepository repository;

    @Mock
    private ContextoUsuarioPort contextoUsuarioPort;

    private RegraClassificacaoService service() {
        return new RegraClassificacaoService(repository, contextoUsuarioPort);
    }

    /** V7 seed: faixas ativas da série 1 (design.md, Data Models). */
    private static List<RegraClassificacao> faixasSerie1() {
        return List.of(
                new RegraClassificacao(1, 0, 3, Fase.PRE_LEITOR, 1),
                new RegraClassificacao(1, 4, 5, Fase.PRE_LEITOR, 2),
                new RegraClassificacao(1, 6, 6, Fase.PRE_LEITOR, 3),
                new RegraClassificacao(1, 7, 7, Fase.PRE_LEITOR, 4),
                new RegraClassificacao(1, 8, 11, Fase.LEITOR_INICIANTE, null),
                new RegraClassificacao(1, 12, null, Fase.LEITOR_FLUENTE, null));
    }

    /** V7 seed: faixas ativas de qualquer série de 2 a 5 (mesmas faixas, design.md, Data Models). */
    private static List<RegraClassificacao> faixasSerie2a5(int serie) {
        return List.of(
                new RegraClassificacao(serie, 0, 4, Fase.PRE_LEITOR, 1),
                new RegraClassificacao(serie, 5, 7, Fase.PRE_LEITOR, 2),
                new RegraClassificacao(serie, 8, 9, Fase.PRE_LEITOR, 3),
                new RegraClassificacao(serie, 10, 11, Fase.PRE_LEITOR, 4),
                new RegraClassificacao(serie, 12, 30, Fase.LEITOR_INICIANTE, null),
                new RegraClassificacao(serie, 31, null, Fase.LEITOR_FLUENTE, null));
    }

    /** spec.md, AC1 (P1: Classificar por série e acertos). */
    private static Stream<Arguments> ac1Serie1() {
        return Stream.of(
                Arguments.of(0, Fase.PRE_LEITOR, 1),
                Arguments.of(3, Fase.PRE_LEITOR, 1),
                Arguments.of(4, Fase.PRE_LEITOR, 2),
                Arguments.of(5, Fase.PRE_LEITOR, 2),
                Arguments.of(6, Fase.PRE_LEITOR, 3),
                Arguments.of(7, Fase.PRE_LEITOR, 4),
                Arguments.of(8, Fase.LEITOR_INICIANTE, null),
                Arguments.of(11, Fase.LEITOR_INICIANTE, null),
                Arguments.of(12, Fase.LEITOR_FLUENTE, null),
                Arguments.of(20, Fase.LEITOR_FLUENTE, null));
    }

    /** spec.md, AC2 (P1: Classificar por série e acertos). */
    private static Stream<Arguments> ac2Serie2a5() {
        return Stream.of(
                Arguments.of(0, Fase.PRE_LEITOR, 1),
                Arguments.of(4, Fase.PRE_LEITOR, 1),
                Arguments.of(5, Fase.PRE_LEITOR, 2),
                Arguments.of(7, Fase.PRE_LEITOR, 2),
                Arguments.of(8, Fase.PRE_LEITOR, 3),
                Arguments.of(9, Fase.PRE_LEITOR, 3),
                Arguments.of(10, Fase.PRE_LEITOR, 4),
                Arguments.of(11, Fase.PRE_LEITOR, 4),
                Arguments.of(12, Fase.LEITOR_INICIANTE, null),
                Arguments.of(30, Fase.LEITOR_INICIANTE, null),
                Arguments.of(31, Fase.LEITOR_FLUENTE, null),
                Arguments.of(60, Fase.LEITOR_FLUENTE, null));
    }

    @ParameterizedTest(name = "serie 1, {0} acertos -> {1}/{2}")
    @MethodSource("ac1Serie1")
    void classificarSerie1RetornaFaseENivelExatosDoSpecAc1(int acertos, Fase faseEsperada, Integer nivelEsperado) {
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(1)).thenReturn(faixasSerie1());

        RegraClassificacaoService.ClassificacaoResultado resultado = service().classificar(1, acertos);

        assertEquals(faseEsperada, resultado.fase());
        assertEquals(nivelEsperado, resultado.nivel());
    }

    @ParameterizedTest(name = "serie 2, {0} acertos -> {1}/{2}")
    @MethodSource("ac2Serie2a5")
    void classificarSerie2a5RetornaFaseENivelExatosDoSpecAc2(int acertos, Fase faseEsperada, Integer nivelEsperado) {
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(2)).thenReturn(faixasSerie2a5(2));

        RegraClassificacaoService.ClassificacaoResultado resultado = service().classificar(2, acertos);

        assertEquals(faseEsperada, resultado.fase());
        assertEquals(nivelEsperado, resultado.nivel());
    }

    @Test
    void classificarUsaApenasFaixasAtivasIgnorandoFaixaInativaQueMudariaOResultado() {
        // O repositório só devolve faixas ativo=true (findBySerieAndAtivoTrue...); uma faixa inativa
        // que cobrisse 999 nunca chega ao service - simulado aqui não incluindo-a no retorno do mock.
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(1)).thenReturn(faixasSerie1());

        RegraClassificacaoService.ClassificacaoResultado resultado = service().classificar(1, 999);

        assertEquals(Fase.LEITOR_FLUENTE, resultado.fase());
    }

    @Test
    void classificarRetornaMesmoResultadoIndependenteDeTipoDeLeituraPorNaoReceberEsseParametro() {
        // REG-04: classificar(serie, acertos) não recebe nenhum parâmetro de tipo de leitura -
        // logo o mesmo par (serie, acertos) sempre produz o mesmo resultado, para os 3 tipos.
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(1)).thenReturn(faixasSerie1());
        RegraClassificacaoService service = service();

        RegraClassificacaoService.ClassificacaoResultado primeiraChamada = service.classificar(1, 7);
        RegraClassificacaoService.ClassificacaoResultado segundaChamada = service.classificar(1, 7);
        RegraClassificacaoService.ClassificacaoResultado terceiraChamada = service.classificar(1, 7);

        assertEquals(primeiraChamada, segundaChamada);
        assertEquals(segundaChamada, terceiraChamada);
        assertEquals(Fase.PRE_LEITOR, primeiraChamada.fase());
        assertEquals(4, primeiraChamada.nivel());
    }

    @Test
    void classificarSemFaixaCobrindoOsAcertosRetornaFaseENivelNulosEmVezDeLancarExcecao() {
        // REG-05: faixa única 0-5; 10 não é coberto por nenhuma faixa ativa.
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(5))
                .thenReturn(List.of(new RegraClassificacao(5, 0, 5, Fase.PRE_LEITOR, 1)));

        RegraClassificacaoService.ClassificacaoResultado resultado = service().classificar(5, 10);

        assertEquals(null, resultado.fase());
        assertEquals(null, resultado.nivel());
    }

    @Test
    void buscarAtivasDelegaAoRepositorioRetornandoFaixasAtivasOrdenadasPorMinimo() {
        // RegraClassificacao não sobrescreve equals/hashCode (é uma entidade JPA) - a mesma lista de
        // instâncias é usada no mock e na asserção para comparar por identidade de forma confiável.
        List<RegraClassificacao> faixas = faixasSerie1();
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(1)).thenReturn(faixas);

        List<RegraClassificacao> ativas = service().buscarAtivas(1);

        assertEquals(faixas, ativas);
    }

    @Test
    void buscarHistoricoAgrupaFaixasConsecutivasComMesmoAlteradoEmPreservandoAOrdemDoRepositorio() {
        // Repositório já ordena: grupo corrente (alteradoEm=null) primeiro, depois os demais do mais
        // recente para o mais antigo (RegraClassificacaoRepository.buscarHistoricoPorSerie, T6).
        Instant maisRecente = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant maisAntigo = Instant.now().minus(2, ChronoUnit.DAYS);

        RegraClassificacao corrente1 = new RegraClassificacao(2, 0, 4, Fase.PRE_LEITOR, 1);
        RegraClassificacao corrente2 = new RegraClassificacao(2, 5, null, Fase.LEITOR_FLUENTE, null);
        RegraClassificacao grupoRecente1 = new RegraClassificacao(2, 0, 5, Fase.PRE_LEITOR, 1);
        RegraClassificacao grupoRecente2 = new RegraClassificacao(2, 6, null, Fase.LEITOR_FLUENTE, null);
        grupoRecente1.inativar(10L, maisRecente);
        grupoRecente2.inativar(10L, maisRecente);
        RegraClassificacao grupoAntigo1 = new RegraClassificacao(2, 0, 6, Fase.PRE_LEITOR, 1);
        grupoAntigo1.inativar(10L, maisAntigo);

        when(repository.buscarHistoricoPorSerie(2))
                .thenReturn(List.of(corrente1, corrente2, grupoRecente1, grupoRecente2, grupoAntigo1));

        List<List<RegraClassificacao>> historico = service().buscarHistorico(2);

        assertEquals(3, historico.size());
        assertEquals(List.of(corrente1, corrente2), historico.get(0));
        assertEquals(List.of(grupoRecente1, grupoRecente2), historico.get(1));
        assertEquals(List.of(grupoAntigo1), historico.get(2));
    }

    // --- substituir (T11, REG-07..REG-13) ---

    private void mockSaveAllIgual() {
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FaixaRequest faixa(int minimo, Integer maximo, Fase fase, Integer nivel) {
        return new FaixaRequest(minimo, maximo, fase, nivel);
    }

    @Test
    void substituirComConjuntoValidoInativaAnterioresComAuditoriaEGravaNovasAtivas() {
        RegraClassificacao anterior1 = new RegraClassificacao(3, 0, 5, Fase.PRE_LEITOR, 1);
        RegraClassificacao anterior2 = new RegraClassificacao(3, 6, null, Fase.LEITOR_FLUENTE, null);
        List<RegraClassificacao> anteriores = new ArrayList<>(List.of(anterior1, anterior2));
        when(repository.buscarAtivasParaAtualizarComLock(3)).thenReturn(anteriores);
        when(contextoUsuarioPort.usuarioIdAtual()).thenReturn(42L);
        mockSaveAllIgual();

        List<FaixaRequest> novoConjunto = List.of(
                faixa(0, 4, Fase.PRE_LEITOR, 1),
                faixa(5, 7, Fase.PRE_LEITOR, 2),
                faixa(8, 9, Fase.PRE_LEITOR, 3),
                faixa(10, 11, Fase.PRE_LEITOR, 4),
                faixa(12, 30, Fase.LEITOR_INICIANTE, null),
                faixa(31, null, Fase.LEITOR_FLUENTE, null));

        List<RegraClassificacao> resultado = service().substituir(3, novoConjunto);

        // REG-07/REG-13: faixas anteriores inativadas com auditoria completa.
        assertFalse(anterior1.isAtivo());
        assertFalse(anterior2.isAtivo());
        assertEquals(42L, anterior1.getAlteradoPor());
        assertEquals(42L, anterior2.getAlteradoPor());
        assertNotNull(anterior1.getAlteradoEm());
        // Mesmo instante para as duas linhas inativadas na mesma substituição (agrupamento do histórico, T10).
        assertEquals(anterior1.getAlteradoEm(), anterior2.getAlteradoEm());

        // REG-07: novas faixas gravadas, ativas, com o conteúdo enviado.
        assertEquals(6, resultado.size());
        assertTrue(resultado.stream().allMatch(RegraClassificacao::isAtivo));
        assertEquals(0, resultado.get(0).getQuantidadeMinimaAcertos());
        assertEquals(Fase.LEITOR_FLUENTE, resultado.get(5).getFase());
    }

    @Test
    void substituirComListaVaziaLancaFaixaNaoIniciaEmZero() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, List.of()));

        assertEquals("FAIXA_NAO_INICIA_EM_ZERO", ex.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void substituirComPrimeiraFaixaNaoComecandoEmZeroLancaFaixaNaoIniciaEmZero() {
        List<FaixaRequest> faixas = List.of(faixa(1, null, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_NAO_INICIA_EM_ZERO", ex.getCode());
    }

    @Test
    void substituirComLacunaEntreFaixasLancaFaixaComLacunaComPrimeiroValorDescoberto() {
        List<FaixaRequest> faixas = List.of(
                faixa(0, 3, Fase.PRE_LEITOR, 1),
                faixa(5, 10, Fase.PRE_LEITOR, 2),
                faixa(11, null, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_COM_LACUNA", ex.getCode());
        assertEquals(4, ex.getDetails().get("valor"));
    }

    @Test
    void substituirComMinimosDuplicadosLancaFaixaSobrepostaComPrimeiroValorDuplicado() {
        List<FaixaRequest> faixas = List.of(
                faixa(0, 5, Fase.PRE_LEITOR, 1),
                faixa(5, 10, Fase.PRE_LEITOR, 2),
                faixa(11, null, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_SOBREPOSTA", ex.getCode());
        assertEquals(5, ex.getDetails().get("valor"));
    }

    @Test
    void substituirComFaixasComIntervalosSobrepostosLancaFaixaSobrepostaComPrimeiroValorDuplicado() {
        // Sobreposição por intervalo (não apenas mínimos iguais): a faixa seguinte começa em 8, dentro
        // do intervalo 0-10 da faixa anterior - o primeiro valor duplicado é 8.
        List<FaixaRequest> faixas = List.of(
                faixa(0, 10, Fase.PRE_LEITOR, 1),
                faixa(8, 20, Fase.PRE_LEITOR, 2),
                faixa(21, null, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_SOBREPOSTA", ex.getCode());
        assertEquals(8, ex.getDetails().get("valor"));
    }

    @Test
    void substituirComFaixaNaoUltimaSemMaximoLancaFaixaSobrepostaNaFaixaSeguinte() {
        List<FaixaRequest> faixas = List.of(
                faixa(0, null, Fase.LEITOR_INICIANTE, null), faixa(999, null, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_SOBREPOSTA", ex.getCode());
        assertEquals(999, ex.getDetails().get("valor"));
    }

    @Test
    void substituirComUltimaFaixaComMaximoLancaFaixaFinalLimitada() {
        List<FaixaRequest> faixas =
                List.of(faixa(0, 5, Fase.PRE_LEITOR, 1), faixa(6, 10, Fase.LEITOR_FLUENTE, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_FINAL_LIMITADA", ex.getCode());
    }

    @Test
    void substituirComPreLeitorSemNivelDe1A4LancaFaixaNivelIncoerente() {
        List<FaixaRequest> faixas = List.of(faixa(0, null, Fase.PRE_LEITOR, null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_NIVEL_INCOERENTE", ex.getCode());
    }

    @Test
    void substituirComFaseDiferenteDePreLeitorENivelPreenchidoLancaFaixaNivelIncoerente() {
        List<FaixaRequest> faixas = List.of(faixa(0, null, Fase.LEITOR_FLUENTE, 2));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("FAIXA_NIVEL_INCOERENTE", ex.getCode());
    }

    @Test
    void substituirComMinimoMaiorQueMaximoNaMesmaFaixaLancaValidacaoInvalida() {
        List<FaixaRequest> faixas =
                List.of(faixa(0, 5, Fase.PRE_LEITOR, 1), faixa(6, 4, Fase.PRE_LEITOR, 2));

        BusinessException ex = assertThrows(BusinessException.class, () -> service().substituir(1, faixas));

        assertEquals("VALIDACAO_INVALIDA", ex.getCode());
    }

    @Test
    void substituirComFalhaDeValidacaoNaoAlteraAsFaixasAnterioresENaoTocaORepositorio() {
        List<RegraClassificacao> faixasAnteriores =
                List.of(new RegraClassificacao(1, 0, 5, Fase.PRE_LEITOR, 1));
        when(repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(1)).thenReturn(faixasAnteriores);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service().substituir(1, List.of()));
        assertEquals("FAIXA_NAO_INICIA_EM_ZERO", ex.getCode());

        // REG-12: nada foi lido/gravado no repositório para persistir a substituição.
        verify(repository, never()).buscarAtivasParaAtualizarComLock(anyInt());
        verify(repository, never()).saveAll(any());

        // As faixas anteriores continuam exatamente as mesmas.
        List<RegraClassificacao> aindaAtivas = service().buscarAtivas(1);
        assertEquals(faixasAnteriores, aindaAtivas);
    }
}

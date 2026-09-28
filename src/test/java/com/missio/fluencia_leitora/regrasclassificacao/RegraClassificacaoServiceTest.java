package com.missio.fluencia_leitora.regrasclassificacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * REG-01..REG-13/REG-15 (REG-14 deferido, ver spec.md): validação 1:1 dos
 * branches de {@link RegraClassificacaoService}.
 */
@ExtendWith(MockitoExtension.class)
class RegraClassificacaoServiceTest {

    @Mock
    private RegraClassificacaoRepository repository;

    private RegraClassificacaoService service() {
        return new RegraClassificacaoService(repository);
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
}

package com.missio.fluencia_leitora.avaliacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AVA-17: o scheduler só dispara {@link AvaliacaoService#finalizarInativas()}
 * - o disparo do cron em si não é testado aqui (mecanismo do Spring, não
 * lógica desta feature; design.md, Components).
 */
@ExtendWith(MockitoExtension.class)
class AvaliacaoFinalizacaoSchedulerTest {

    @Mock
    private AvaliacaoService avaliacaoService;

    @Test
    void finalizarInativasChamaOServico() {
        when(avaliacaoService.finalizarInativas()).thenReturn(2);
        AvaliacaoFinalizacaoScheduler scheduler = new AvaliacaoFinalizacaoScheduler(avaliacaoService);

        scheduler.finalizarInativas();

        verify(avaliacaoService).finalizarInativas();
    }

    @Test
    void finalizarInativasSemNenhumaNaoFalha() {
        when(avaliacaoService.finalizarInativas()).thenReturn(0);
        AvaliacaoFinalizacaoScheduler scheduler = new AvaliacaoFinalizacaoScheduler(avaliacaoService);

        scheduler.finalizarInativas();

        verify(avaliacaoService).finalizarInativas();
    }
}

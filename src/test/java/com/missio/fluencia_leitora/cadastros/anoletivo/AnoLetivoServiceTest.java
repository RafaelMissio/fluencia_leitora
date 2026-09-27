package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-01/CAD-02/CAD-04: AnoLetivoService.criar seeds the 5 series
 * configurations and rejects duplicate years; AnoLetivoService.ativar closes
 * any previous ATIVO year before activating the new one.
 */
@ExtendWith(MockitoExtension.class)
class AnoLetivoServiceTest {

    @Mock
    private AnoLetivoRepository anoLetivoRepository;

    @Mock
    private ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    @Captor
    private ArgumentCaptor<ConfiguracaoAvaliacao> configuracaoCaptor;

    private AnoLetivoService service() {
        return new AnoLetivoService(anoLetivoRepository, configuracaoAvaliacaoRepository);
    }

    @Test
    void criarComSucessoGravaOAnoEAsCincoConfiguracoesDoSeed() {
        LocalDate dataInicio = LocalDate.of(2026, 2, 1);
        LocalDate dataFim = LocalDate.of(2026, 12, 15);
        when(anoLetivoRepository.existsByAno(2026)).thenReturn(false);
        when(anoLetivoRepository.save(any(AnoLetivo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnoLetivo criado = service().criar(2026, dataInicio, dataFim);

        assertEquals(2026, criado.getAno());
        assertEquals(dataInicio, criado.getDataInicio());
        assertEquals(dataFim, criado.getDataFim());

        verify(configuracaoAvaliacaoRepository, times(5)).save(configuracaoCaptor.capture());
        List<ConfiguracaoAvaliacao> configuracoes = configuracaoCaptor.getAllValues();
        assertEquals(5, configuracoes.size());

        ConfiguracaoAvaliacao serie1 = configuracoes.get(0);
        assertEquals(1, serie1.getSerie());
        assertEquals(15, serie1.getQuantidadeMinima());
        assertEquals(20, serie1.getQuantidadeMaxima());

        for (int i = 1; i < 5; i++) {
            ConfiguracaoAvaliacao serie = configuracoes.get(i);
            assertEquals(i + 1, serie.getSerie());
            assertEquals(20, serie.getQuantidadeMinima());
            assertEquals(60, serie.getQuantidadeMaxima());
        }
    }

    @Test
    void criarComAnoDuplicadoLanca409SemGravarConfiguracoes() {
        when(anoLetivoRepository.existsByAno(2026)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().criar(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("ANO_LETIVO_DUPLICADO", exception.getCode());
        verify(anoLetivoRepository, never()).save(any());
        verify(configuracaoAvaliacaoRepository, never()).save(any());
    }

    @Test
    void ativarSemAtivoAnteriorApenasAtivaONovo() {
        AnoLetivo novo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        when(anoLetivoRepository.findById(1L)).thenReturn(Optional.of(novo));
        when(anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO)).thenReturn(List.of());
        when(anoLetivoRepository.save(any(AnoLetivo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnoLetivo resultado = service().ativar(1L);

        assertEquals(SituacaoAnoLetivo.ATIVO, resultado.getSituacao());
        verify(anoLetivoRepository, times(1)).save(any(AnoLetivo.class));
    }

    @Test
    void ativarComAtivoAnteriorEncerraOAntigoEAtivaONovo() {
        AnoLetivo antigoAtivo = new AnoLetivo(2025, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 12, 15));
        antigoAtivo.setSituacao(SituacaoAnoLetivo.ATIVO);
        AnoLetivo novo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));

        when(anoLetivoRepository.findById(2L)).thenReturn(Optional.of(novo));
        when(anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO)).thenReturn(List.of(antigoAtivo));
        when(anoLetivoRepository.save(any(AnoLetivo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnoLetivo resultado = service().ativar(2L);

        assertEquals(SituacaoAnoLetivo.ENCERRADO, antigoAtivo.getSituacao());
        assertEquals(SituacaoAnoLetivo.ATIVO, resultado.getSituacao());
        verify(anoLetivoRepository, times(1)).save(eq(antigoAtivo));
        verify(anoLetivoRepository, times(1)).save(eq(novo));
    }
}

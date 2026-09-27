package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CAD-06: ConfiguracaoAvaliacaoService.atualizar grava novos limites válidos
 * e rejeita cada violação (intervalo invertido, mínimo/máximo fora da faixa,
 * série fora de 1-5) com 422, sem alterar o registro.
 */
@ExtendWith(MockitoExtension.class)
class ConfiguracaoAvaliacaoServiceTest {

    @Mock
    private ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    private ConfiguracaoAvaliacaoService service() {
        return new ConfiguracaoAvaliacaoService(configuracaoAvaliacaoRepository);
    }

    private ConfiguracaoAvaliacao configuracaoExistente() {
        AnoLetivo anoLetivo = new AnoLetivo(2026, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 15));
        return new ConfiguracaoAvaliacao(anoLetivo, 1, 15, 20);
    }

    @Test
    void atualizacaoValidaGravaOsNovosLimites() {
        ConfiguracaoAvaliacao existente = configuracaoExistente();
        when(configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(10L, 1)).thenReturn(Optional.of(existente));
        when(configuracaoAvaliacaoRepository.save(any(ConfiguracaoAvaliacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracaoAvaliacao atualizada = service().atualizar(10L, 1, 10, 25);

        assertEquals(10, atualizada.getQuantidadeMinima());
        assertEquals(25, atualizada.getQuantidadeMaxima());
    }

    @Test
    void quantidadeMinimaMaiorQueMaximaRetorna422SemAlterarRegistro() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().atualizar(10L, 1, 30, 20));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("INTERVALO_INVALIDO", exception.getCode());
        verify(configuracaoAvaliacaoRepository, never()).save(any());
    }

    @Test
    void quantidadeMinimaMenorQueUmRetorna422SemAlterarRegistro() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().atualizar(10L, 1, 0, 20));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("QUANTIDADE_MINIMA_INVALIDA", exception.getCode());
        verify(configuracaoAvaliacaoRepository, never()).save(any());
    }

    @Test
    void quantidadeMaximaMaiorQueDuzentosRetorna422SemAlterarRegistro() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().atualizar(10L, 1, 15, 201));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("QUANTIDADE_MAXIMA_INVALIDA", exception.getCode());
        verify(configuracaoAvaliacaoRepository, never()).save(any());
    }

    @Test
    void serieForaDeUmACincoRetorna422SemAlterarRegistro() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().atualizar(10L, 6, 15, 20));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("SERIE_INVALIDA", exception.getCode());
        verify(configuracaoAvaliacaoRepository, never()).save(any());
    }
}

package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAD-06: valida e atualiza os limites mínimo/máximo de palavras de uma
 * série, dentro de um ano letivo.
 *
 * <p>SPEC_DEVIATION: {@code atualizar} recebe parâmetros primitivos em vez de
 * um {@code AtualizarConfiguracaoRequest} (mencionado no texto da T10),
 * porque esse DTO só é criado na T11 (Where da T10 lista somente
 * {@code ConfiguracaoAvaliacaoService.java}). O comportamento exigido pela
 * T10 é o mesmo; a T11 mapeia seu request DTO para estes parâmetros.
 */
@Service
public class ConfiguracaoAvaliacaoService {

    private static final int SERIE_MINIMA = 1;
    private static final int SERIE_MAXIMA = 5;
    private static final int QUANTIDADE_MINIMA_PERMITIDA = 1;
    private static final int QUANTIDADE_MAXIMA_PERMITIDA = 200;

    private final ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    public ConfiguracaoAvaliacaoService(ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository) {
        this.configuracaoAvaliacaoRepository = configuracaoAvaliacaoRepository;
    }

    @Transactional
    public ConfiguracaoAvaliacao atualizar(
            Long anoLetivoId, int serie, int quantidadeMinima, int quantidadeMaxima) {
        if (serie < SERIE_MINIMA || serie > SERIE_MAXIMA) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "SERIE_INVALIDA", "Série deve estar entre 1 e 5");
        }
        if (quantidadeMinima < QUANTIDADE_MINIMA_PERMITIDA) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "QUANTIDADE_MINIMA_INVALIDA",
                    "Quantidade mínima deve ser pelo menos 1");
        }
        if (quantidadeMaxima > QUANTIDADE_MAXIMA_PERMITIDA) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "QUANTIDADE_MAXIMA_INVALIDA",
                    "Quantidade máxima não pode passar de 200");
        }
        if (quantidadeMinima > quantidadeMaxima) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INTERVALO_INVALIDO",
                    "Quantidade mínima não pode ser maior que a máxima");
        }

        ConfiguracaoAvaliacao configuracao = configuracaoAvaliacaoRepository
                .findByAnoLetivoIdAndSerie(anoLetivoId, serie)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "CONFIGURACAO_NAO_ENCONTRADA", "Configuração não encontrada"));

        configuracao.setQuantidadeMinima(quantidadeMinima);
        configuracao.setQuantidadeMaxima(quantidadeMaxima);
        return configuracaoAvaliacaoRepository.save(configuracao);
    }
}

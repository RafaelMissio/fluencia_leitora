package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CAD-06: valida e atualiza os limites mínimo/máximo de palavras de uma
 * série, dentro de um ano letivo.
 *
 * <p>SPEC_DEVIATION: {@code atualizar} recebe parâmetros primitivos em vez de
 * um {@code AtualizarConfiguracaoRequest} (mencionado no texto da T10),
 * porque esse DTO só é criado na T11 (Where da T10 lista somente
 * {@code ConfiguracaoAvaliacaoService.java}). O comportamento exigido pela
 * T10 é o mesmo; a T11 mapeia seu request DTO para estes parâmetros.
 *
 * <p>SPEC_DEVIATION: {@code buscarAtivaPorSerie} foi adicionada depois de
 * {@code cadastros-base} fechar com Verifier PASS - a feature `frontend-web`
 * (T14, spec.md AC3 "contador N/mín/máx") descobriu que não existia nenhum
 * `GET` para o PROFESSOR ler os limites de uma série antes de enviar a
 * avaliação (só havia o `PUT` COORDENADOR-only). Decisão do usuário: adição
 * aditiva, sem alterar nenhum comportamento já testado.
 */
@Service
public class ConfiguracaoAvaliacaoService {

    private static final int SERIE_MINIMA = 1;
    private static final int SERIE_MAXIMA = 5;
    private static final int QUANTIDADE_MINIMA_PERMITIDA = 1;
    private static final int QUANTIDADE_MAXIMA_PERMITIDA = 200;

    private final ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;
    private final AnoLetivoRepository anoLetivoRepository;

    public ConfiguracaoAvaliacaoService(
            ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository, AnoLetivoRepository anoLetivoRepository) {
        this.configuracaoAvaliacaoRepository = configuracaoAvaliacaoRepository;
        this.anoLetivoRepository = anoLetivoRepository;
    }

    /**
     * Lê a configuração de {@code serie} no ano letivo ATIVO (CAD-04
     * garante no máximo um). 404 {@code ANO_LETIVO_ATIVO_NAO_ENCONTRADO}
     * quando não há ano ATIVO; 404 {@code CONFIGURACAO_NAO_ENCONTRADA}
     * quando a série não tem configuração nesse ano (ex. fora de 1-5).
     */
    public ConfiguracaoAvaliacao buscarAtivaPorSerie(int serie) {
        AnoLetivo anoAtivo = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "ANO_LETIVO_ATIVO_NAO_ENCONTRADO", "Nenhum ano letivo ativo"));
        return configuracaoAvaliacaoRepository
                .findByAnoLetivoIdAndSerie(anoAtivo.getId(), serie)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "CONFIGURACAO_NAO_ENCONTRADA", "Configuração não encontrada"));
    }

    /** Configurações (séries 1-5) de um ano letivo, para a tela de cadastro (frontend-web T30). 404 {@code ANO_LETIVO_NAO_ENCONTRADO} se o ano não existe. */
    @Transactional(readOnly = true)
    public List<ConfiguracaoAvaliacao> listarPorAnoLetivo(Long anoLetivoId) {
        if (!anoLetivoRepository.existsById(anoLetivoId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND, "ANO_LETIVO_NAO_ENCONTRADO", "Ano letivo não encontrado");
        }
        return configuracaoAvaliacaoRepository.findByAnoLetivoIdOrderBySerieAsc(anoLetivoId);
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

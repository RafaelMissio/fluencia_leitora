package com.missio.fluencia_leitora.cadastros.anoletivo;

import com.missio.fluencia_leitora.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * CAD-01/CAD-02/CAD-04: criação do ano letivo com o seed de configuração de
 * palavras por série, e ativação (com no máximo um ATIVO por vez).
 *
 * <p>SPEC_DEVIATION: {@code criar}/{@code ativar} recebem parâmetros
 * primitivos em vez de um {@code CriarAnoLetivoRequest} (mencionado no texto
 * da T9), porque esse DTO só é criado na T11 (Where da T9 lista somente
 * {@code AnoLetivoService.java}). O comportamento exigido pela T9 é o mesmo;
 * a T11 mapeia seu request DTO para estes parâmetros ao chamar o service.
 */
@Service
public class AnoLetivoService {

    private static final int SERIE_MINIMA = 1;
    private static final int SERIE_MAXIMA = 5;

    private final AnoLetivoRepository anoLetivoRepository;
    private final ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;

    public AnoLetivoService(
            AnoLetivoRepository anoLetivoRepository,
            ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository) {
        this.anoLetivoRepository = anoLetivoRepository;
        this.configuracaoAvaliacaoRepository = configuracaoAvaliacaoRepository;
    }

    @Transactional
    public AnoLetivo criar(int ano, LocalDate dataInicio, LocalDate dataFim) {
        if (anoLetivoRepository.existsByAno(ano)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ANO_LETIVO_DUPLICADO", "Ano letivo " + ano + " já cadastrado");
        }

        AnoLetivo anoLetivo = anoLetivoRepository.save(new AnoLetivo(ano, dataInicio, dataFim));
        criarConfiguracoesSeed(anoLetivo);
        return anoLetivo;
    }

    private void criarConfiguracoesSeed(AnoLetivo anoLetivo) {
        configuracaoAvaliacaoRepository.save(new ConfiguracaoAvaliacao(anoLetivo, SERIE_MINIMA, 15, 20));
        for (int serie = SERIE_MINIMA + 1; serie <= SERIE_MAXIMA; serie++) {
            configuracaoAvaliacaoRepository.save(new ConfiguracaoAvaliacao(anoLetivo, serie, 20, 60));
        }
    }

    @Transactional
    public AnoLetivo ativar(Long id) {
        AnoLetivo novoAtivo = anoLetivoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "ANO_LETIVO_NAO_ENCONTRADO", "Ano letivo não encontrado"));

        List<AnoLetivo> ativosAtuais = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO);
        for (AnoLetivo ativoAtual : ativosAtuais) {
            ativoAtual.setSituacao(SituacaoAnoLetivo.ENCERRADO);
            anoLetivoRepository.save(ativoAtual);
        }

        novoAtivo.setSituacao(SituacaoAnoLetivo.ATIVO);
        return anoLetivoRepository.save(novoAtivo);
    }
}

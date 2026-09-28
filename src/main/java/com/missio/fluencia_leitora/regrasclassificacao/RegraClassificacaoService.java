package com.missio.fluencia_leitora.regrasclassificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Classificação (REG-03..REG-05) e leitura (REG-06, REG-15) das faixas de
 * uma série (design.md, Components).
 */
@Service
public class RegraClassificacaoService {

    private final RegraClassificacaoRepository repository;

    public RegraClassificacaoService(RegraClassificacaoRepository repository) {
        this.repository = repository;
    }

    /**
     * REG-03/REG-04/REG-05: busca as faixas ativas da série e retorna a
     * fase/nível da faixa cujo intervalo cobre {@code acertos}, ou
     * {@code (null, null)} quando nenhuma cobre. Chamada Java direta, sem
     * HTTP - assinatura estável para {@code avaliacao} chamar depois.
     */
    @Transactional(readOnly = true)
    public ClassificacaoResultado classificar(int serie, int acertos) {
        List<RegraClassificacao> faixasAtivas =
                repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(serie);

        for (RegraClassificacao faixa : faixasAtivas) {
            boolean acimaDoMinimo = acertos >= faixa.getQuantidadeMinimaAcertos();
            boolean dentroDoMaximo =
                    faixa.getQuantidadeMaximaAcertos() == null || acertos <= faixa.getQuantidadeMaximaAcertos();
            if (acimaDoMinimo && dentroDoMaximo) {
                return new ClassificacaoResultado(faixa.getFase(), faixa.getNivel());
            }
        }

        return new ClassificacaoResultado(null, null);
    }

    /** REG-05: fase/nível resultantes de {@link #classificar}; ambos {@code null} quando nenhuma faixa cobre. */
    public record ClassificacaoResultado(Fase fase, Integer nivel) {
    }
}

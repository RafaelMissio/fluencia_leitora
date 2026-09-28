package com.missio.fluencia_leitora.regrasclassificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    /** REG-06: faixas ativas da série, ordenadas por {@code quantidadeMinimaAcertos}. */
    @Transactional(readOnly = true)
    public List<RegraClassificacao> buscarAtivas(int serie) {
        return repository.findBySerieAndAtivoTrueOrderByQuantidadeMinimaAcertosAsc(serie);
    }

    /**
     * REG-15: todas as faixas (ativas e inativas) da série, agrupadas por
     * {@code alteradoEm} na ordem já dada pelo repositório (grupo corrente
     * primeiro, depois os demais do mais recente para o mais antigo).
     *
     * <p>SPEC_DEVIATION: design.md descreve a assinatura como
     * {@code List<RegraClassificacao> buscarHistorico(int serie)} (lista
     * plana), mas tasks.md (T10, Done when) exige que o agrupamento por
     * {@code alteradoEm} aconteça aqui, não no controller. Reason: retornar
     * {@code List<List<RegraClassificacao>>} resolve a divergência sem
     * inventar um DTO dentro do service - a conversão para
     * {@code HistoricoVersaoResponse} continua no controller (T14), igual
     * ao padrão já usado por {@code RegraClassificacaoResponse.from(...)}.
     */
    @Transactional(readOnly = true)
    public List<List<RegraClassificacao>> buscarHistorico(int serie) {
        List<RegraClassificacao> todas = repository.buscarHistoricoPorSerie(serie);

        List<List<RegraClassificacao>> grupos = new ArrayList<>();
        List<RegraClassificacao> grupoAtual = null;
        Instant chaveGrupoAtual = null;

        for (RegraClassificacao faixa : todas) {
            if (grupoAtual == null || !Objects.equals(chaveGrupoAtual, faixa.getAlteradoEm())) {
                grupoAtual = new ArrayList<>();
                grupos.add(grupoAtual);
                chaveGrupoAtual = faixa.getAlteradoEm();
            }
            grupoAtual.add(faixa);
        }

        return grupos;
    }

    /** REG-05: fase/nível resultantes de {@link #classificar}; ambos {@code null} quando nenhuma faixa cobre. */
    public record ClassificacaoResultado(Fase fase, Integer nivel) {
    }
}

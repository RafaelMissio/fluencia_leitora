package com.missio.fluencia_leitora.regrasclassificacao;

import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.ContextoUsuarioPort;
import com.missio.fluencia_leitora.regrasclassificacao.dto.FaixaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Classificação (REG-03..REG-05), leitura (REG-06, REG-15) e substituição
 * atômica (REG-07..REG-13) das faixas de uma série (design.md, Components).
 */
@Service
public class RegraClassificacaoService {

    private static final Logger log = LoggerFactory.getLogger(RegraClassificacaoService.class);

    private static final int NIVEL_MINIMO_PRE_LEITOR = 1;
    private static final int NIVEL_MAXIMO_PRE_LEITOR = 4;

    private final RegraClassificacaoRepository repository;
    private final ContextoUsuarioPort contextoUsuarioPort;

    public RegraClassificacaoService(RegraClassificacaoRepository repository, ContextoUsuarioPort contextoUsuarioPort) {
        this.repository = repository;
        this.contextoUsuarioPort = contextoUsuarioPort;
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

    /**
     * REG-07..REG-13: valida o conjunto inteiro de faixas (a lista inteira
     * substitui a anterior - design.md, Tech Decisions) e, se válido,
     * inativa as faixas ativas da série (lock pessimista, serializa dois
     * {@code PUT} concorrentes) e grava as novas, tudo na mesma transação.
     * Se qualquer validação falhar, a exceção é lançada antes de qualquer
     * leitura/escrita no repositório - as faixas anteriores continuam
     * intocadas (REG-12).
     */
    @Transactional
    public List<RegraClassificacao> substituir(int serie, List<FaixaRequest> faixas) {
        validarSubstituicao(faixas);

        List<RegraClassificacao> faixasAnteriores = repository.buscarAtivasParaAtualizarComLock(serie);
        Long usuarioId = contextoUsuarioPort.usuarioIdAtual();
        Instant agora = Instant.now();
        faixasAnteriores.forEach(faixa -> faixa.inativar(usuarioId, agora));
        repository.saveAll(faixasAnteriores);

        List<RegraClassificacao> novasFaixas = faixas.stream()
                .map(f -> new RegraClassificacao(
                        serie, f.quantidadeMinimaAcertos(), f.quantidadeMaximaAcertos(), f.fase(), f.nivel()))
                .toList();
        List<RegraClassificacao> salvas = repository.saveAll(novasFaixas);

        log.info("Faixas da série {} substituídas por usuário {}: {} nova(s) faixa(s)", serie, usuarioId, salvas.size());

        return salvas;
    }

    /** Orquestra os validadores cruzados de {@link #substituir}, na ordem do design.md, Error Handling Strategy. */
    private void validarSubstituicao(List<FaixaRequest> faixas) {
        validarPrimeiraFaixaComecaEmZero(faixas);
        for (FaixaRequest faixa : faixas) {
            validarIntervaloDaFaixa(faixa);
            validarNivelCoerenteComAFase(faixa);
        }
        validarContiguidade(faixas);
        validarUltimaFaixaSemLimiteSuperior(faixas);
    }

    /** REG-08 + Edge Cases (lista vazia): a primeira faixa precisa começar em 0. */
    private void validarPrimeiraFaixaComecaEmZero(List<FaixaRequest> faixas) {
        if (faixas.isEmpty() || faixas.get(0).quantidadeMinimaAcertos() != 0) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "FAIXA_NAO_INICIA_EM_ZERO",
                    "A primeira faixa precisa começar em 0 acertos");
        }
    }

    /** Edge Cases: {@code quantidadeMinimaAcertos} não pode ser maior que {@code quantidadeMaximaAcertos}. */
    private void validarIntervaloDaFaixa(FaixaRequest faixa) {
        if (faixa.quantidadeMaximaAcertos() != null
                && faixa.quantidadeMinimaAcertos() > faixa.quantidadeMaximaAcertos()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDACAO_INVALIDA",
                    "quantidadeMinimaAcertos não pode ser maior que quantidadeMaximaAcertos");
        }
    }

    /** REG-11: {@code nivel} só é válido (1-4) quando {@code fase=PRE_LEITOR}; nas demais fases precisa ser nulo. */
    private void validarNivelCoerenteComAFase(FaixaRequest faixa) {
        boolean nivelValidoParaPreLeitor = faixa.nivel() != null
                && faixa.nivel() >= NIVEL_MINIMO_PRE_LEITOR
                && faixa.nivel() <= NIVEL_MAXIMO_PRE_LEITOR;
        boolean incoerente = faixa.fase() == Fase.PRE_LEITOR ? !nivelValidoParaPreLeitor : faixa.nivel() != null;

        if (incoerente) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "FAIXA_NIVEL_INCOERENTE",
                    "O nível informado não é coerente com a fase da faixa");
        }
    }

    /**
     * REG-09/REG-10: percorre as faixas em pares consecutivos (a ordem enviada define a ordem das
     * faixas - design.md, Tech Decisions). Uma faixa não-última com {@code quantidadeMaximaAcertos=null}
     * "engole" a faixa seguinte - vira {@code FAIXA_SOBREPOSTA}, não um código dedicado.
     */
    private void validarContiguidade(List<FaixaRequest> faixas) {
        for (int i = 0; i < faixas.size() - 1; i++) {
            FaixaRequest atual = faixas.get(i);
            FaixaRequest proxima = faixas.get(i + 1);

            if (atual.quantidadeMaximaAcertos() == null
                    || proxima.quantidadeMinimaAcertos() <= atual.quantidadeMaximaAcertos()) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "FAIXA_SOBREPOSTA",
                        "As faixas se sobrepõem",
                        Map.of("valor", proxima.quantidadeMinimaAcertos()));
            }

            if (proxima.quantidadeMinimaAcertos() > atual.quantidadeMaximaAcertos() + 1) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "FAIXA_COM_LACUNA",
                        "Há uma lacuna entre as faixas",
                        Map.of("valor", atual.quantidadeMaximaAcertos() + 1));
            }
        }
    }

    /** REG-11: a última faixa (maior mínimo) precisa ficar sem limite superior. */
    private void validarUltimaFaixaSemLimiteSuperior(List<FaixaRequest> faixas) {
        FaixaRequest ultima = faixas.get(faixas.size() - 1);
        if (ultima.quantidadeMaximaAcertos() != null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "FAIXA_FINAL_LIMITADA",
                    "A última faixa não pode ter um limite máximo de acertos");
        }
    }

    /** REG-05: fase/nível resultantes de {@link #classificar}; ambos {@code null} quando nenhuma faixa cobre. */
    public record ClassificacaoResultado(Fase fase, Integer nivel) {
    }
}

package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudioRepository;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoRepository;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoService;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * HIST-01..11, HIST-20..21: histórico e evolução por ciclo de avaliações
 * finalizadas de um aluno (design.md, Components). {@code historico}/{@code
 * comAudio} reaproveitam a mesma checagem de ownership de {@code
 * AlunoController.buscarPorId} (AUTH-09), mas aqui dentro do service em vez
 * do controller, porque a feature expõe 3 endpoints que compartilham a
 * mesma resolução de aluno. {@code evolucaoPorCiclo} não faz essa checagem
 * (design.md, Tech Decisions - RF013 é restrito só por papel).
 */
@Service
public class HistoricoEvolucaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AvaliacaoAudioRepository avaliacaoAudioRepository;
    private final AlunoService alunoService;
    private final AnoLetivoRepository anoLetivoRepository;
    private final CicloRepository cicloRepository;
    private final PertencimentoProfessorGuard pertencimentoProfessorGuard;
    private final MatriculaRepository matriculaRepository;

    public HistoricoEvolucaoService(
            AvaliacaoRepository avaliacaoRepository,
            AvaliacaoAudioRepository avaliacaoAudioRepository,
            AlunoService alunoService,
            AnoLetivoRepository anoLetivoRepository,
            CicloRepository cicloRepository,
            PertencimentoProfessorGuard pertencimentoProfessorGuard,
            MatriculaRepository matriculaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.avaliacaoAudioRepository = avaliacaoAudioRepository;
        this.alunoService = alunoService;
        this.anoLetivoRepository = anoLetivoRepository;
        this.cicloRepository = cicloRepository;
        this.pertencimentoProfessorGuard = pertencimentoProfessorGuard;
        this.matriculaRepository = matriculaRepository;
    }

    /** HIST-22 (Verifier PASS 1, gap E4): `cicloId` numérico mas fora do domínio fixo de `ciclo` também é 400. */
    @Transactional(readOnly = true)
    public Page<Avaliacao> historico(
            Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura, Long cicloId, Pageable pageable) {
        AlunoService.AlunoBusca alunoBusca = alunoService.buscarPorId(alunoId);
        verificarPertencimento(alunoBusca.matriculaAtiva());
        if (cicloId != null && !cicloRepository.existsById(cicloId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CICLO_INVALIDO", "cicloId inválido");
        }

        return avaliacaoRepository.buscarHistorico(
                alunoId, StatusAvaliacao.FINALIZADA, anoLetivoId, tipoLeitura, cicloId, pageable);
    }

    @Transactional(readOnly = true)
    public Set<Long> comAudio(List<Long> avaliacaoIds) {
        return new HashSet<>(avaliacaoAudioRepository.findAvaliacaoIdByAvaliacaoIdIn(avaliacaoIds));
    }

    /**
     * HIST-07..11: os três ciclos (ENTRADA/ACOMPANHAMENTO/SAIDA) do ano
     * informado (ou o ano ATIVO), cada um com a tentativa FINALIZADA de maior
     * número de corretas ou {@code null} quando o ciclo não tem avaliação.
     *
     * <p>Sem {@link PertencimentoProfessorGuard} de propósito: design.md,
     * Tech Decisions - RF013 é tratado como relatório gerencial, restrito só
     * por papel (`hasRole('COORDENADOR')` no controller), sem checar aluno
     * específico.
     */
    @Transactional(readOnly = true)
    public EvolucaoCiclos evolucaoPorCiclo(Long alunoId, Long anoLetivoId, TipoLeituraCodigo tipoLeitura) {
        alunoService.buscarPorId(alunoId);
        AnoLetivo anoLetivo = resolverAnoLetivo(anoLetivoId);

        List<Avaliacao> finalizadas = avaliacaoRepository.buscarFinalizadasPorAnoETipo(
                alunoId, StatusAvaliacao.FINALIZADA, anoLetivo.getId(), tipoLeitura);
        Collection<Avaliacao> maisRecentePorCiclo =
                melhorPorGrupo(finalizadas, avaliacao -> avaliacao.getCiclo().getId()).values();

        return new EvolucaoCiclos(
                alunoId,
                anoLetivo.getAno(),
                tipoLeitura,
                porCicloCodigo(maisRecentePorCiclo, "ENTRADA"),
                porCicloCodigo(maisRecentePorCiclo, "ACOMPANHAMENTO"),
                porCicloCodigo(maisRecentePorCiclo, "SAIDA"));
    }

    /**
     * HIST-12..19: uma linha por ano letivo com ao menos 1 `FINALIZADA` do
     * tipo pedido, ordenadas por ano crescente (a query já ordena por {@code
     * anoLetivo.ano} - HIST-13), com a evolução absoluta/percentual de cada
     * ciclo contra a mesma linha (ano) imediatamente anterior na lista
     * (design.md, Assumptions - "mesmo ciclo, ano a ano"). Cada linha traz a
     * turma em que o aluno estava matriculado naquele ano (ex.: 1º A em 2026,
     * 2º B em 2027), para comparar a evolução entre turmas.
     *
     * <p>Sem {@link PertencimentoProfessorGuard}, mesmo motivo de {@link
     * #evolucaoPorCiclo}.
     */
    @Transactional(readOnly = true)
    public List<EvolucaoAnualLinha> evolucaoAnual(Long alunoId, TipoLeituraCodigo tipoLeitura) {
        alunoService.buscarPorId(alunoId);

        List<Avaliacao> finalizadas =
                avaliacaoRepository.buscarFinalizadasPorTipo(alunoId, StatusAvaliacao.FINALIZADA, tipoLeitura);
        Collection<Avaliacao> maisRecentePorAnoECiclo = melhorPorGrupo(
                        finalizadas,
                        avaliacao -> new GrupoAnoCiclo(avaliacao.getAnoLetivo().getId(), avaliacao.getCiclo().getId()))
                .values();

        Map<Long, List<Avaliacao>> avaliacoesPorAno = new LinkedHashMap<>();
        for (Avaliacao avaliacao : maisRecentePorAnoECiclo) {
            avaliacoesPorAno
                    .computeIfAbsent(avaliacao.getAnoLetivo().getId(), id -> new ArrayList<>())
                    .add(avaliacao);
        }

        Map<Long, String> turmaPorAno = new LinkedHashMap<>();
        for (Matricula matricula : matriculaRepository.findByAlunoId(alunoId)) {
            turmaPorAno.put(matricula.getAnoLetivo().getId(), matricula.getTurma().getNome());
        }

        List<EvolucaoAnualLinha> linhas = new ArrayList<>();
        List<Avaliacao> anoAnterior = null;
        for (List<Avaliacao> anoAtual : avaliacoesPorAno.values()) {
            Avaliacao referencia = anoAtual.get(0);
            Avaliacao entrada = porCicloCodigo(anoAtual, "ENTRADA");
            Avaliacao acompanhamento = porCicloCodigo(anoAtual, "ACOMPANHAMENTO");
            Avaliacao saida = porCicloCodigo(anoAtual, "SAIDA");

            linhas.add(new EvolucaoAnualLinha(
                    referencia.getAnoLetivo().getAno(),
                    referencia.getSerie(),
                    turmaPorAno.get(referencia.getAnoLetivo().getId()),
                    entrada,
                    evolucao(entrada, porCicloCodigo(anoAnterior, "ENTRADA")),
                    acompanhamento,
                    evolucao(acompanhamento, porCicloCodigo(anoAnterior, "ACOMPANHAMENTO")),
                    saida,
                    evolucao(saida, porCicloCodigo(anoAnterior, "SAIDA"))));
            anoAnterior = anoAtual;
        }
        return linhas;
    }

    /**
     * HIST-14, HIST-17..19: evolução absoluta/percentual sobre {@code
     * quantidadeCorretas}. {@code null}/{@code null} quando falta o atual ou
     * o anterior (sem ciclo no ano/ano-anterior); {@code 0} quando os dois
     * lados são zero (HIST-18); {@code null} só no percentual quando o
     * anterior é zero e o atual é maior que zero (HIST-19, evita divisão
     * por zero).
     */
    private static EvolucaoValor evolucao(Avaliacao atual, Avaliacao anterior) {
        if (atual == null || anterior == null) {
            return new EvolucaoValor(null, null);
        }
        int corretasAtual = atual.getQuantidadeCorretas();
        int corretasAnterior = anterior.getQuantidadeCorretas();
        int absoluta = corretasAtual - corretasAnterior;
        BigDecimal percentual;
        if (corretasAnterior == 0) {
            percentual = corretasAtual == 0 ? BigDecimal.ZERO : null;
        } else {
            percentual = BigDecimal.valueOf(absoluta * 100L)
                    .divide(BigDecimal.valueOf(corretasAnterior), 2, RoundingMode.HALF_UP);
        }
        return new EvolucaoValor(absoluta, percentual);
    }

    private AnoLetivo resolverAnoLetivo(Long anoLetivoId) {
        if (anoLetivoId != null) {
            return anoLetivoRepository.findById(anoLetivoId).orElseThrow(this::anoLetivoNaoEncontrado);
        }
        return anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO).stream()
                .findFirst()
                .orElseThrow(this::anoLetivoNaoEncontrado);
    }

    private BusinessException anoLetivoNaoEncontrado() {
        return new BusinessException(HttpStatus.NOT_FOUND, "ANO_LETIVO_NAO_ENCONTRADO", "Ano letivo não encontrado");
    }

    /**
     * Por grupo (ciclo, ou ano+ciclo), a tentativa com mais palavras corretas -
     * inclusive as refeitas já inativadas; no empate vale a mais recente (a
     * lista chega ordenada da mais recente para a mais antiga).
     */
    private static <K> Map<K, Avaliacao> melhorPorGrupo(List<Avaliacao> ordenadosPorGrupo, Function<Avaliacao, K> chave) {
        Map<K, Avaliacao> melhorPorChave = new LinkedHashMap<>();
        for (Avaliacao item : ordenadosPorGrupo) {
            melhorPorChave.merge(
                    chave.apply(item),
                    item,
                    (atual, candidata) ->
                            candidata.getQuantidadeCorretas() > atual.getQuantidadeCorretas() ? candidata : atual);
        }
        return melhorPorChave;
    }

    private static Avaliacao porCicloCodigo(Collection<Avaliacao> avaliacoes, String codigoCiclo) {
        if (avaliacoes == null) {
            return null;
        }
        return avaliacoes.stream()
                .filter(avaliacao -> avaliacao.getCiclo().getCodigo().equals(codigoCiclo))
                .findFirst()
                .orElse(null);
    }

    private void verificarPertencimento(Matricula matriculaAtiva) {
        pertencimentoProfessorGuard.verificar(
                matriculaAtiva == null || matriculaAtiva.getProfessor() == null
                        ? null
                        : matriculaAtiva.getProfessor().getId());
    }

    /** HIST-07..11: tipo de retorno interno, mapeado a DTO no controller (T7). */
    public record EvolucaoCiclos(
            Long alunoId,
            int anoLetivo,
            TipoLeituraCodigo tipoLeitura,
            Avaliacao entrada,
            Avaliacao acompanhamento,
            Avaliacao saida) {
    }

    /** Chave de agrupamento "mais recente por (ano letivo, ciclo)" (HIST-20). */
    private record GrupoAnoCiclo(Long anoLetivoId, Long cicloId) {
    }

    /** HIST-14, HIST-17..19: evolução absoluta/percentual sobre `quantidadeCorretas`. */
    public record EvolucaoValor(Integer absoluta, BigDecimal percentual) {
    }

    /** HIST-12..19: uma linha da comparação anual, tipo interno mapeado a DTO no controller (T8). */
    public record EvolucaoAnualLinha(
            int anoLetivo,
            int serie,
            String turma,
            Avaliacao entrada,
            EvolucaoValor evolucaoEntrada,
            Avaliacao acompanhamento,
            EvolucaoValor evolucaoAcompanhamento,
            Avaliacao saida,
            EvolucaoValor evolucaoSaida) {
    }
}
